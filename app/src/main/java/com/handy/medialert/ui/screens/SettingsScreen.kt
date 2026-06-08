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
    var selectedCalendarId by remember { mutableStateOf<Long?>(null) }
    var showCalendarDialog by remember { mutableStateOf(false) }
    var exportPath by remember { mutableStateOf("") }
    var isExporting by remember { mutableStateOf(false) }

    var calendars by remember { mutableStateOf(calendarManager.getCalendars()) }

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
                            val path = viewModel.exportToCsv(context)
                            exportPath = path ?: exportFailed
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
}
