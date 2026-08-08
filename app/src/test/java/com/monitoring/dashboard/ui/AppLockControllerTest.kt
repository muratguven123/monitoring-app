package com.monitoring.dashboard.ui

import androidx.lifecycle.LifecycleOwner
import com.monitoring.dashboard.data.local.SecurePreferencesManager
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AppLockControllerTest {

    private lateinit var securePreferencesManager: SecurePreferencesManager
    private lateinit var controller: AppLockController
    private val owner: LifecycleOwner = mockk(relaxed = true)

    @Before
    fun setup() {
        securePreferencesManager = mockk(relaxed = true)
        every { securePreferencesManager.isOnboardingComplete() } returns true
        every { securePreferencesManager.isAppLockEnabled() } returns true
        controller = AppLockController(securePreferencesManager)
    }

    @Test
    fun `starts locked when app lock enabled`() {
        assertTrue(controller.isLocked.value)
    }

    @Test
    fun `unlock clears lock flag`() {
        controller.unlock()
        assertFalse(controller.isLocked.value)
    }

    @Test
    fun `onStop relocks when app lock enabled`() {
        controller.unlock()
        assertFalse(controller.isLocked.value)

        controller.onStop(owner)
        assertTrue(controller.isLocked.value)
    }

    @Test
    fun `onStop does not lock when app lock disabled`() {
        every { securePreferencesManager.isAppLockEnabled() } returns false
        controller = AppLockController(securePreferencesManager)
        controller.unlock()

        controller.onStop(owner)
        assertFalse(controller.isLocked.value)
    }

    @Test
    fun `onAppLockSettingChanged updates lock state`() {
        controller.unlock()
        controller.onAppLockSettingChanged(enabled = true)
        assertTrue(controller.isLocked.value)

        controller.onAppLockSettingChanged(enabled = false)
        assertFalse(controller.isLocked.value)
    }
}
