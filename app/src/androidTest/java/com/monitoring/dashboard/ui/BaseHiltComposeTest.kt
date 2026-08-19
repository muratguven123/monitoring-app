package com.monitoring.dashboard.ui

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.lifecycle.ViewModelProvider
import com.monitoring.dashboard.MainActivity
import com.monitoring.dashboard.R
import com.monitoring.dashboard.data.DataRefreshBus
import com.monitoring.dashboard.data.local.SecurePreferencesManager
import com.monitoring.dashboard.data.local.dao.AlertDao
import com.monitoring.dashboard.data.local.dao.GrafanaDao
import com.monitoring.dashboard.data.local.dao.NewRelicDao
import com.monitoring.dashboard.data.local.entity.AlertViolationEntity
import dagger.hilt.android.testing.HiltAndroidRule
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import javax.inject.Inject

abstract class BaseHiltComposeTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Inject lateinit var securePreferencesManager: SecurePreferencesManager
    @Inject lateinit var appLockController: AppLockController
    @Inject lateinit var dataRefreshBus: DataRefreshBus
    @Inject lateinit var alertDao: AlertDao
    @Inject lateinit var grafanaDao: GrafanaDao
    @Inject lateinit var newRelicDao: NewRelicDao

    @Before
    fun resetAppState() {
        hiltRule.inject()
        securePreferencesManager.clearAll()
        appLockController.onAppLockSettingChanged(false)
        appLockController.unlock()
        dataRefreshBus.requestRefresh()
        runBlocking {
            alertDao.deleteAll()
            grafanaDao.deleteAll()
            newRelicDao.deleteAll()
        }
        composeRule.runOnUiThread {
            ViewModelProvider(composeRule.activity)[MainViewModel::class.java].refreshGates()
        }
        composeRule.waitForIdle()
        composeRule.waitUntil(15_000) {
            composeRule.onAllNodesWithTag(TestTags.GRAFANA_URL_FIELD)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    @After
    fun releaseLockForNextTest() {
        if (this::appLockController.isInitialized) {
            appLockController.onAppLockSettingChanged(false)
            appLockController.unlock()
        }
    }

    protected fun str(id: Int): String = composeRule.activity.getString(id)

    protected fun waitForText(text: String, timeoutMs: Long = 15_000) {
        composeRule.waitUntil(timeoutMs) {
            composeRule.onAllNodes(hasText(text, substring = false))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    protected fun enterTaggedText(tag: String, value: String) {
        composeRule.onNodeWithTag(tag).performTextReplacement(value)
    }

    protected fun clickNav(labelRes: Int) {
        composeRule.onNodeWithContentDescription(str(labelRes), useUnmergedTree = true)
            .performClick()
        composeRule.waitForIdle()
    }

    protected fun completeOnboardingWithGrafana(
        url: String = "grafana.example.com",
        apiKey: String = "glsa_test_key",
    ) {
        enterTaggedText(TestTags.GRAFANA_URL_FIELD, url)
        enterTaggedText(TestTags.GRAFANA_KEY_FIELD, apiKey)
        composeRule.onNodeWithText(str(R.string.action_next)).performClick()
        composeRule.onNodeWithText(str(R.string.action_next)).performClick()
        composeRule.onNodeWithText(str(R.string.action_skip_for_now)).performClick()
        composeRule.onNodeWithText(str(R.string.action_connect_and_save)).performClick()
        waitForText(str(R.string.screen_home_title))
    }

    protected fun completeOnboardingWithNewRelicOnly(apiKey: String = "NRAK-test") {
        composeRule.onNodeWithText(str(R.string.action_skip_grafana)).performClick()
        enterTaggedText(TestTags.NEW_RELIC_KEY_FIELD, apiKey)
        composeRule.onNodeWithText(str(R.string.action_next)).performClick()
        composeRule.onNodeWithText(str(R.string.action_skip_for_now)).performClick()
        composeRule.onNodeWithText(str(R.string.action_connect_and_save)).performClick()
        waitForText(str(R.string.screen_home_title))
    }

    protected fun seedResolvedAlert() {
        runBlocking {
            alertDao.insertAll(
                listOf(
                    AlertViolationEntity(
                        id = 99L,
                        label = "Disk Full",
                        policyName = "Prod policy",
                        conditionName = "Disk",
                        openedAt = 500L,
                        severity = "critical",
                        isOpen = false,
                        resolvedAt = 800L,
                    ),
                ),
            )
        }
    }
}
