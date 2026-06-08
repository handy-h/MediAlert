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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
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

    val packageUnits = listOf("盒", "支", "瓶")
    val dosageForms = listOf("片", "粒", "ml", "g")
    var packageUnitExpanded by remember { mutableStateOf(false) }
    var dosageFormExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("添加药品") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
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
                label = { Text("通用名 *") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = brandName,
                onValueChange = { brandName = it },
                label = { Text("商品名") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = specification,
                onValueChange = { specification = it },
                label = { Text("规格（如：80/12.5）") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            // 包装信息
            Text("包装信息", style = MaterialTheme.typography.titleMedium)
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
                        label = { Text("包装") },
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
                        label = { Text("剂型") },
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
            Text("当前库存", style = MaterialTheme.typography.titleMedium)
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
            Text("用药频率", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = frequencyType == FrequencyType.EVERY_X_DAYS,
                    onClick = { frequencyType = FrequencyType.EVERY_X_DAYS },
                    label = { Text("每X天") }
                )
                FilterChip(
                    selected = frequencyType == FrequencyType.EVERY_XTH_DAY,
                    onClick = { frequencyType = FrequencyType.EVERY_XTH_DAY },
                    label = { Text("每隔X天") }
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
                    onValueChange = { frequencyValue = it.filter { c -> c.isDigit() } },
                    modifier = Modifier.width(80.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
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
                    onValueChange = { dailyDosage = it },
                    label = { Text("剂量") },
                    modifier = Modifier.width(100.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
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
                Text("设置开始服用日期（不勾选则立即开始）")
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
                    val pkgSize = packageSize.toIntOrNull() ?: return@Button
                    val pkgs = currentStockPackages.toIntOrNull() ?: 0
                    val units = currentStockUnits.toIntOrNull() ?: 0
                    val totalStock = pkgs * pkgSize + units
                    val freq = frequencyValue.toIntOrNull() ?: 1
                    val dosage = dailyDosage.toDoubleOrNull() ?: 1.0

                    viewModel.addMedication(
                        genericName = genericName,
                        brandName = brandName.takeIf { it.isNotBlank() },
                        specification = specification.takeIf { it.isNotBlank() },
                        packageUnit = packageUnit,
                        dosageForm = dosageForm,
                        packageSize = pkgSize,
                        currentStock = totalStock.toDouble(),
                        frequencyType = frequencyType,
                        frequencyValue = freq,
                        dailyDosage = dosage,
                        startDate = if (hasStartDate) startDate else null
                    )
                    onNavigateBack()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = genericName.isNotBlank() && packageSize.isNotBlank()
            ) {
                Text("保存药品")
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
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("取消")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
