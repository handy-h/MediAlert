package com.handy.medialert.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.handy.medialert.R
import com.handy.medialert.data.entity.FrequencyType
import com.handy.medialert.viewmodel.MedicationViewModel
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMedicationScreen(
    onNavigateBack: () -> Unit,
    viewModel: MedicationViewModel = viewModel()
) {
    var genericName by remember { mutableStateOf("") }
    var brandName by remember { mutableStateOf("") }
    var specification by remember { mutableStateOf("") }
    var packageUnit by remember { mutableStateOf("盒") }
    var dosageForm by remember { mutableStateOf("片") }
    var packageSize by remember { mutableStateOf("") }
    var currentStockPackages by remember { mutableStateOf("") }
    var currentStockUnits by remember { mutableStateOf("") }
    var frequencyType by remember { mutableStateOf(FrequencyType.EVERY_X_DAYS) }
    var frequencyValue by remember { mutableStateOf("1") }
    var dailyDosage by remember { mutableStateOf("1") }
    var hasStartDate by remember { mutableStateOf(false) }
    var startDate by remember { mutableStateOf(LocalDate.now()) }
    var showDatePicker by remember { mutableStateOf(false) }

    // 验证错误状态
    var frequencyValueError by remember { mutableStateOf(false) }
    var dailyDosageError by remember { mutableStateOf(false) }

    val packageUnits = listOf("盒", "支", "瓶")
    val dosageForms = listOf("片", "粒", "ml", "g")
    var packageUnitExpanded by remember { mutableStateOf(false) }
    var dosageFormExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_medication)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
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
            // 药品名称
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
                // 最小销售包装
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
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
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

                Spacer(modifier = Modifier.width(8.dp))

                // 剂型
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
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
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
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = packageSize,
                onValueChange = { packageSize = it.filter { c -> c.isDigit() } },
                label = { Text("每${packageUnit}含多少${dosageForm} *") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            // 当前库存
            Text(stringResource(R.string.current_stock), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = currentStockPackages,
                    onValueChange = { currentStockPackages = it.filter { c -> c.isDigit() } },
                    label = { Text("${packageUnit}数") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = currentStockUnits,
                    onValueChange = { currentStockUnits = it.filter { c -> c.isDigit() } },
                    label = { Text("零散${dosageForm}数") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            // 用药频率
            Text(stringResource(R.string.medication_frequency), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = frequencyType == FrequencyType.EVERY_X_DAYS,
                    onClick = { frequencyType = FrequencyType.EVERY_X_DAYS },
                    label = { Text(stringResource(R.string.every_x_days)) }
                )
                FilterChip(
                    selected = frequencyType == FrequencyType.EVERY_XTH_DAY,
                    onClick = { frequencyType = FrequencyType.EVERY_XTH_DAY },
                    label = { Text(stringResource(R.string.every_xth_day)) }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Text(
                    when (frequencyType) {
                        FrequencyType.EVERY_X_DAYS -> "每"
                        FrequencyType.EVERY_XTH_DAY -> "每隔"
                    }
                )
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
                        dailyDosage = it
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
                    Text("开始日期：${startDate}")
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            // 保存按钮
            Button(
                onClick = {
                    val freq = frequencyValue.toIntOrNull()
                    val dosage = dailyDosage.toDoubleOrNull()

                    // 验证频率值
                    frequencyValueError = freq == null || freq <= 0
                    dailyDosageError = dosage == null || dosage <= 0

                    if (frequencyValueError || dailyDosageError) return@Button

                    val pkgSize = packageSize.toIntOrNull() ?: return@Button
                    val pkgs = currentStockPackages.toIntOrNull() ?: 0
                    val units = currentStockUnits.toIntOrNull() ?: 0
                    val totalStock = pkgs * pkgSize + units

                    viewModel.addMedication(
                        genericName = genericName,
                        brandName = brandName.takeIf { it.isNotBlank() },
                        specification = specification.takeIf { it.isNotBlank() },
                        packageUnit = packageUnit,
                        dosageForm = dosageForm,
                        packageSize = pkgSize,
                        currentStock = totalStock.toDouble(),
                        frequencyType = frequencyType,
                        frequencyValue = freq!!,
                        dailyDosage = dosage!!,
                        startDate = if (hasStartDate) startDate else null
                    )
                    onNavigateBack()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = genericName.isNotBlank() && packageSize.isNotBlank()
            ) {
                Text(stringResource(R.string.save_medication))
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = startDate.toEpochDay() * 86400000
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        startDate = java.time.Instant.ofEpochMilli(millis)
                            .atZone(java.time.ZoneId.systemDefault())
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
