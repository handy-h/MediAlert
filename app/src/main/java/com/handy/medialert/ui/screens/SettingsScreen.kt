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
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.handy.medialert.calendar.CalendarManager
import com.handy.medialert.viewmodel.MedicationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: MedicationViewModel = viewModel()
) {
    val context = LocalContext.current
    val calendarManager = remember { CalendarManager(context) }
    var selectedCalendarId by remember { mutableStateOf<Long?>(null) }
    var showCalendarDialog by remember { mutableStateOf(false) }
    var exportPath by remember { mutableStateOf("") }

    val calendars = remember { calendarManager.getCalendars() }

    // 权限请求
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions[Manifest.permission.READ_CALENDAR] == true &&
            permissions[Manifest.permission.WRITE_CALENDAR] == true -> {
                showCalendarDialog = true
            }
            else -> {
                Toast.makeText(context, "需要日历权限才能设置提醒", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
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
                .padding(16.dp)
        ) {
            // 日历设置
            ListItem(
                headlineContent = { Text("日历账户") },
                supportingContent = {
                    Text(
                        selectedCalendarId?.let { id ->
                            calendars.find { it.id == id }?.displayName ?: "已选择"
                        } ?: "未设置（提醒将使用默认日历）"
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
            ListItem(
                headlineContent = { Text("导出数据") },
                supportingContent = { Text("将药品数据导出为CSV文件") },
                leadingContent = {
                    Icon(Icons.Default.FileDownload, contentDescription = null)
                },
                modifier = Modifier.clickable {
                    val path = viewModel.exportToCsv(context)
                    exportPath = path ?: "导出失败"
                }
            )

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
                headlineContent = { Text("关于") },
                supportingContent = { Text("药箱库存管家 v1.0") }
            )
        }
    }

    if (showCalendarDialog) {
        AlertDialog(
            onDismissRequest = { showCalendarDialog = false },
            title = { Text("选择日历账户") },
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
                    Text("取消")
                }
            }
        )
    }
}
