package com.monitoring.dashboard.crash

import timber.log.Timber

/**
 * Thin facade for crash / error reporting.
 *
 * Wire a real SDK (Firebase Crashlytics, Sentry, etc.) inside [install] without
 * changing call sites across the app.
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

    class TimberBreadcrumbSink : Sink {
        override fun log(message: String) {
            Timber.tag("CrashReporting").w(message)
        }

        override fun record(throwable: Throwable) {
            Timber.tag("CrashReporting").e(throwable, "Recorded exception")
        }
    }
}
