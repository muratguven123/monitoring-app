package com.monitoring.dashboard.domain.usecase

import com.monitoring.dashboard.data.local.NotificationPreferences
import com.monitoring.dashboard.data.local.UserPreferencesRepository
import com.monitoring.dashboard.domain.model.AlertViolation
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar

class ShouldNotifyViolationUseCaseTest {

    private lateinit var userPreferencesRepository: UserPreferencesRepository
    private lateinit var useCase: ShouldNotifyViolationUseCase

    private val critical = AlertViolation(
        id = 1L,
        label = "CPU",
        policyName = "Infra",
        conditionName = null,
        severity = "critical",
        openedAt = 1L,
        isOpen = true,
        resolvedAt = null,
    )
    private val warning = critical.copy(id = 2L, severity = "warning")

    @Before
    fun setup() {
        userPreferencesRepository = mockk()
        coEvery { userPreferencesRepository.clearExpiredMutes() } just runs
        every { userPreferencesRepository.mutedViolationUntil } returns flowOf(emptyMap())
        useCase = ShouldNotifyViolationUseCase(userPreferencesRepository)
    }

    @Test
    fun `returns all violations when prefs allow`() = runTest {
        every { userPreferencesRepository.notificationPreferences } returns flowOf(
            NotificationPreferences(criticalOnly = false, quietHoursEnabled = false),
        )

        val result = useCase(listOf(critical, warning))
        assertEquals(2, result.size)
    }

    @Test
    fun `criticalOnly filters non-critical`() = runTest {
        every { userPreferencesRepository.notificationPreferences } returns flowOf(
            NotificationPreferences(criticalOnly = true, quietHoursEnabled = false),
        )

        val result = useCase(listOf(critical, warning))
        assertEquals(listOf(critical), result)
    }

    @Test
    fun `quiet hours suppress all notifications`() = runTest {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        // Build a window that always includes the current hour
        every { userPreferencesRepository.notificationPreferences } returns flowOf(
            NotificationPreferences(
                quietHoursEnabled = true,
                quietHoursStartHour = hour,
                quietHoursEndHour = (hour + 2) % 24,
            ),
        )

        val result = useCase(listOf(critical, warning))
        assertTrue(result.isEmpty())
    }

    @Test
    fun `muted violation is excluded`() = runTest {
        every { userPreferencesRepository.notificationPreferences } returns flowOf(
            NotificationPreferences(criticalOnly = false, quietHoursEnabled = false),
        )
        every { userPreferencesRepository.mutedViolationUntil } returns flowOf(
            mapOf(1L to System.currentTimeMillis() + 60_000L),
        )

        val result = useCase(listOf(critical, warning))
        assertEquals(listOf(warning), result)
    }
}
