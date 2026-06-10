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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(stringResource(R.string.current_stock_display, medication.genericName, medication.getStockDisplay()))
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = packageInput,
                    onValueChange = { packageInput = it.filter { c -> c.isDigit() } },
                    label = { Text("${medication.packageUnit}数") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = unitInput,
                    onValueChange = { unitInput = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("零散${medication.dosageForm}数") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text(stringResource(R.string.reason_optional)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val packages = packageInput.toIntOrNull() ?: 0
                    val units = unitInput.toDoubleOrNull() ?: 0.0
                    val total = packages * medication.packageSize + units
                    if (total > 0) {
                        onConfirm(total, reason.takeIf { it.isNotBlank() })
                    }
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
