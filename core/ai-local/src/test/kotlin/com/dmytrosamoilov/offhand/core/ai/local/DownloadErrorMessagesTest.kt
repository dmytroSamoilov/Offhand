package com.dmytrosamoilov.offhand.core.ai.local

import org.junit.Assert.assertEquals
import org.junit.Test

class DownloadErrorMessagesTest {

    @Test
    fun `401 maps to access denied guidance`() {
        assertEquals(
            "Access denied (401). The model host rejected the download.",
            DownloadErrorMessages.describeHttpFailure(401, "Unauthorized"),
        )
    }

    @Test
    fun `403 maps to access denied guidance`() {
        assertEquals(
            "Access denied (403). The model host rejected the download.",
            DownloadErrorMessages.describeHttpFailure(403, "Forbidden"),
        )
    }

    @Test
    fun `404 maps to catalog guidance`() {
        assertEquals(
            "Model file not found (404). Verify the catalog entry.",
            DownloadErrorMessages.describeHttpFailure(404, "Not Found"),
        )
    }

    @Test
    fun `other codes map to generic http failure`() {
        assertEquals(
            "HTTP 503 — Service Unavailable",
            DownloadErrorMessages.describeHttpFailure(503, "Service Unavailable"),
        )
    }
}
