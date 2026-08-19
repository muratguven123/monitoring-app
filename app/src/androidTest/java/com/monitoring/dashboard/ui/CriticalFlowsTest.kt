package com.monitoring.dashboard.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.monitoring.dashboard.R
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class CriticalFlowsTest : BaseHiltComposeTest() {

    @Test
    fun onboardingSavesGrafanaUrlAndApiKey() {
        completeOnboardingWithGrafana()

        clickNav(R.string.nav_settings)
        composeRule.onNodeWithText(str(R.string.settings_grafana_section)).assertIsDisplayed()
        composeRule.onNodeWithText(
            str(R.string.settings_grafana_url_resolved).format("https://grafana.example.com/"),
        ).assertIsDisplayed()
    }

    @Test
    fun grafanaUrlValidationShowsErrorThenNormalizedValue() {
        completeOnboardingWithNewRelicOnly()

        clickNav(R.string.nav_settings)

        composeRule.onNodeWithTag(TestTags.GRAFANA_URL_FIELD)
            .performTextReplacement("ftp://not-supported")
        composeRule.onNodeWithText(str(R.string.settings_grafana_url_error_scheme))
            .assertIsDisplayed()

        composeRule.onNodeWithTag(TestTags.GRAFANA_URL_FIELD)
            .performTextReplacement("grafana.example.com")
        composeRule.onNodeWithText(
            str(R.string.settings_grafana_url_resolved).format("https://grafana.example.com/"),
        ).assertIsDisplayed()
    }

    @Test
    fun homeUnconfiguredCardOpensSettings() {
        completeOnboardingWithNewRelicOnly()

        waitForText(str(R.string.home_grafana_unconfigured_title))
        composeRule.onNode(
            hasText(str(R.string.home_grafana_unconfigured_title), substring = false) and
                hasClickAction(),
        ).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(str(R.string.settings_grafana_section)).assertIsDisplayed()
    }

    @Test
    fun alertsFiltersOpenCriticalAndResolved() {
        completeOnboardingWithNewRelicOnly()
        seedResolvedAlert()

        clickNav(R.string.screen_alerts_title)
        waitForText("CPU High")

        composeRule.onNodeWithText("CPU High").assertIsDisplayed()
        composeRule.onNodeWithText("Apdex Low").assertIsDisplayed()
        assertNoText("Disk Full")

        composeRule.onNodeWithText(str(R.string.alert_filter_critical)).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("CPU High").assertIsDisplayed()
        assertNoText("Apdex Low")

        composeRule.onNodeWithText(str(R.string.alert_filter_resolved)).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Disk Full").assertIsDisplayed()
        assertNoText("CPU High")

        composeRule.onNodeWithText(str(R.string.alert_filter_open)).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("CPU High").assertIsDisplayed()
        composeRule.onNodeWithText("Apdex Low").assertIsDisplayed()
    }

    @Test
    fun enablingAppLockShowsLockScreen() {
        completeOnboardingWithNewRelicOnly()

        clickNav(R.string.nav_settings)
        waitForText(str(R.string.settings_grafana_section))

        composeRule.waitUntil(10_000) {
            val found = composeRule.onAllNodesWithTag(TestTags.APP_LOCK_SWITCH)
                .fetchSemanticsNodes()
                .isNotEmpty()
            if (!found) {
                composeRule.onNodeWithTag(TestTags.SETTINGS_LIST)
                    .performTouchInput { swipeUp() }
            }
            found
        }
        composeRule.onNodeWithTag(TestTags.APP_LOCK_SWITCH).performClick()

        waitForText(str(R.string.lock_title))
        composeRule.onNodeWithText(str(R.string.lock_title)).assertIsDisplayed()
        composeRule.onNodeWithText(str(R.string.lock_subtitle)).assertIsDisplayed()
    }

    @Test
    fun bottomNavigationOpensMainScreensAndBackReturnsHome() {
        completeOnboardingWithGrafana()

        clickNav(R.string.nav_grafana)
        waitForText(str(R.string.screen_grafana_dashboards_title))

        clickNav(R.string.nav_newrelic)
        waitForText(str(R.string.screen_newrelic_apps_title))

        clickNav(R.string.screen_alerts_title)
        waitForText(str(R.string.screen_alerts_title))

        clickNav(R.string.nav_settings)
        waitForText(str(R.string.settings_grafana_section))

        composeRule.runOnUiThread {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText(str(R.string.screen_home_title)).assertIsDisplayed()

        clickNav(R.string.nav_dashboard)
        composeRule.onNodeWithText(str(R.string.screen_home_title)).assertIsDisplayed()
    }

    private fun assertNoText(text: String) {
        assertTrue(
            "Expected no node with text '$text'",
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isEmpty(),
        )
    }
}
