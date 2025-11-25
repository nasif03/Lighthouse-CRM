package com.project.lighthouse.config

import com.project.lighthouse.BuildConfig

object ApiConfig {
    private const val DEFAULT_BASE_URL = "http://10.0.2.2:3000/"

    /**
     * Base URL configured via BuildConfig (API_BASE_URL).
     * Falls back to a sane default for local emulator runs.
     */
    val BASE_URL: String = sanitizeUrl(
        BuildConfig.API_BASE_URL.ifBlank { DEFAULT_BASE_URL }
    )

    // API endpoints
    const val API_PREFIX = "/api"

    private fun sanitizeUrl(url: String): String =
        if (url.endsWith("/")) url else "$url/"
}

