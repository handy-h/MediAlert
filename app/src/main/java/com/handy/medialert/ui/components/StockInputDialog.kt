package com.handy.medialert.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.handy.medialert.R
import com.handy.medialert.data.entity.Medication

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockInputDialog(
    title: String,
    medication: Medication,
    isAddition: Boolean,
    onConfirm: (Double, String?) -> Unit,
    onDismiss: () -> Unit
) {
    var packageInput by remember { mutableStateOf("") }
    var unitInput by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    // 减少库存时，若输入量超过当前库存则展示错误并阻止提交
    var showExceedError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(stringResource(R.string.current_stock_display, medication.genericName, medication.getStockDisplay()))
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = packageInput,
                    onValueChange = {
                        packageInput = it.filter { c -> c.isDigit() }
                        showExceedError = false
                    },
                    label = { Text(stringResource(R.string.package_count_label, medication.packageUnit)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    isError = showExceedError
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = unitInput,
                    onValueChange = {
                        unitInput = it.filter { c -> c.isDigit() || c == '.' }
                        showExceedError = false
                    },
                    label = { Text(stringResource(R.string.loose_units_label, medication.dosageForm)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    isError = showExceedError
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text(stringResource(R.string.reason_optional)) },
                    modifier = Modifier.fillMaxWidth()
                )
                if (showExceedError) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.error_reduce_exceeds_stock, medication.getStockDisplay()),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val packages = packageInput.toIntOrNull() ?: 0
                    val units = unitInput.toDoubleOrNull() ?: 0.0
                    val total = packages * medication.packageSize + units
                    if (total <= 0) return@TextButton
                    // 减少库存时校验不超量，避免用户误输入导致库存被意外清零
                    if (!isAddition && total > medication.currentStock) {
                        showExceedError = true
                        return@TextButton
                    }
                    onConfirm(total, reason.takeIf { it.isNotBlank() })
                }
            ) {
                Text(stringResource(R.string.confirm_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
