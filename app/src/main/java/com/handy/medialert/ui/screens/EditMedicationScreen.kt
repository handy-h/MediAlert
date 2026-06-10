package com.handy.medialert.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.handy.medialert.R
import com.handy.medialert.data.entity.FrequencyType
import com.handy.medialert.viewmodel.MedicationViewModel
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditMedicationScreen(
    medicationId: Long,
    onNavigateBack: () -> Unit,
    viewModel: MedicationViewModel = viewModel()
) {
    val medication by viewModel.getMedicationFlow(medicationId)
        .collectAsStateWithLifecycle(initialValue = null)
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    medication?.let { med ->
        // 基础信息
        var genericName by remember { mutableStateOf(med.genericName) }
        var brandName by remember { mutableStateOf(med.brandName ?: "") }
        var specification by remember { mutableStateOf(med.specification ?: "") }

        // 包装信息
        var packageUnit by remember { mutableStateOf(med.packageUnit) }
        var dosageForm by remember { mutableStateOf(med.dosageForm) }
        var packageSize by remember { mutableStateOf(med.packageSize.toString()) }

        // 库存信息
        var currentStock by remember { mutableStateOf(med.currentStock.toString()) }

        // 用药频率
        var frequencyType by remember { mutableStateOf(med.frequencyType) }
        var frequencyValue by remember { mutableStateOf(med.frequencyValue.toString()) }
        var dailyDosage by remember { mutableStateOf(med.dailyDosage.toString()) }

        // 开始日期
        var hasStartDate by remember { mutableStateOf(med.startDate != null) }
        var startDate by remember { mutableStateOf(med.startDate ?: LocalDate.now()) }

        // 验证状态
        var frequencyValueError by remember { mutableStateOf(false) }
        var dailyDosageError by remember { mutableStateOf(false) }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.edit_medication)) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    },
                    actions = {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete))
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // 基础信息
                OutlinedTextField(
                    value = genericName,
                    onValueChange = { genericName = it },
                    label = { Text(stringResource(R.string.generic_name_required)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = brandName,
                    onValueChange = { brandName = it },
                    label = { Text(stringResource(R.string.brand_name)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = specification,
                    onValueChange = { specification = it },
                    label = { Text(stringResource(R.string.specification_hint)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))

                // 包装信息
                Text(stringResource(R.string.package_info), style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = packageUnit,
                        onValueChange = { packageUnit = it },
                        label = { Text(stringResource(R.string.package_label)) },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = dosageForm,
                        onValueChange = { dosageForm = it },
                        label = { Text(stringResource(R.string.dosage_form_label)) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = packageSize,
                    onValueChange = { packageSize = it.filter { c -> c.isDigit() } },
                    label = { Text(stringResource(R.string.package_size_label, packageUnit)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))

                // 当前库存
                Text(stringResource(R.string.current_stock), style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = currentStock,
                    onValueChange = { currentStock = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text(stringResource(R.string.stock_with_form, dosageForm)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))

                // 用药频率
                Text(stringResource(R.string.medication_frequency), style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.weight(1f)) {
                        SegmentedButton(
                            selected = frequencyType == FrequencyType.EVERY_X_DAYS,
                            onClick = { frequencyType = FrequencyType.EVERY_X_DAYS },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                        ) {
                            Text(stringResource(R.string.every_x_days))
                        }
                        SegmentedButton(
                            selected = frequencyType == FrequencyType.EVERY_XTH_DAY,
                            onClick = { frequencyType = FrequencyType.EVERY_XTH_DAY },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                        ) {
                            Text(stringResource(R.string.every_xth_day))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = frequencyValue,
                        onValueChange = {
                            frequencyValue = it.filter { c -> c.isDigit() }
                            frequencyValueError = false
                        },
                        modifier = Modifier.width(80.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = frequencyValueError,
                        supportingText = if (frequencyValueError) {{ Text(stringResource(R.string.error_frequency_must_be_positive)) }} else null
                    )
                    Text(
                        when (frequencyType) {
                            FrequencyType.EVERY_X_DAYS -> "天"
                            FrequencyType.EVERY_XTH_DAY -> "天"
                        }
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    OutlinedTextField(
                        value = dailyDosage,
                        onValueChange = {
                            dailyDosage = it.filter { c -> c.isDigit() || c == '.' }
                            dailyDosageError = false
                        },
                        label = { Text(stringResource(R.string.dosage_label)) },
                        modifier = Modifier.width(100.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = dailyDosageError,
                        supportingText = if (dailyDosageError) {{ Text(stringResource(R.string.error_invalid_dosage)) }} else null
                    )
                    Text(dosageForm)
                }
                Spacer(modifier = Modifier.height(16.dp))

                // 开始日期
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = hasStartDate,
                        onCheckedChange = { hasStartDate = it }
                    )
                    Text(stringResource(R.string.set_start_date))
                }
                if (hasStartDate) {
                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.start_date_label, startDate.toString()))
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        val freq = frequencyValue.toIntOrNull()
                        val dosage = dailyDosage.toDoubleOrNull()
                        val stock = currentStock.toDoubleOrNull()
                        val pkgSize = packageSize.toIntOrNull()

                        frequencyValueError = freq == null || freq <= 0
                        dailyDosageError = dosage == null || dosage <= 0

                        if (freq == null || freq <= 0 || dosage == null || dosage <= 0 || stock == null || pkgSize == null) return@Button

                        viewModel.updateMedication(
                            med.copy(
                                genericName = genericName,
                                brandName = brandName.takeIf { it.isNotBlank() },
                                specification = specification.takeIf { it.isNotBlank() },
                                packageUnit = packageUnit,
                                dosageForm = dosageForm,
                                packageSize = pkgSize,
                                currentStock = stock,
                                frequencyType = frequencyType,
                                frequencyValue = freq,
                                dailyDosage = dosage,
                                startDate = if (hasStartDate) startDate else null
                            )
                        )
                        onNavigateBack()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = genericName.isNotBlank() && packageSize.isNotBlank()
                ) {
                    Text(stringResource(R.string.save_changes))
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        if (showDatePicker) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            )
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            startDate = java.time.Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                        }
                        showDatePicker = false
                    }) {
                        Text(stringResource(R.string.confirm))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.confirm_delete)) },
            text = { Text(stringResource(R.string.delete_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteMedication(medicationId)
                        onNavigateBack()
                    }
                ) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
