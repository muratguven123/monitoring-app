package com.monitoring.dashboard.crash

import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber

/**
 * Thin facade for crash / error reporting.
 *
 * Production installs [CrashlyticsSink]. Do not install a Timber-backed sink in
 * release — that can recurse with [com.monitoring.dashboard.MonitoringApp]'s ProductionTree.
 */
object CrashReporting {

    interface Sink {
        fun log(message: String)
        fun record(throwable: Throwable)
    }

    @Volatile
    private var sink: Sink = NoOpSink

    fun install(sink: Sink) {
        this.sink = sink
    }

    fun log(message: String) {
        sink.log(message)
    }

    fun record(throwable: Throwable) {
        sink.record(throwable)
    }

    private object NoOpSink : Sink {
        override fun log(message: String) = Unit
        override fun record(throwable: Throwable) = Unit
    }

    /** Firebase Crashlytics sink — safe to call from Timber trees (does not log via Timber). */
    class CrashlyticsSink : Sink {
        override fun log(message: String) {
            FirebaseCrashlytics.getInstance().log(LogSanitizer.sanitize(message))
        }

        override fun record(throwable: Throwable) {
            FirebaseCrashlytics.getInstance().recordException(throwable)
        }
    }

    /** Debug-only helper; never install in release. */
    class TimberBreadcrumbSink : Sink {
        override fun log(message: String) {
            Timber.tag("CrashReporting").d(message)
        }

        override fun record(throwable: Throwable) {
            Timber.tag("CrashReporting").e(throwable, "Recorded exception")
        }
    }
}
