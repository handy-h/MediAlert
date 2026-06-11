package com.handy.medialert.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.handy.medialert.R
import com.handy.medialert.data.entity.Medication
import com.handy.medialert.ui.theme.NormalGreen
import com.handy.medialert.ui.theme.UrgentRed
import com.handy.medialert.ui.theme.WarningYellow
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun MedicationCard(
    medication: Medication,
    onAddStock: () -> Unit,
    onReduceStock: () -> Unit,
    onDeactivate: () -> Unit,
    onEdit: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val daysLeft = medication.daysUntilDepletion()
    val depletionDate = medication.depletionDate()
    val dayOfWeek = depletionDate.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
    val dateFormatPattern = stringResource(R.string.date_format_month_day)
    val dateFormatter = remember(dateFormatPattern) { DateTimeFormatter.ofPattern(dateFormatPattern, Locale.getDefault()) }

    val (statusColor, statusText) = when {
        daysLeft <= 1 -> UrgentRed to stringResource(R.string.days_until_depleted, daysLeft)
        daysLeft <= 4 -> WarningYellow to stringResource(R.string.days_until_depleted, daysLeft)
        else -> NormalGreen to stringResource(R.string.days_until_depleted, daysLeft)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 标题行：药名 + 编辑 + 状态标签（自适应换行）
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp)) {
                    Text(
                        text = medication.genericName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    medication.brandName?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = stringResource(R.string.edit_medication),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    StatusBadge(color = statusColor, text = statusText)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 规格信息
            Text(
                text = stringResource(R.string.spec_display, medication.specification ?: stringResource(R.string.not_filled), medication.packageSize, medication.dosageForm, medication.packageUnit),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 库存和耗尽日期（允许换行适配大字体）
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.stock_display_label, medication.getStockDisplay()),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = stringResource(R.string.depletion_display, depletionDate.format(dateFormatter), dayOfWeek),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 用药频率
            val freqText = when (medication.frequencyType) {
                com.handy.medialert.data.entity.FrequencyType.EVERY_X_DAYS ->
                    stringResource(R.string.freq_every_x_days_format, medication.frequencyValue, medication.dailyDosage, medication.dosageForm)
                com.handy.medialert.data.entity.FrequencyType.EVERY_XTH_DAY ->
                    stringResource(R.string.freq_every_xth_day_format, medication.frequencyValue, medication.dailyDosage, medication.dosageForm)
            }
            Text(
                text = stringResource(R.string.dosage_display, freqText),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 操作按钮（纯图标模式，避免大字体截断）
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
            ) {
                OutlinedIconButton(onClick = onAddStock) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.restock), modifier = Modifier.size(22.dp))
                }
                OutlinedIconButton(onClick = onReduceStock) {
                    Icon(Icons.Default.Remove, contentDescription = stringResource(R.string.consume), modifier = Modifier.size(22.dp))
                }
                OutlinedIconButton(onClick = onDeactivate) {
                    Icon(Icons.Default.Pause, contentDescription = stringResource(R.string.deactivate), modifier = Modifier.size(22.dp))
                }
            }
        }
    }
}
