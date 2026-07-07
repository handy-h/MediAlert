package com.handy.medialert.viewmodel

import android.app.Application
import com.handy.medialert.MediAlertApplication
import com.handy.medialert.TestMedications
import com.handy.medialert.data.entity.FrequencyType
import com.handy.medialert.repository.MedicationRepository
import com.handy.medialert.reminder.ReminderManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MedicationViewModelTest {

    private lateinit var mockApp: MediAlertApplication
    private lateinit var mockRepo: MedicationRepository
    private lateinit var mockReminder: ReminderManager
    private lateinit var viewModel: MedicationViewModel
    private lateinit var scheduler: TestCoroutineScheduler

    @Before
    fun setUp() {
        // 使用显式 scheduler，便于在需要时 advanceUntilIdle() 确保 viewModelScope 协程执行完毕
        scheduler = TestCoroutineScheduler()
        Dispatchers.setMain(UnconfinedTestDispatcher(scheduler))
        mockApp = mockk(relaxed = true)
        mockRepo = mockk(relaxed = true)
        mockReminder = mockk(relaxed = true)
        every { mockApp.repository } returns mockRepo
        every { mockApp.getSharedPreferences(any(), any()) } returns mockk(relaxed = true)
        // 纯 JVM 单测中 R 资源不可用时，统一返回固定文案，便于断言 errorMessage
        every { mockApp.getString(any()) } returns "操作失败，请重试"
        // 将写操作协程调度到测试调度器上，使 MockK suspend mock 的恢复也在 scheduler 内，
        // advanceUntilIdle() 可确定性地驱动 catch 块并设置 errorMessage
        viewModel = MedicationViewModel(mockApp, mockReminder, UnconfinedTestDispatcher(scheduler))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `addMedication 先插入数据库再注册提醒`() {
        val med = TestMedications.sample(id = 1L)
        coEvery { mockRepo.addMedication(any()) } returns 1L
        coEvery { mockRepo.getMedicationById(1L) } returns med

        viewModel.addMedication(
            genericName = med.genericName,
            brandName = null,
            specification = null,
            packageUnit = med.packageUnit,
            dosageForm = med.dosageForm,
            packageSize = med.packageSize,
            currentStock = med.currentStock,
            frequencyType = med.frequencyType,
            frequencyValue = med.frequencyValue,
            dailyDosage = med.dailyDosage,
            startDate = med.startDate
        )

        coVerify { mockRepo.addMedication(any()) }
        coVerify { mockReminder.registerReminders(med, any()) }
    }

    @Test
    fun `addStock 写入库存后重新注册提醒`() {
        val med = TestMedications.sample(id = 2L)
        coEvery { mockRepo.getMedicationById(2L) } returns med

        viewModel.addStock(2L, 5.0, null)

        coVerify { mockRepo.addStock(2L, 5.0, null) }
        coVerify { mockReminder.registerReminders(med, any()) }
    }

    @Test
    fun `reduceStock 写入库存后重新注册提醒`() {
        val med = TestMedications.sample(id = 3L)
        coEvery { mockRepo.getMedicationById(3L) } returns med

        viewModel.reduceStock(3L, 2.0, "原因")

        coVerify { mockRepo.reduceStock(3L, 2.0, "原因") }
        coVerify { mockReminder.registerReminders(med, any()) }
    }

    @Test
    fun `deleteMedication 先取消提醒再删除`() {
        val med = TestMedications.sample(id = 4L)
        coEvery { mockRepo.getMedicationById(4L) } returns med

        viewModel.deleteMedication(4L)

        coVerifyOrder {
            mockReminder.cancelReminders(med)
            mockRepo.deleteMedication(med)
        }
    }

    @Test
    fun `deactivateMedication 先取消提醒再置为停用`() {
        val med = TestMedications.sample(id = 5L)
        coEvery { mockRepo.getMedicationById(5L) } returns med

        viewModel.deactivateMedication(5L)

        coVerify { mockReminder.cancelReminders(med) }
        coVerify { mockRepo.setMedicationActive(5L, false) }
    }

    @Test
    fun `写入失败时在 StateFlow 中暴露错误消息`() = runTest {
        // runTest 的调度器同时作为 Main 与写操作调度器，异常被 ViewModel 的
        // CoroutineExceptionHandler（兜底）捕获并设置 errorMessage，由 UI 通过 Snackbar 展示。
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            coEvery { mockRepo.addMedication(any()) } throws RuntimeException("db failure")
            val vm = MedicationViewModel(mockApp, mockReminder, dispatcher)

            vm.addMedication(
                genericName = "X",
                brandName = null,
                specification = null,
                packageUnit = "盒",
                dosageForm = "片",
                packageSize = 10.0,
                currentStock = 30.0,
                frequencyType = FrequencyType.EVERY_X_DAYS,
                frequencyValue = 1,
                dailyDosage = 1.0,
                startDate = null
            )

            testScheduler.advanceUntilIdle()
            assertEquals("操作失败，请重试", vm.errorMessage.value)
        } finally {
            Dispatchers.resetMain()
        }
    }
}
