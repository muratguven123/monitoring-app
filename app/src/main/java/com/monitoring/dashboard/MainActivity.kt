package com.monitoring.dashboard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.monitoring.dashboard.data.local.SecurePreferencesManager
import com.monitoring.dashboard.ui.navigation.MonitoringNavGraph
import com.monitoring.dashboard.ui.navigation.Route
import com.monitoring.dashboard.ui.theme.MonitoringDashboardTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var securePreferencesManager: SecurePreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MonitoringDashboardTheme {
                Surface(modifier = Modifier.fillMaxSize()) {

                    val navController = rememberNavController()

                    // Eğer Grafana URL ve API key kayıtlıysa direkt listeye, yoksa settings'e git
                    val startDestination by rememberSaveable {
                        val hasCredentials =
                            !securePreferencesManager.getGrafanaBaseUrl().isNullOrBlank() &&
                            !securePreferencesManager.getGrafanaApiKey().isNullOrBlank()
                        mutableStateOf(
                            if (hasCredentials) Route.DASHBOARD_LIST else Route.SETTINGS
                        )
                    }

                    MonitoringNavGraph(
                        navController    = navController,
                        startDestination = startDestination,
                    )
                }
            }
        }
    }
}
