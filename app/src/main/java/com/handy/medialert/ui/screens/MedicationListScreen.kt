package com.handy.medialert.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.handy.medialert.R
import com.handy.medialert.data.entity.Medication
import com.handy.medialert.ui.components.EmptyState
import com.handy.medialert.ui.components.MedicationCard
import com.handy.medialert.ui.components.StockInputDialog
import com.handy.medialert.viewmodel.MedicationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationListScreen(
    onAddMedication: () -> Unit,
    onEditMedication: (Long) -> Unit,
    onViewInactive: () -> Unit,
    onViewMergedAlerts: () -> Unit,
    onSettings: () -> Unit,
    viewModel: MedicationViewModel = viewModel()
) {
    val medications by viewModel.activeMedications.collectAsStateWithLifecycle()
    var showAddStockDialog by remember { mutableStateOf(false) }
    var showReduceStockDialog by remember { mutableStateOf(false) }
    var selectedMedication by remember { mutableStateOf<Medication?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearErrorMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_home)) },
                actions = {
                    IconButton(onClick = onViewMergedAlerts) {
                        Icon(Icons.Default.Notifications, contentDescription = stringResource(R.string.view_merged_alerts))
                    }
                    IconButton(onClick = onViewInactive) {
                        Icon(Icons.Default.Archive, contentDescription = stringResource(R.string.view_inactive))
                    }
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddMedication) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_medication))
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        if (medications.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.no_medications),
                subtitle = stringResource(R.string.no_medications_hint),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        } else {
            // 使用 remember 缓存排序结果，避免每次重组都重新计算 daysUntilDepletion()
            val sortedMedications = remember(medications) {
                medications.sortedBy { it.daysUntilDepletion() }
            }
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(
                    items = sortedMedications,
                    key = { it.id }
                ) { medication ->
                    MedicationCard(
                        medication = medication,
                        onAddStock = {
                            selectedMedication = medication
                            showAddStockDialog = true
                        },
                        onReduceStock = {
                            selectedMedication = medication
                            showReduceStockDialog = true
                        },
                        onDeactivate = {
                            viewModel.deactivateMedication(medication.id)
                        },
                        onEdit = {
                            onEditMedication(medication.id)
                        }
                    )
                }
            }
        }
    }

    if (showAddStockDialog) {
        selectedMedication?.let { med ->
            StockInputDialog(
                title = stringResource(R.string.stock_label),
                medication = med,
                isAddition = true,
                onConfirm = { quantity, reason ->
                    viewModel.addStock(med.id, quantity, reason)
                    showAddStockDialog = false
                    selectedMedication = null
                },
                onDismiss = {
                    showAddStockDialog = false
                    selectedMedication = null
                }
            )
        }
    }

    if (showReduceStockDialog) {
        selectedMedication?.let { med ->
            StockInputDialog(
                title = stringResource(R.string.advance_consume),
                medication = med,
                isAddition = false,
                onConfirm = { quantity, reason ->
                    viewModel.reduceStock(med.id, quantity, reason)
                    showReduceStockDialog = false
                    selectedMedication = null
                },
                onDismiss = {
                    showReduceStockDialog = false
                    selectedMedication = null
                }
            )
        }
    }
}
