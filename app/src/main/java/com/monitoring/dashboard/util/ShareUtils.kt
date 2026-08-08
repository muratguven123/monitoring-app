package com.monitoring.dashboard.util

import android.content.Context
import android.content.Intent
import com.monitoring.dashboard.domain.model.AlertViolation
import org.json.JSONArray
import org.json.JSONObject

object ShareUtils {

    fun shareText(context: Context, text: String, title: String = "Share") {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, title))
    }

    fun buildAlertSummary(violations: List<AlertViolation>): String {
        if (violations.isEmpty()) return "No open alert violations."
        return buildString {
            appendLine("Open Alert Violations (${violations.size})")
            appendLine("—".repeat(24))
            violations.forEach { v ->
                appendLine("• [${v.severity?.uppercase() ?: "ALERT"}] ${v.label ?: v.conditionName}")
                v.policyName?.let { appendLine("  Policy: $it") }
            }
        }
    }

    fun buildAlertJson(violations: List<AlertViolation>): String {
        val arr = JSONArray()
        violations.forEach { v ->
            arr.put(
                JSONObject()
                    .put("id", v.id)
                    .put("label", v.label)
                    .put("policy", v.policyName)
                    .put("severity", v.severity)
                    .put("open", v.isOpen)
                    .put("openedAt", v.openedAt),
            )
        }
        return JSONObject().put("violations", arr).put("exportedAt", System.currentTimeMillis()).toString(2)
    }
}
