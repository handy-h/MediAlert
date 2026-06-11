package com.handy.medialert.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.handy.medialert.R
import com.handy.medialert.calendar.CalendarManager
import com.handy.medialert.viewmodel.MedicationViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: MedicationViewModel = viewModel()
) {
    val context = LocalContext.current
    val calendarManager = remember { CalendarManager(context) }
    val coroutineScope = rememberCoroutineScope()
    var selectedCalendarId by remember { mutableStateOf(viewModel.getSavedCalendarId()) }
    var showCalendarDialog by remember { mutableStateOf(false) }
    var exportPath by remember { mutableStateOf("") }
    var isExporting by remember { mutableStateOf(false) }
    var importMsg by remember { mutableStateOf("") }
    var isImporting by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }
    var isResetting by remember { mutableStateOf(false) }

    val calendarFallbackWarning = stringResource(R.string.calendar_reminder_fallback)

    // 电池优化检查（Binder IPC 调用，延迟到后台线程避免阻塞主线程）
    var isBatteryOptimized by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val powerManager = context.getSystemService(PowerManager::class.java)
            isBatteryOptimized = !powerManager.isIgnoringBatteryOptimizations(context.packageName)
        }
    }

    // 日历列表延迟加载，避免主线程阻塞导致ANR
    var calendars by remember { mutableStateOf(emptyList<CalendarManager.CalendarInfo>()) }
    var isLoadingCalendars by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        calendars = withContext(Dispatchers.IO) { calendarManager.getCalendars() }
        isLoadingCalendars = false
    }

    // CSV 文件选择器
    val filePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            isImporting = true
            importMsg = ""
            val importSuccessMsg = context.getString(R.string.csv_import_success_count)
            val importSkipMsg = context.getString(R.string.csv_import_skip_count)
            val importFailMsg = context.getString(R.string.csv_import_fail_count)
            val importNoDataMsg = context.getString(R.string.csv_import_no_data)
            val importMoreErrorsMsg = context.getString(R.string.csv_import_more_errors)
            coroutineScope.launch {
                val result = viewModel.importFromCsv(context, uri)
                val parts = mutableListOf<String>()
                if (result.successCount > 0) parts.add(importSuccessMsg.format(result.successCount))
                if (result.skipCount > 0) parts.add(importSkipMsg.format(result.skipCount))
                if (result.errors.isNotEmpty()) parts.add(importFailMsg.format(result.errors.size))
                importMsg = if (parts.isEmpty()) importNoDataMsg else parts.joinToString("，")
                result.errors.take(5).forEach { importMsg += "\n$it" }
                if (result.errors.size > 5) importMsg += importMoreErrorsMsg.format(result.errors.size - 5)
                isImporting = false
            }
        }
    }

    // 权限请求
    val calendarPermissionNeeded = stringResource(R.string.calendar_permission_needed)
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions[Manifest.permission.READ_CALENDAR] == true &&
            permissions[Manifest.permission.WRITE_CALENDAR] == true -> {
                isLoadingCalendars = true
                coroutineScope.launch {
                    calendars = withContext(Dispatchers.IO) { calendarManager.getCalendars() }
                    isLoadingCalendars = false
                    showCalendarDialog = true
                }
            }
            else -> {
                Toast.makeText(context, calendarPermissionNeeded, Toast.LENGTH_SHORT).show()
                // 如果已保存日历但权限被拒，提示回退到仅闹钟
                if (selectedCalendarId != null) {
                    Toast.makeText(context, calendarFallbackWarning, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // 日历设置
            ListItem(
                headlineContent = { Text(stringResource(R.string.calendar_account)) },
                supportingContent = {
                    if (isLoadingCalendars) {
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.loading))
                        }
                    } else {
                        Text(
                            selectedCalendarId?.let { id ->
                                calendars.find { it.id == id }?.displayName ?: stringResource(R.string.selected)
                            } ?: stringResource(R.string.calendar_not_set)
                        )
                    }
                },
                leadingContent = {
                    Icon(Icons.Default.CalendarToday, contentDescription = null)
                },
                modifier = Modifier.clickable {
                    if (isLoadingCalendars) return@clickable
                    when {
                        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED ->
                            showCalendarDialog = true
                        else -> {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.READ_CALENDAR,
                                    Manifest.permission.WRITE_CALENDAR
                                )
                            )
                        }
                    }
                }
            )

            HorizontalDivider()

            // 数据导出
            val exportFailed = stringResource(R.string.export_failed)
            ListItem(
                headlineContent = { Text(stringResource(R.string.export_data)) },
                supportingContent = { Text(stringResource(R.string.export_description)) },
                leadingContent = {
                    Icon(Icons.Default.FileDownload, contentDescription = null)
                },
                modifier = Modifier.clickable {
                    if (!isExporting) {
                        isExporting = true
                        exportPath = ""
                        coroutineScope.launch {
                            val (path, error) = viewModel.exportToCsv(context)
                            exportPath = path ?: error ?: exportFailed
                            isExporting = false
                        }
                    }
                }
            )

            if (isExporting) {
                Text(
                    text = stringResource(R.string.exporting),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(16.dp)
                )
            }

            if (exportPath.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.export_path_prefix, exportPath),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(16.dp)
                )
            }

            HorizontalDivider()

            // 数据导入
            ListItem(
                headlineContent = { Text(stringResource(R.string.import_data)) },
                supportingContent = { Text(stringResource(R.string.import_description)) },
                leadingContent = {
                    Icon(Icons.Default.FileOpen, contentDescription = null)
                },
                modifier = Modifier.clickable {
                    if (!isImporting) {
                        filePickerLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "*/*"))
                    }
                }
            )

            if (isImporting) {
                Text(
                    text = stringResource(R.string.importing),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(16.dp)
                )
            }

            if (importMsg.isNotEmpty()) {
                Text(
                    text = importMsg,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(16.dp)
                )
            }

            HorizontalDivider()

            // 重设所有提醒
            ListItem(
                headlineContent = { Text(stringResource(R.string.reset_reminders)) },
                supportingContent = { Text(stringResource(R.string.reset_reminders_desc)) },
                leadingContent = {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                },
                modifier = Modifier.clickable {
                    if (!isResetting) {
                        showResetConfirm = true
                    }
                }
            )

            if (isResetting) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))
            }

            HorizontalDivider()

            // 关于
            ListItem(
                headlineContent = { Text(stringResource(R.string.about)) },
                supportingContent = { Text(stringResource(R.string.about_text)) }
            )

            HorizontalDivider()

            // 电池优化（后台保活）
            if (isBatteryOptimized) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.battery_optimization_title)) },
                    supportingContent = { Text(stringResource(R.string.battery_optimization_message)) },
                    leadingContent = {
                        Icon(Icons.Default.BatterySaver, contentDescription = null)
                    },
                    modifier = Modifier.clickable {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            context.startActivity(intent)
                        }
                    }
                )
            }

            // 精确闹钟权限状态（Android 12+）
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val alarmManager = context.getSystemService(android.app.AlarmManager::class.java)
                if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.exact_alarm_permission_title)) },
                        supportingContent = { Text(stringResource(R.string.exact_alarm_permission_message)) },
                        leadingContent = {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                        },
                        modifier = Modifier.clickable {
                            context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                        }
                    )
                }
            }
        }
    }

    if (showCalendarDialog) {
        AlertDialog(
            onDismissRequest = { showCalendarDialog = false },
            title = { Text(stringResource(R.string.select_calendar)) },
            text = {
                Column {
                    calendars.forEach { calendar ->
                        TextButton(
                            onClick = {
                                selectedCalendarId = calendar.id
                                viewModel.saveCalendarId(calendar.id)
                                showCalendarDialog = false
                                // 重新注册所有提醒
                                viewModel.refreshAllReminders(calendar.id)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("${calendar.displayName} (${calendar.accountName})")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCalendarDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // 重设提醒确认对话框
    if (showResetConfirm) {
        val resetSuccess = stringResource(R.string.reset_reminders_success)
        val resetEmpty = stringResource(R.string.reset_reminders_empty)
        val resetFailed = stringResource(R.string.reset_reminders_failed)
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text(stringResource(R.string.reset_reminders_confirm_title)) },
            text = { Text(stringResource(R.string.reset_reminders_confirm_msg)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetConfirm = false
                        isResetting = true
                        viewModel.resetAllReminders { count, error ->
                            isResetting = false
                            val msg = when {
                                error != null -> resetFailed.format(error)
                                count == 0 -> resetEmpty
                                else -> resetSuccess.format(count)
                            }
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                    }
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
