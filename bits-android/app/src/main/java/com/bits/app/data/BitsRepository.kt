package com.bits.app.data

import android.content.Context
import android.net.Uri
import androidx.glance.appwidget.updateAll
import com.bits.app.widget.BitsWidget
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.Executors

/**
 * The single source of truth for the app and the widget.
 *
 * All reads and writes run on one background thread, so changes are applied
 * strictly in order. The in-memory state updates instantly; the file on disk
 * and the home screen widget follow a moment later.
 */
class BitsRepository private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val file = File(appContext.filesDir, "bits-state.json")
    private val dispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    private val _state = MutableStateFlow<BitsState?>(null)
    val state: StateFlow<BitsState?> = _state.asStateFlow()

    private var writeJob: Job? = null
    private var widgetJob: Job? = null

    /** The most recent deletion, kept in memory only, so it can be undone. */
    private val _lastDeleted = MutableStateFlow<Item?>(null)
    val lastDeleted: StateFlow<Item?> = _lastDeleted.asStateFlow()

    /** Loads from disk if needed and applies the day transition. */
    suspend fun load(): BitsState = withContext(dispatcher) { ensureLoaded() }

    /** Apply a change. Disk and widget are updated shortly after. */
    fun edit(transform: (BitsState) -> BitsState) {
        scope.launch {
            val current = ensureLoaded()
            val next = transform(current)
            if (next != current) {
                _state.value = next
                scheduleWrite()
                scheduleWidgetRefresh()
            }
        }
    }

    /** Apply a change and wait until it's on disk and on the home screen. Used by widget taps. */
    suspend fun editNow(transform: (BitsState) -> BitsState) {
        withContext(dispatcher) {
            val current = ensureLoaded()
            val next = transform(current)
            if (next != current) {
                _state.value = next
                writeJob?.cancel()
                write(next)
            }
        }
        refreshWidgetsNow()
    }

    /** Deletes an item and remembers it, so the undo prompt can bring it back. */
    fun deleteItemWithUndo(itemId: String) {
        scope.launch {
            val current = ensureLoaded()
            val item = current.items.firstOrNull { it.id == itemId } ?: return@launch
            _lastDeleted.value = item
            val next = current.deleteItem(itemId)
            _state.value = next
            scheduleWrite()
            scheduleWidgetRefresh()
        }
    }

    fun undoDelete() {
        scope.launch {
            val item = _lastDeleted.value ?: return@launch
            _lastDeleted.value = null
            val current = ensureLoaded()
            val next = current.restoreItem(item)
            if (next != current) {
                _state.value = next
                scheduleWrite()
                scheduleWidgetRefresh()
            }
        }
    }

    fun clearUndo() {
        _lastDeleted.value = null
    }

    fun refresh() {
        scope.launch { ensureLoaded() }
    }

    /** Write any pending change immediately, e.g. when the app goes to the background. */
    fun flush() {
        scope.launch {
            writeJob?.cancel()
            _state.value?.let { write(it) }
        }
    }

    fun simulateMidnight() = edit { Rollover.apply(it.copy(lastRollover = "simulated"), today()) }

    /** Restores the starter content. Opacity, clock and preferences are kept. */
    fun resetToSample() = edit { current ->
        val seed = Seed.create()
        seed.copy(
            widget = seed.widget.copy(opacity = current.widget.opacity, showClock = current.widget.showClock),
            preferences = current.preferences,
        )
    }

    fun exportBackup(uri: Uri, onResult: (Boolean) -> Unit) {
        scope.launch {
            val ok = try {
                val text = StateJson.encode(ensureLoaded(), exportedAt = System.currentTimeMillis())
                val stream = try {
                    appContext.contentResolver.openOutputStream(uri, "wt")
                } catch (e: Exception) {
                    appContext.contentResolver.openOutputStream(uri, "w")
                }
                stream?.use { it.write(text.toByteArray(Charsets.UTF_8)) } != null
            } catch (e: Exception) {
                false
            }
            withContext(Dispatchers.Main) { onResult(ok) }
        }
    }

    /** Reads and checks a backup file without applying it. Returns null if it isn't a valid Bits backup. */
    fun readBackup(uri: Uri, onResult: (BitsState?) -> Unit) {
        scope.launch {
            val restored = try {
                appContext.contentResolver.openInputStream(uri)?.use { input ->
                    StateJson.decode(input.readBytes().toString(Charsets.UTF_8))
                }
            } catch (e: Exception) {
                null
            }
            withContext(Dispatchers.Main) { onResult(restored) }
        }
    }

    /**
     * Brings back lists, categories and widget settings from a backup, but keeps this
     * device's own entitlements. A hand-edited backup therefore can't grant Pro or
     * easter-egg unlocks.
     */
    fun restore(backup: BitsState) = edit { device ->
        Rollover.apply(backup.withTutorialSeen(true).withEntitlementsFrom(device), today())
    }

    suspend fun refreshWidgetsNow() {
        try {
            BitsWidget().updateAll(appContext)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // A widget failing to refresh should never break the app.
        }
    }

    // Everything below runs on the repository thread only.

    private fun ensureLoaded(): BitsState {
        val existing = _state.value
        var dirty = false
        val base: BitsState = existing ?: (read() ?: Seed.create().also { dirty = true })
        val now = System.currentTimeMillis()
        // Start the thirty-day clock on the first read, then drop in the note pointing at
        // the easter egg once it is due. Both are one-way, so repeated reads are harmless.
        val stamped = base.withInstallRecorded(now)
        if (stamped !== base) dirty = true
        val greeted = stamped.grantAnniversary(now, ANNIVERSARY_MESSAGE)
        if (greeted !== stamped) dirty = true
        val rolled = Rollover.apply(greeted, today())
        if (rolled !== greeted) dirty = true
        if (rolled !== existing) _state.value = rolled
        if (dirty) {
            writeJob?.cancel()
            write(rolled)
            scheduleWidgetRefresh()
        }
        return rolled
    }

    private fun scheduleWrite() {
        writeJob?.cancel()
        writeJob = scope.launch {
            delay(250)
            _state.value?.let { write(it) }
        }
    }

    private fun scheduleWidgetRefresh() {
        widgetJob?.cancel()
        widgetJob = scope.launch {
            delay(300)
            refreshWidgetsNow()
        }
    }

    private fun read(): BitsState? {
        if (!file.exists()) return null
        return try {
            StateJson.decode(file.readText())
        } catch (e: Exception) {
            // Keep the unreadable file instead of silently overwriting someone's data.
            file.renameTo(File(appContext.filesDir, "bits-state-unreadable-${System.currentTimeMillis()}.json"))
            null
        }
    }

    private fun write(state: BitsState) {
        try {
            val tmp = File(appContext.filesDir, "bits-state.json.tmp")
            tmp.writeText(StateJson.encode(state))
            if (!tmp.renameTo(file)) {
                file.delete()
                tmp.renameTo(file)
            }
        } catch (e: Exception) {
            // Leave the previous file in place.
        }
    }

    companion object {
        /** Shown in Today once the app has been kept for thirty days. */
        const val ANNIVERSARY_MESSAGE =
            "Thank you for keeping our app for a whole month! We have a small surprise for you \u2014 " +
                "open the Bits app and tap \u201CBits\u201D at the top left of the app's home page " +
                "four times. (In the app, not the widget!) :D"

        @Volatile
        private var instance: BitsRepository? = null

        fun get(context: Context): BitsRepository =
            instance ?: synchronized(this) {
                instance ?: BitsRepository(context).also { instance = it }
            }
    }
}
