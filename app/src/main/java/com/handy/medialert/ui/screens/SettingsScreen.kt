package com.handy.medialert.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import kotlinx.coroutines.launch

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

    var calendars by remember { mutableStateOf(calendarManager.getCalendars()) }

    // CSV 文件选择器
    val filePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            isImporting = true
            importMsg = ""
            coroutineScope.launch {
                val result = viewModel.importFromCsv(context, uri)
                val parts = mutableListOf<String>()
                if (result.successCount > 0) parts.add("成功导入 ${result.successCount} 条")
                if (result.skipCount > 0) parts.add("跳过 ${result.skipCount} 条空行")
                if (result.errors.isNotEmpty()) parts.add("${result.errors.size} 条失败")
                importMsg = if (parts.isEmpty()) "没有数据被导入" else parts.joinToString("，")
                result.errors.take(5).forEach { importMsg += "\n$it" }
                if (result.errors.size > 5) importMsg += "\n...等 ${result.errors.size} 条错误"
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
                calendars = calendarManager.getCalendars()
                showCalendarDialog = true
            }
            else -> {
                Toast.makeText(context, calendarPermissionNeeded, Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
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
                .padding(16.dp)
        ) {
            // 日历设置
            ListItem(
                headlineContent = { Text(stringResource(R.string.calendar_account)) },
                supportingContent = {
                    Text(
                        selectedCalendarId?.let { id ->
                            calendars.find { it.id == id }?.displayName ?: "已选择"
                        } ?: stringResource(R.string.calendar_not_set)
                    )
                },
                leadingContent = {
                    Icon(Icons.Default.CalendarToday, contentDescription = null)
                },
                modifier = Modifier.clickable {
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

            Divider()

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
                    text = "导出路径：$exportPath",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(16.dp)
                )
            }

            Divider()

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

            Divider()

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

            Divider()

            // 关于
            ListItem(
                headlineContent = { Text(stringResource(R.string.about)) },
                supportingContent = { Text(stringResource(R.string.about_text)) }
            )
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
