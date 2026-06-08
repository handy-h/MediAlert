package com.handy.medialert

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.handy.medialert.ui.screens.AddMedicationScreen
import com.handy.medialert.ui.screens.EditMedicationScreen
import com.handy.medialert.ui.screens.InactiveMedicationsScreen
import com.handy.medialert.ui.screens.MedicationListScreen
import com.handy.medialert.ui.screens.MergedAlertScreen
import com.handy.medialert.ui.screens.SettingsScreen
import com.handy.medialert.ui.theme.MediAlertTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Android 12+ 精确闹钟权限检查
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(ALARM_SERVICE) as android.app.AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                AlertDialog.Builder(this)
                    .setTitle(R.string.exact_alarm_permission_title)
                    .setMessage(R.string.exact_alarm_permission_message)
                    .setPositiveButton(R.string.go_to_settings) { _, _ ->
                        startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                    }
                    .setNegativeButton(R.string.later, null)
                    .show()
            }
        }

        // Android 13+ 通知权限请求
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)
        }

        setContent {
            MediAlertTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MediAlertApp()
                }
            }
        }
    }
}

@Composable
fun MediAlertApp() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "list") {
        composable("list") {
            MedicationListScreen(
                onAddMedication = { navController.navigate("add") },
                onEditMedication = { id -> navController.navigate("edit/$id") },
                onViewInactive = { navController.navigate("inactive") },
                onViewMergedAlerts = { navController.navigate("merged_alerts") },
                onSettings = { navController.navigate("settings") }
            )
        }
        composable("add") {
            AddMedicationScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("edit/{medicationId}") { backStackEntry ->
            val medicationId = backStackEntry.arguments?.getString("medicationId")?.toLongOrNull()
            medicationId?.let {
                EditMedicationScreen(
                    medicationId = it,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
        composable("inactive") {
            InactiveMedicationsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("settings") {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("merged_alerts") {
            MergedAlertScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
