package com.dmytrosamoilov.offhand.core.ai.local

import java.io.File
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ModelDownloaderTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val server = MockWebServer()
    private val downloader = ModelDownloader()
    private val payload = ByteArray(64_000) { (it % 251).toByte() }

    private lateinit var destination: File

    @Before
    fun setUp() {
        server.start()
        destination = File(folder.root, "model.litertlm")
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun partFile(): File = File(folder.root, "model.litertlm.part")

    private fun bodyOf(bytes: ByteArray): Buffer = Buffer().apply { write(bytes) }

    private fun download(): List<DownloadProgress> = runBlocking {
        downloader.download(server.url("/model.litertlm").toString(), destination).toList()
    }

    @Test
    fun `fresh download completes and finalizes the file`() {
        server.enqueue(MockResponse().setBody(bodyOf(payload)))

        val events = download()

        assertEquals(DownloadProgress.Completed(destination), events.last())
        assertTrue(payload.contentEquals(destination.readBytes()))
        assertFalse(partFile().exists())
    }

    @Test
    fun `existing part file resumes with a range request`() {
        val alreadyDownloaded = payload.copyOfRange(0, 24_000)
        val remainder = payload.copyOfRange(24_000, payload.size)
        partFile().writeBytes(alreadyDownloaded)
        server.enqueue(
            MockResponse()
                .setResponseCode(206)
                .setHeader("Content-Range", "bytes 24000-63999/64000")
                .setBody(bodyOf(remainder)),
        )

        val events = download()

        assertEquals("bytes=24000-", server.takeRequest().getHeader("Range"))
        assertEquals(DownloadProgress.Completed(destination), events.last())
        assertTrue(payload.contentEquals(destination.readBytes()))
        val progress = events.filterIsInstance<DownloadProgress.InProgress>()
        assertTrue(progress.all { it.bytesTotal == payload.size.toLong() })
        assertTrue(progress.first().bytesDownloaded > 24_000)
    }

    @Test
    fun `interrupted download retries and resumes without losing bytes`() {
        server.enqueue(
            MockResponse()
                .setBody(bodyOf(payload))
                .setSocketPolicy(SocketPolicy.DISCONNECT_DURING_RESPONSE_BODY),
        )
        server.enqueue(
            MockResponse()
                .setResponseCode(206)
                .setBody(bodyOf(payload.copyOfRange(32_000, payload.size))),
        )

        val events = download()

        assertEquals(DownloadProgress.Completed(destination), events.last())
        server.takeRequest()
        assertEquals("bytes=32000-", server.takeRequest().getHeader("Range"))
        assertTrue(payload.contentEquals(destination.readBytes()))
    }

    @Test
    fun `http error fails without retrying and clears the part file`() {
        server.enqueue(MockResponse().setResponseCode(404))

        val events = download()

        assertEquals(
            DownloadProgress.Failed("Model file not found (404). Verify the catalog entry."),
            events.single(),
        )
        assertEquals(1, server.requestCount)
        assertFalse(partFile().exists())
        assertFalse(destination.exists())
    }

    @Test
    fun `stale range restarts from scratch`() {
        partFile().writeBytes(ByteArray(payload.size + 1))
        server.enqueue(MockResponse().setResponseCode(416))
        server.enqueue(MockResponse().setBody(bodyOf(payload)))

        val events = download()

        assertEquals(DownloadProgress.Completed(destination), events.last())
        assertTrue(payload.contentEquals(destination.readBytes()))
    }
}
