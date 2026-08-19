package com.monitoring.dashboard.domain.model

/**
 * New Relic data-center region. REST and NerdGraph share the same API host;
 * only the host name changes (see https://docs.newrelic.com/docs/accounts/accounts-billing/account-setup/choose-your-data-center/).
 *
 * Using the US host with an EU account yields "account is invalid in this region".
 */
enum class NewRelicRegion(
    val storageId: String,
    val apiHost: String,
) {
    US("US", "api.newrelic.com"),
    EU("EU", "api.eu.newrelic.com"),
    JP("JP", "api.jp.newrelic.com"),
    ;

    companion object {
        fun fromStorage(id: String?): NewRelicRegion =
            entries.firstOrNull { it.storageId.equals(id, ignoreCase = true) } ?: US

        val rewritableHosts: Set<String> = entries.map { it.apiHost }.toSet()
    }
}
