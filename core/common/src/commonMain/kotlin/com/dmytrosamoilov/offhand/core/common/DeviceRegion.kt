package com.dmytrosamoilov.offhand.core.common

fun interface DeviceRegion {
    fun countryCode(): String
}

// Where the law wants a yes before any usage data is collected: the EU, the
// rest of the EEA, the United Kingdom and Switzerland. A phone that reports no
// region is asked as well, because a wrong guess there costs the user, not us.
val DeviceRegion.asksBeforeCollecting: Boolean
    get() = countryCode().uppercase().let { it.isBlank() || it in CONSENT_FIRST_COUNTRIES }

private val CONSENT_FIRST_COUNTRIES = setOf(
    "AT", "BE", "BG", "HR", "CY", "CZ", "DK", "EE", "FI", "FR", "DE", "GR", "HU", "IE",
    "IT", "LV", "LT", "LU", "MT", "NL", "PL", "PT", "RO", "SK", "SI", "ES", "SE",
    "IS", "LI", "NO", "GB", "CH",
)
