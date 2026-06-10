package com.handy.medialert.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
    val context = LocalContext.current
    val medication by viewModel.getMedicationFlow(medicationId)
        .collectAsStateWithLifecycle(initialValue = null)
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val packageUnits = remember { context.resources.getStringArray(R.array.package_units).toList() }
    val dosageForms = remember { context.resources.getStringArray(R.array.dosage_forms).toList() }
    var packageUnitExpanded by rememberSaveable { mutableStateOf(false) }
    var dosageFormExpanded by rememberSaveable { mutableStateOf(false) }

    medication?.let { med ->
        // 基础信息
        var genericName by remember { mutableStateOf(med.genericName) }
        var brandName by remember { mutableStateOf(med.brandName ?: "") }
        var specification by remember { mutableStateOf(med.specification ?: "") }

        // 包装信息
        var packageUnit by remember { mutableStateOf(med.packageUnit) }
        var dosageForm by remember { mutableStateOf(med.dosageForm) }
        var packageSize by remember { mutableStateOf(med.packageSize.toString()) }

        // 库存信息（二元输入，与新增页一致）
        var stockPackages by remember {
            mutableStateOf((med.currentStock / med.packageSize).toInt().toString())
        }
        var stockUnits by remember {
            mutableStateOf(formatStockRemainder(med.currentStock % med.packageSize))
        }

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
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
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
                // ===== 基础信息 =====
                OutlinedTextField(
                    value = genericName,
                    onValueChange = { genericName = it },
                    label = { Text(stringResource(R.string.generic_name_required)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = brandName,
                    onValueChange = { brandName = it },
                    label = { Text(stringResource(R.string.brand_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = specification,
                    onValueChange = { specification = it },
                    label = { Text(stringResource(R.string.specification_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(20.dp))

                // ===== 包装信息 =====
                Text(stringResource(R.string.package_info), style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ExposedDropdownMenuBox(
                        expanded = packageUnitExpanded,
                        onExpandedChange = { packageUnitExpanded = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = packageUnit,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.package_label)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = packageUnitExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            singleLine = true
                        )
                        DropdownMenu(
                            expanded = packageUnitExpanded,
                            onDismissRequest = { packageUnitExpanded = false }
                        ) {
                            packageUnits.forEach { unit ->
                                DropdownMenuItem(
                                    text = { Text(unit) },
                                    onClick = {
                                        packageUnit = unit
                                        packageUnitExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    ExposedDropdownMenuBox(
                        expanded = dosageFormExpanded,
                        onExpandedChange = { dosageFormExpanded = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = dosageForm,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.dosage_form_label)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dosageFormExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            singleLine = true
                        )
                        DropdownMenu(
                            expanded = dosageFormExpanded,
                            onDismissRequest = { dosageFormExpanded = false }
                        ) {
                            dosageForms.forEach { form ->
                                DropdownMenuItem(
                                    text = { Text(form) },
                                    onClick = {
                                        dosageForm = form
                                        dosageFormExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = packageSize,
                    onValueChange = { packageSize = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text(stringResource(R.string.package_size_hint, packageUnit, dosageForm)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(20.dp))

                // ===== 当前库存 =====
                Text(stringResource(R.string.current_stock), style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = stockPackages,
                        onValueChange = { stockPackages = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text(stringResource(R.string.stock_packages_label, packageUnit)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = stockUnits,
                        onValueChange = { stockUnits = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text(stringResource(R.string.stock_units_label, dosageForm)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))

                // ===== 用药频率 =====
                Text(stringResource(R.string.medication_frequency), style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
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
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = frequencyValue,
                        onValueChange = {
                            frequencyValue = it.filter { c -> c.isDigit() }
                            frequencyValueError = false
                        },
                        modifier = Modifier.width(72.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = frequencyValueError,
                        singleLine = true,
                        supportingText = if (frequencyValueError) {{ Text(stringResource(R.string.error_frequency_must_be_positive)) }} else null
                    )
                    Text(stringResource(R.string.day_unit))
                    Spacer(modifier = Modifier.width(12.dp))
                    OutlinedTextField(
                        value = dailyDosage,
                        onValueChange = {
                            dailyDosage = it.filter { c -> c.isDigit() || c == '.' }
                            dailyDosageError = false
                        },
                        label = { Text(stringResource(R.string.dosage_label)) },
                        modifier = Modifier.width(88.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = dailyDosageError,
                        singleLine = true,
                        supportingText = if (dailyDosageError) {{ Text(stringResource(R.string.error_invalid_dosage)) }} else null
                    )
                    Text(dosageForm)
                }
                Spacer(modifier = Modifier.height(20.dp))

                // ===== 开始日期 =====
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
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
                        val pkgs = stockPackages.toDoubleOrNull() ?: 0.0
                        val units = stockUnits.toDoubleOrNull() ?: 0.0
                        val pkgSize = packageSize.toDoubleOrNull()
                        val totalStock = pkgs * (pkgSize ?: 0.0) + units

                        frequencyValueError = freq == null || freq <= 0
                        dailyDosageError = dosage == null || dosage <= 0

                        if (freq == null || freq <= 0 || dosage == null || dosage <= 0 || pkgSize == null || pkgSize <= 0) return@Button

                        viewModel.updateMedication(
                            med.copy(
                                genericName = genericName,
                                brandName = brandName.takeIf { it.isNotBlank() },
                                specification = specification.takeIf { it.isNotBlank() },
                                packageUnit = packageUnit,
                                dosageForm = dosageForm,
                                packageSize = pkgSize,
                                currentStock = totalStock,
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

/** 格式化库存零头：整数不显示小数点 */
private fun formatStockRemainder(value: Double): String {
    return if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()
}
