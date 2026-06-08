package com.handy.medialert.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
    modifier: Modifier = Modifier
) {
    val daysLeft = medication.daysUntilDepletion()
    val depletionDate = medication.depletionDate()
    val dayOfWeek = depletionDate.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.CHINESE)
    val dateFormatter = DateTimeFormatter.ofPattern("M月d日")

    val (statusColor, statusText) = when {
        daysLeft <= 1 -> UrgentRed to "${daysLeft}天后耗尽"
        daysLeft <= 4 -> WarningYellow to "${daysLeft}天后耗尽"
        else -> NormalGreen to "${daysLeft}天后耗尽"
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
            // 标题行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = medication.genericName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    medication.brandName?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                StatusBadge(color = statusColor, text = statusText)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 规格信息
            Text(
                text = "规格：${medication.specification ?: "未填写"} | ${medication.packageSize}${medication.dosageForm}/${medication.packageUnit}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 库存和耗尽日期
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "当前库存：${medication.getStockDisplay()}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "耗尽：${depletionDate.format(dateFormatter)} $dayOfWeek",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 用药频率
            val freqText = when (medication.frequencyType) {
                com.handy.medialert.data.entity.FrequencyType.EVERY_X_DAYS ->
                    "每${medication.frequencyValue}天${medication.dailyDosage}${medication.dosageForm}"
                com.handy.medialert.data.entity.FrequencyType.EVERY_XTH_DAY ->
                    "每隔${medication.frequencyValue}天${medication.dailyDosage}${medication.dosageForm}"
            }
            Text(
                text = "用药：$freqText",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 操作按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onAddStock,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("补货")
                }
                OutlinedButton(
                    onClick = onReduceStock,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("消耗")
                }
                OutlinedButton(
                    onClick = onDeactivate,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Pause, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("停用")
                }
            }
        }
    }
}
