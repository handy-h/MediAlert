package com.handy.medialert.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.handy.medialert.R
import com.handy.medialert.viewmodel.MedicationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditMedicationScreen(
    medicationId: Long,
    onNavigateBack: () -> Unit,
    viewModel: MedicationViewModel = viewModel()
) {
    val medication by viewModel.getMedicationFlow(medicationId)
        .collectAsStateWithLifecycle(initialValue = null)
    var showDeleteConfirm by remember { mutableStateOf(false) }

    medication?.let { med ->
        var genericName by remember { mutableStateOf(med.genericName) }
        var brandName by remember { mutableStateOf(med.brandName ?: "") }
        var specification by remember { mutableStateOf(med.specification ?: "") }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.edit_medication)) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
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
                OutlinedTextField(
                    value = genericName,
                    onValueChange = { genericName = it },
                    label = { Text(stringResource(R.string.generic_name)) },
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
                    label = { Text(stringResource(R.string.specification)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        viewModel.updateMedication(
                            med.copy(
                                genericName = genericName,
                                brandName = brandName.takeIf { it.isNotBlank() },
                                specification = specification.takeIf { it.isNotBlank() }
                            )
                        )
                        onNavigateBack()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.save_changes))
                }
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
