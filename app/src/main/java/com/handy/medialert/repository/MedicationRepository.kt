package com.handy.medialert.repository

import com.handy.medialert.data.dao.MedicationDao
import com.handy.medialert.data.dao.StockLogDao
import com.handy.medialert.data.entity.Medication
import com.handy.medialert.data.entity.StockLog
import com.handy.medialert.data.entity.StockLogType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn

class MedicationRepository(
    private val medicationDao: MedicationDao,
    private val stockLogDao: StockLogDao
) {
    fun getAllActiveMedications(): Flow<List<Medication>> = medicationDao.getAllActive().flowOn(Dispatchers.IO)
    fun getAllInactiveMedications(): Flow<List<Medication>> = medicationDao.getAllInactive().flowOn(Dispatchers.IO)
    fun getAllMedications(): Flow<List<Medication>> = medicationDao.getAll().flowOn(Dispatchers.IO)
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
                type = StockLogType.ADJUSTMENT,
                quantity = -quantity,
                reason = reason
            )
        )
    }

    suspend fun setMedicationActive(medicationId: Long, active: Boolean) {
        medicationDao.setActive(medicationId, active)
    }
}
