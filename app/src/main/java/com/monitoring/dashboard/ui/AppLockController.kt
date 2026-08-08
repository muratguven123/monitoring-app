package com.monitoring.dashboard.ui

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.monitoring.dashboard.data.local.SecurePreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Relocks the UI when the app goes to background if biometric/PIN lock is enabled.
 */
@Singleton
class AppLockController @Inject constructor(
    private val securePreferencesManager: SecurePreferencesManager,
) : DefaultLifecycleObserver {

    private val _isLocked = MutableStateFlow(
        securePreferencesManager.isAppLockEnabled() && securePreferencesManager.isOnboardingComplete(),
    )
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    fun start() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        if (securePreferencesManager.isAppLockEnabled() &&
            securePreferencesManager.isOnboardingComplete()
        ) {
            _isLocked.value = true
        }
    }

    fun unlock() {
        _isLocked.value = false
    }

    fun onAppLockSettingChanged(enabled: Boolean) {
        _isLocked.value = enabled && securePreferencesManager.isOnboardingComplete()
    }

    override fun onStop(owner: LifecycleOwner) {
        if (securePreferencesManager.isAppLockEnabled() &&
            securePreferencesManager.isOnboardingComplete()
        ) {
            _isLocked.value = true
        }
    }
}
