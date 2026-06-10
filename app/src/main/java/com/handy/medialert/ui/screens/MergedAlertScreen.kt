package com.handy.medialert.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.handy.medialert.R
import com.handy.medialert.data.entity.Medication
import com.handy.medialert.ui.components.EmptyState
import com.handy.medialert.viewmodel.MedicationViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MergedAlertScreen(
    onNavigateBack: () -> Unit,
    viewModel: MedicationViewModel = viewModel()
) {
    val medications by viewModel.activeMedications.collectAsStateWithLifecycle()
    val mergedAlerts = remember(medications) { calculateMergedAlerts(medications) }
    val dateFormatPattern = stringResource(R.string.date_format_month_day)
    val dateFormatter = remember(dateFormatPattern) { DateTimeFormatter.ofPattern(dateFormatPattern, Locale.getDefault()) }
    val alertList = remember(mergedAlerts) { mergedAlerts.toList() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.merged_alerts_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { paddingValues ->
        if (mergedAlerts.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.no_alerts),
                subtitle = stringResource(R.string.no_alerts_subtitle),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(alertList, key = { it.first }) { (alertDate, meds) ->
                    val dayOfWeek = alertDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
                    val isWorkday = alertDate.dayOfWeek.value <= 5
                    val weekdayLabel = if (isWorkday) stringResource(R.string.workday_label) else stringResource(R.string.weekend_label)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.merged_alert_date_display, alertDate.format(dateFormatter), dayOfWeek, weekdayLabel),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            meds.forEach { med ->
                                val depletionDate = med.depletionDate()
                                val depletionDayOfWeek = depletionDate.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                                Text(
                                    text = stringResource(R.string.merged_alert_item, med.genericName, med.brandName ?: "", depletionDate.format(dateFormatter), depletionDayOfWeek),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun calculateMergedAlerts(medications: List<Medication>): Map<LocalDate, List<Medication>> {
    val activeMeds = medications.filter { it.isActive && it.daysUntilDepletion() > 0 }
    if (activeMeds.isEmpty()) return emptyMap()

    // 计算每个药品的提醒日期（提前4天）
    val alertDates = activeMeds.map { it to it.depletionDate().minusDays(4) }

    // 按提醒日期分组
    val groupedByAlertDate = alertDates.groupBy { it.second }
        .mapValues { it.value.map { pair -> pair.first } }
        .toSortedMap()

    // 合并3天内的提醒
    val merged = mutableMapOf<LocalDate, MutableList<Medication>>()
    var currentMergeDate: LocalDate? = null

    groupedByAlertDate.forEach { (alertDate, meds) ->
        if (currentMergeDate == null || alertDate.isAfter(currentMergeDate!!.plusDays(3))) {
            // 开始新的合并组
            currentMergeDate = alertDate
            merged[alertDate] = meds.toMutableList()
        } else {
            // 合并到当前组
            merged[currentMergeDate!!]?.addAll(meds)
        }
    }

    return merged
}
