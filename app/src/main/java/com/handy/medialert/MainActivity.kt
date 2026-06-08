package com.handy.medialert

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
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
