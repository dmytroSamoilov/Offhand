package com.dmytrosamoilov.offhand

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import com.dmytrosamoilov.offhand.core.data.domain.NotesRepository
import com.dmytrosamoilov.offhand.core.data.domain.UserPreferencesRepository
import com.dmytrosamoilov.offhand.core.security.AppLockManager
import com.dmytrosamoilov.offhand.core.security.AppLockState
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.regex.Pattern
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import org.koin.core.context.loadKoinModules
import org.koin.core.context.unloadKoinModules
import org.koin.dsl.module

// The system picker is another activity, so the app locks behind it. The
// picked file must still turn into a note once the user unlocks, instead of
// being lost with the Settings screen the lock screen replaced.
@RunWith(AndroidJUnit4::class)
class AudioImportWhileLockedTest {

    @get:Rule
    val composeRule = createEmptyComposeRule()

    private val lock = FakeAppLockManager()
    private val lockModule = module { single<AppLockManager> { lock } }
    private val fileName = "lk-${System.currentTimeMillis() % 1_000_000}.wav"
    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val device: UiDevice get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private var sampleUri: Uri? = null
    private var scenario: ActivityScenario<MainActivity>? = null

    @Before
    fun setUp() {
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        loadKoinModules(lockModule)
        runBlocking {
            val preferences: UserPreferencesRepository = GlobalContext.get().get()
            preferences.setOnboardingCompleted(true)
            preferences.setAppLockEnabled(true)
        }
        sampleUri = insertSampleIntoDownloads()
    }

    @After
    fun tearDown() {
        scenario?.close()
        sampleUri?.let { context.contentResolver.delete(it, null, null) }
        runBlocking { GlobalContext.get().get<UserPreferencesRepository>().setAppLockEnabled(false) }
        unloadKoinModules(lockModule)
    }

    @Test
    fun filePickedWhileTheAppLocksIsImportedAfterUnlock() {
        val notes: NotesRepository = GlobalContext.get().get()
        val notesBefore = runBlocking { notes.countNotes() }
        scenario = ActivityScenario.launch(MainActivity::class.java)
        composeRule.waitUntil(UI_TIMEOUT_MS) { composeRule.onAllNodesWithTag(TAB_SETTINGS).fetchSemanticsNodes().isNotEmpty() }
        composeRule.onNodeWithTag(TAB_SETTINGS).performClick()
        // Real taps through UiAutomator, like a finger: the content scrolls
        // under the tab bar, so the list is flung to the end first.
        composeRule.onAllNodes(hasScrollAction()).onFirst().performTouchInput { swipeUp() }
        composeRule.onNodeWithText(IMPORT_ROW).performScrollTo()
        val row = device.wait(Until.findObject(By.text(IMPORT_ROW)), SHORT_TIMEOUT_MS)
        assertNotNull("Settings did not show the $IMPORT_ROW row; visible: ${visibleTexts()}", row)
        row!!.click()
        // Compose frames only advance while the test waits through the rule,
        // so the picker is awaited there and not by polling UiAutomator alone.
        awaitThroughCompose("The system picker did not open") { device.hasObject(By.pkg(DOCUMENTS_UI)) }

        val tile = findInPicker(fileName)
        assertNotNull("The system picker did not list $fileName; visible: ${visibleTexts()}", tile)
        lock.markLocked()
        tile!!.click()

        composeRule.waitUntil(UI_TIMEOUT_MS) { composeRule.onAllNodesWithText(LOCK_TITLE).fetchSemanticsNodes().isNotEmpty() }
        lock.markUnlocked()

        composeRule.waitUntil(UI_TIMEOUT_MS) { composeRule.onAllNodesWithText(IMPORT_STARTED).fetchSemanticsNodes().isNotEmpty() }
        runBlocking {
            withTimeout(IMPORT_TIMEOUT_MS) { notes.observeNotes().first { it.size == notesBefore + 1 } }
        }
    }

    private fun awaitThroughCompose(failure: String, condition: () -> Boolean) {
        try {
            composeRule.waitUntil(PICKER_TIMEOUT_MS, condition)
        } catch (timeout: ComposeTimeoutException) {
            throw AssertionError("$failure; visible: ${visibleTexts()}", timeout)
        }
    }

    // The picker reopens wherever it was last, so the file is looked for in the
    // first view and then under the Downloads root.
    private fun findInPicker(name: String): UiObject2? {
        device.wait(Until.findObject(By.text(name)), PICKER_TIMEOUT_MS)?.let { return it }
        device.findObject(By.desc(SHOW_ROOTS))?.click()
        device.wait(Until.findObject(By.text(DOWNLOADS_ROOT)), SHORT_TIMEOUT_MS)?.click()
        return device.wait(Until.findObject(By.text(name)), SHORT_TIMEOUT_MS)
    }

    private fun visibleTexts(): List<String> =
        device.findObjects(By.textContains("")).mapNotNull { it.text }.filter { it.isNotBlank() }.distinct()

    private fun insertSampleIntoDownloads(): Uri {
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "audio/wav")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val uri = requireNotNull(resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values))
        resolver.openOutputStream(uri)!!.use { it.write(silentWav()) }
        resolver.update(uri, ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }, null, null)
        return uri
    }

    // One second of silence, 16 kHz mono PCM16: enough for the fakes to run the
    // whole import pipeline.
    private fun silentWav(): ByteArray {
        val samples = SAMPLE_RATE
        val dataSize = samples * 2
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        header.put("RIFF".toByteArray()).putInt(36 + dataSize).put("WAVE".toByteArray())
        header.put("fmt ".toByteArray()).putInt(16).putShort(1).putShort(1).putInt(SAMPLE_RATE).putInt(SAMPLE_RATE * 2).putShort(2).putShort(16)
        header.put("data".toByteArray()).putInt(dataSize)
        return ByteArrayOutputStream(44 + dataSize).apply {
            write(header.array())
            write(ByteArray(dataSize))
        }.toByteArray()
    }

    private class FakeAppLockManager : AppLockManager {
        override val isDeviceSecure: Boolean = true
        private val state = MutableStateFlow(AppLockState.UNLOCKED)
        override val lockState: StateFlow<AppLockState> = state.asStateFlow()
        override fun markUnlocked() { state.value = AppLockState.UNLOCKED }
        override fun markLocked() { state.value = AppLockState.LOCKED }
    }

    private companion object {
        const val TAB_SETTINGS = "tab_settings"
        const val IMPORT_ROW = "Import audio"
        const val LOCK_TITLE = "Notes locked"
        const val IMPORT_STARTED = "Import started"
        const val SAMPLE_RATE = 16_000
        const val UI_TIMEOUT_MS = 15_000L
        const val PICKER_TIMEOUT_MS = 15_000L
        const val SHORT_TIMEOUT_MS = 5_000L
        const val SHOW_ROOTS = "Show roots"
        val DOCUMENTS_UI: Pattern = Pattern.compile(".*documentsui")
        const val DOWNLOADS_ROOT = "Downloads"
        const val IMPORT_TIMEOUT_MS = 60_000L
    }
}
