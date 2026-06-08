package com.handy.medialert.repository

import android.content.Context
import com.handy.medialert.data.dao.MedicationDao
import com.handy.medialert.data.dao.StockLogDao
import com.handy.medialert.data.entity.Medication
import com.handy.medialert.data.entity.StockLog
import com.handy.medialert.data.entity.StockLogType
import kotlinx.coroutines.flow.Flow

class MedicationRepository(
    private val medicationDao: MedicationDao,
    private val stockLogDao: StockLogDao,
    private val context: Context
) {
    fun getAllActiveMedications(): Flow<List<Medication>> = medicationDao.getAllActive()
    fun getAllInactiveMedications(): Flow<List<Medication>> = medicationDao.getAllInactive()
    fun getAllMedications(): Flow<List<Medication>> = medicationDao.getAll()
    suspend fun getMedicationById(id: Long): Medication? = medicationDao.getById(id)

    suspend fun addMedication(medication: Medication): Long {
        return medicationDao.insert(medication)
    }

    suspend fun updateMedication(medication: Medication) {
        medicationDao.update(medication)
    }

    suspend fun deleteMedication(medication: Medication) {
        medicationDao.delete(medication)
    }

    suspend fun addStock(medicationId: Long, quantity: Double, reason: String? = null) {
        val medication = medicationDao.getById(medicationId) ?: return
        val newStock = medication.currentStock + quantity
        medicationDao.updateStock(medicationId, newStock)
        stockLogDao.insert(
            StockLog(
                medicationId = medicationId,
                type = StockLogType.PURCHASE,
                quantity = quantity,
                reason = reason
            )
        )
    }

    suspend fun reduceStock(medicationId: Long, quantity: Double, reason: String? = null) {
        val medication = medicationDao.getById(medicationId) ?: return
        val newStock = (medication.currentStock - quantity).coerceAtLeast(0.0)
        medicationDao.updateStock(medicationId, newStock)
        stockLogDao.insert(
            StockLog(
                medicationId = medicationId,
                type = StockLogType.LOST,
                quantity = -quantity,
                reason = reason
            )
        )
    }

    suspend fun setMedicationActive(medicationId: Long, active: Boolean) {
        medicationDao.setActive(medicationId, active)
    }
}
