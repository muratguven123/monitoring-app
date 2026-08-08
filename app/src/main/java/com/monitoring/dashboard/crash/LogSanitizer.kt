package com.monitoring.dashboard.crash

/**
 * Redacts common API token patterns from log lines before they hit Logcat / Crashlytics.
 */
object LogSanitizer {
    private val patterns = listOf(
        Regex("""Bearer [A-Za-z0-9\-._~+/]+=*"""),
        Regex("""Api-Key:\s*[A-Za-z0-9\-._]+""", RegexOption.IGNORE_CASE),
        Regex("""NRAK-[A-Za-z0-9]+"""),
        Regex("""ghp_[A-Za-z0-9]+"""),
        Regex("""github_pat_[A-Za-z0-9_]+"""),
        Regex("""gho_[A-Za-z0-9]+"""),
    )

    fun sanitize(message: String): String {
        var result = message
        for (pattern in patterns) {
            result = pattern.replace(result) { match ->
                when {
                    match.value.startsWith("Bearer", ignoreCase = true) -> "Bearer [REDACTED]"
                    match.value.contains("Api-Key", ignoreCase = true) -> "Api-Key: [REDACTED]"
                    match.value.startsWith("NRAK-") -> "NRAK-[REDACTED]"
                    match.value.startsWith("ghp_") -> "ghp_[REDACTED]"
                    match.value.startsWith("github_pat_") -> "github_pat_[REDACTED]"
                    match.value.startsWith("gho_") -> "gho_[REDACTED]"
                    else -> "[REDACTED]"
                }
            }
        }
        return result
    }
}
