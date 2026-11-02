package io.github.andrearandazzo.papyrus.ui.scan

import android.app.Application
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.andrearandazzo.papyrus.PapyrusApp
import io.github.andrearandazzo.papyrus.data.Category
import io.github.andrearandazzo.papyrus.data.CategoryRepository
import io.github.andrearandazzo.papyrus.data.Corners
import io.github.andrearandazzo.papyrus.data.DocumentRepository
import io.github.andrearandazzo.papyrus.data.PdfMaker
import io.github.andrearandazzo.papyrus.data.ScanPage
import io.github.andrearandazzo.papyrus.data.ScanPages
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException

/** One-off results of saving, each handled once by the screen. */
enum class ScanEvent { SAVED, FAILED }

data class ScanState(
    val pages: List<ScanPage> = emptyList(),
    val title: String = "",
    val categoryId: Long? = null,
    val categories: List<Category> = emptyList(),
    val saving: Boolean = false,
    /** Page whose crop editor is open, or null. */
    val cropIndex: Int? = null,
)

/**
 * A scan in progress. Everything lives in [SavedStateHandle] and a session folder in the cache, so
 * the pages survive Android closing Papyrus while the camera app is open.
 */
class ScanViewModel(
    app: Application,
    private val saved: SavedStateHandle,
    private val documents: DocumentRepository,
    categories: CategoryRepository,
) : ViewModel() {

    private val sessionDir = File(
        saved.get<String>(KEY_SESSION) ?: run {
            // A new scan: photos left by scans that never finished (app closed mid-scan) can go.
            File(app.cacheDir, "scan").listFiles()?.forEach { it.deleteRecursively() }
            File(app.cacheDir, "scan/${UUID.randomUUID()}").path.also { saved[KEY_SESSION] = it }
        },
    ).apply { mkdirs() }

    private val pagePaths = saved.getStateFlow(KEY_PATHS, emptyList<String>())
    private val pageRotations = saved.getStateFlow(KEY_ROTATIONS, emptyList<Int>())
    private val pageCorners = saved.getStateFlow(KEY_CORNERS, emptyList<String>())
    private val cropIndex = saved.getStateFlow<Int?>(KEY_CROP_INDEX, null)
    private val title = saved.getStateFlow(KEY_TITLE, "")
    private val categoryId = saved.getStateFlow<Long?>(
        KEY_CATEGORY,
        // Opened from inside a folder: start in its category (navigation argument, -1 means none).
        saved.get<Long>(KEY_FOLDER_CATEGORY)?.takeIf { it > 0 },
    )
    private val saving = saved.getStateFlow(KEY_SAVING, false)

    private val _events = Channel<ScanEvent>(Channel.BUFFERED)
    val events: Flow<ScanEvent> = _events.receiveAsFlow()

    init {
        // A save interrupted by Android closing the app never finished: allow trying again.
        saved[KEY_SAVING] = false
    }

    val state: StateFlow<ScanState> = combine(
        combine(pagePaths, pageRotations, pageCorners) { paths, rotations, corners ->
            paths.mapIndexed { i, path ->
                ScanPage(path, rotations.getOrElse(i) { 0 }, corners.getOrNull(i)?.let(Corners::decode))
            }
        },
        title,
        categoryId,
        categories.categories,
        combine(saving, cropIndex, ::Pair),
    ) { pages, title, categoryId, categories, (saving, cropIndex) ->
        ScanState(pages, title, categoryId, categories, saving, cropIndex?.takeIf { it in pages.indices })
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ScanState())

    /** Whether the camera was opened automatically once; true after the first page request. */
    var autoStarted: Boolean
        get() = saved[KEY_AUTO_STARTED] ?: false
        set(value) {
            saved[KEY_AUTO_STARTED] = value
        }

    /** A new file for the camera app to write the next photo into. */
    fun newPhotoFile(): File = File(sessionDir, "page-${UUID.randomUUID()}.jpg").also {
        saved[KEY_PENDING] = it.path
    }

    /** Called when the camera app returns; adds the photo if one was taken. */
    fun onPhotoResult(taken: Boolean) {
        val pending = saved.get<String>(KEY_PENDING) ?: return
        saved[KEY_PENDING] = null
        val file = File(pending)
        if (taken && file.length() > 0) {
            val pages = currentPages()
            setPages(pages + ScanPage(pending))
            // Straight to cropping, like a scanner app; "Done" without changes keeps the whole photo.
            saved[KEY_CROP_INDEX] = pages.size
        } else {
            file.delete()
        }
    }

    fun rotate(index: Int) = setPages(ScanPages.rotate(currentPages(), index))

    fun startCrop(index: Int) {
        saved[KEY_CROP_INDEX] = index
    }

    fun finishCrop(corners: Corners?) {
        val index = cropIndex.value ?: return
        saved[KEY_CROP_INDEX] = null
        if (corners != null) setPages(ScanPages.crop(currentPages(), index, corners))
    }

    fun move(index: Int, delta: Int) = setPages(ScanPages.move(currentPages(), index, delta))

    fun remove(index: Int) {
        currentPages().getOrNull(index)?.let { File(it.path).delete() }
        setPages(ScanPages.remove(currentPages(), index))
    }

    fun setTitle(value: String) {
        saved[KEY_TITLE] = value
    }

    fun selectCategory(id: Long?) {
        saved[KEY_CATEGORY] = id
    }

    /** Builds the PDF and stores it as a document, then reports [ScanEvent.SAVED] or [ScanEvent.FAILED]. */
    fun save(defaultTitle: String) {
        val current = state.value
        if (current.pages.isEmpty() || current.saving) return
        saved[KEY_SAVING] = true
        viewModelScope.launch {
            val pdf = File(sessionDir, "scan.pdf")
            try {
                PdfMaker.write(current.pages, pdf)
                documents.addFile(pdf, current.title.trim().ifEmpty { defaultTitle }, "application/pdf", current.categoryId)
                sessionDir.deleteRecursively()
                _events.send(ScanEvent.SAVED)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Saving the scan failed", e)
                pdf.delete()
                saved[KEY_SAVING] = false
                _events.send(ScanEvent.FAILED)
            }
        }
    }

    /** The pages as saved right now (the [state] flow catches up a moment later). */
    private fun currentPages(): List<ScanPage> = pagePaths.value.mapIndexed { i, path ->
        ScanPage(path, pageRotations.value.getOrElse(i) { 0 }, pageCorners.value.getOrNull(i)?.let(Corners::decode))
    }

    private fun setPages(pages: List<ScanPage>) {
        saved[KEY_PATHS] = pages.map { it.path }
        saved[KEY_ROTATIONS] = pages.map { it.rotation }
        saved[KEY_CORNERS] = pages.map { it.corners?.encode().orEmpty() }
    }

    /** Leaving the screen (saved or not) throws away the photos; the PDF is already in storage. */
    override fun onCleared() {
        sessionDir.deleteRecursively()
    }

    companion object {
        private const val TAG = "ScanViewModel"
        private const val KEY_SESSION = "session"
        private const val KEY_PATHS = "paths"
        private const val KEY_ROTATIONS = "rotations"
        private const val KEY_CORNERS = "corners"
        private const val KEY_CROP_INDEX = "cropIndex"
        private const val KEY_TITLE = "title"
        private const val KEY_CATEGORY = "category"
        private const val KEY_FOLDER_CATEGORY = "folderCategory"
        private const val KEY_SAVING = "saving"
        private const val KEY_PENDING = "pending"
        private const val KEY_AUTO_STARTED = "autoStarted"

        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as PapyrusApp
                ScanViewModel(app, createSavedStateHandle(), app.documentRepository, app.categoryRepository)
            }
        }
    }
}
