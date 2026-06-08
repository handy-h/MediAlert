package com.handy.medialert

import android.app.Application
import com.handy.medialert.data.database.AppDatabase
import com.handy.medialert.repository.MedicationRepository

class MediAlertApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy {
        MedicationRepository(
            database.medicationDao(),
            database.stockLogDao(),
            this
        )
    }
}
