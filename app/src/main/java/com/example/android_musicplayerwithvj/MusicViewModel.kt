package com.example.android_musicplayerwithvj

import android.app.Application
import android.content.ComponentName
import android.content.ContentUris
import android.content.Context
import android.media.audiofx.Visualizer
import android.provider.MediaStore
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.hypot
import kotlin.math.max

enum class MusicRepeatMode { NONE, ONE, ALL }
enum class VJStyle { LIQUID, FLOWER, NEON_WAVES, AURA_HEAT, SPEKTRO, ALCHEMY, SPIKE, BARS }
enum class BrowseCategory { ALL, FOLDERS, ALBUMS, ARTISTS }
enum class SortOrder { TITLE, ARTIST, ALBUM, DATE_ADDED }
enum class VJColorMode { SINGLE, COLORFUL }
enum class BackgroundMode { NONE, ALBUM_ART, CUSTOM }

@UnstableApi
class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("vj_prefs", Context.MODE_PRIVATE)

    private val _allTracks = MutableStateFlow<List<MusicTrack>>(emptyList())
    private val _tracks = MutableStateFlow<List<MusicTrack>>(emptyList())
    val tracks = _tracks.asStateFlow()

    private val _currentCategory = MutableStateFlow(BrowseCategory.ALL)
    val currentCategory = _currentCategory.asStateFlow()

    private val _selectedItem = MutableStateFlow<String?>(null)
    val selectedItem = _selectedItem.asStateFlow()

    private val _categoryItems = MutableStateFlow<List<String>>(emptyList())
    val categoryItems = _categoryItems.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.TITLE)
    val sortOrder = _sortOrder.asStateFlow()

    private val _currentTrack = MutableStateFlow<MusicTrack?>(null)
    val currentTrack = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle = _isShuffle.asStateFlow()

    private val _repeatMode = MutableStateFlow(MusicRepeatMode.NONE)
    val repeatMode = _repeatMode.asStateFlow()

    private val _isDarkMode = MutableStateFlow(prefs.getBoolean("is_dark_mode", true))
    val isDarkMode = _isDarkMode.asStateFlow()

    private val _vjStyle = MutableStateFlow(VJStyle.valueOf(prefs.getString("vj_style", VJStyle.LIQUID.name)!!))
    val vjStyle = _vjStyle.asStateFlow()
    
    private val _vjColorMode = MutableStateFlow(VJColorMode.valueOf(prefs.getString("vj_color_mode", VJColorMode.COLORFUL.name)!!))
    val vjColorMode = _vjColorMode.asStateFlow()

    private val _singleColor = MutableStateFlow(Color(prefs.getInt("single_color", Color.Cyan.toArgb())))
    val singleColor = _singleColor.asStateFlow()

    private val _backgroundMode = MutableStateFlow(BackgroundMode.valueOf(prefs.getString("background_mode", BackgroundMode.NONE.name)!!))
    val backgroundMode = _backgroundMode.asStateFlow()

    private val _customBackgroundUri = MutableStateFlow(prefs.getString("custom_background_uri", null))
    val customBackgroundUri = _customBackgroundUri.asStateFlow()

    private val _backgroundOpacity = MutableStateFlow(prefs.getFloat("background_opacity", 0.3f))
    val backgroundOpacity = _backgroundOpacity.asStateFlow()

    private val _zoomOnKick = MutableStateFlow(prefs.getBoolean("zoom_on_kick", true))
    val zoomOnKick = _zoomOnKick.asStateFlow()

    private val _fftData = MutableStateFlow(FloatArray(256)) 
    val fftData = _fftData.asStateFlow()

    private val _waveData = MutableStateFlow(ByteArray(512))
    val waveData = _waveData.asStateFlow()

    private val _trackPeakLow = MutableStateFlow(0.2f)
    val trackPeakLow = _trackPeakLow.asStateFlow()

    private val _trackPeakAll = MutableStateFlow(0.2f)
    val trackPeakAll = _trackPeakAll.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration = _duration.asStateFlow()

    private var visualizer: Visualizer? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private val controller: MediaController?
        get() = try {
            val future = controllerFuture
            if (future != null && future.isDone && !future.isCancelled) future.get() else null
        } catch (e: Exception) {
            null
        }

    private var positionUpdateJob: Job? = null

    init {
        val sessionToken = SessionToken(application, ComponentName(application, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(application, sessionToken).buildAsync()
        controllerFuture?.addListener({
            val controller = controller ?: return@addListener
            controller.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _isPlaying.value = isPlaying
                    if (isPlaying) {
                        viewModelScope.launch(Dispatchers.Main) { setupVisualizer() }
                        startPositionUpdate()
                    } else {
                        releaseVisualizer()
                        stopPositionUpdate()
                    }
                }
                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    _currentTrack.value = _allTracks.value.find { it.contentUri.toString() == mediaItem?.mediaId }
                    _duration.value = controller.duration.coerceAtLeast(0L)
                    _trackPeakLow.value = 0.2f
                    _trackPeakAll.value = 0.2f
                }
                override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                    _isShuffle.value = shuffleModeEnabled
                }
                override fun onRepeatModeChanged(repeatMode: Int) {
                    _repeatMode.value = when (repeatMode) {
                        Player.REPEAT_MODE_OFF -> MusicRepeatMode.NONE
                        Player.REPEAT_MODE_ALL -> MusicRepeatMode.ALL
                        Player.REPEAT_MODE_ONE -> MusicRepeatMode.ONE
                        else -> MusicRepeatMode.NONE
                    }
                }
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) {
                        _duration.value = controller.duration.coerceAtLeast(0L)
                    }
                }
            })
            _isPlaying.value = controller.isPlaying
            _isShuffle.value = controller.shuffleModeEnabled
            if (controller.isPlaying) {
                viewModelScope.launch(Dispatchers.Main) { setupVisualizer() }
                startPositionUpdate()
            }
        }, MoreExecutors.directExecutor())
    }

    private fun startPositionUpdate() {
        positionUpdateJob?.cancel()
        positionUpdateJob = viewModelScope.launch {
            while (true) {
                controller?.let {
                    _currentPosition.value = it.currentPosition
                }
                delay(1000)
            }
        }
    }

    private fun stopPositionUpdate() {
        positionUpdateJob?.cancel()
        positionUpdateJob = null
    }

    @Synchronized
    private fun setupVisualizer() {
        try {
            releaseVisualizer()
            val sessionId = controller?.sessionExtras?.getInt("AUDIO_SESSION_ID") ?: 0
            if (sessionId != 0) {
                visualizer = Visualizer(sessionId).apply {
                    captureSize = 1024 
                    setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                        override fun onWaveFormDataCapture(v: Visualizer?, waveform: ByteArray?, samplingRate: Int) {
                            waveform?.let { _waveData.value = it.copyOf() }
                        }
                        override fun onFftDataCapture(v: Visualizer?, fft: ByteArray?, samplingRate: Int) {
                            fft?.let { data ->
                                val magnitudes = FloatArray(data.size / 2)
                                var currentMaxLow = 0f
                                var currentMaxAll = 0f
                                for (i in 0 until magnitudes.size) {
                                    val r = data[i * 2].toFloat()
                                    val im = data[i * 2 + 1].toFloat()
                                    val mag = hypot(r, im) / 512f
                                    magnitudes[i] = mag
                                    
                                    if (i in 1..2) {
                                        if (mag > currentMaxLow) currentMaxLow = mag
                                    }
                                    if (mag > currentMaxAll) currentMaxAll = mag
                                }
                                _fftData.value = magnitudes
                                
                                if (currentMaxLow > _trackPeakLow.value) _trackPeakLow.value = currentMaxLow
                                if (currentMaxAll > _trackPeakAll.value) _trackPeakAll.value = currentMaxAll
                            }
                        }
                    }, Visualizer.getMaxCaptureRate() / 2, true, true)
                    enabled = true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Synchronized
    private fun releaseVisualizer() {
        visualizer?.enabled = false
        visualizer?.release()
        visualizer = null
        _fftData.value = FloatArray(256)
        _waveData.value = ByteArray(512)
    }

    fun loadMusic() {
        viewModelScope.launch {
            val musicList = withContext(Dispatchers.IO) {
                val list = mutableListOf<MusicTrack>()
                val projection = arrayOf(
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.ARTIST,
                    MediaStore.Audio.Media.ALBUM,
                    MediaStore.Audio.Media.ALBUM_ID,
                    MediaStore.Audio.Media.DATA,
                    MediaStore.Audio.Media.DATE_ADDED
                )
                
                getApplication<Application>().contentResolver.query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    "${MediaStore.Audio.Media.IS_MUSIC} != 0",
                    null,
                    "${MediaStore.Audio.Media.TITLE} ASC"
                )?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                    val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                    val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                    val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                    val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                    val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                    val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)

                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idCol)
                        val albumId = cursor.getLong(albumIdCol)
                        val data = cursor.getString(dataCol)
                        val folder = File(data).parentFile?.name ?: "Unknown"
                        list.add(MusicTrack(
                            id,
                            cursor.getString(titleCol),
                            cursor.getString(artistCol),
                            cursor.getString(albumCol),
                            albumId,
                            folder,
                            ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id),
                            cursor.getLong(dateAddedCol)
                        ))
                    }
                }
                list
            }
            _allTracks.value = musicList
            applyFilterAndSort()
        }
    }

    private fun applyFilterAndSort() {
        val all = _allTracks.value
        
        if (_currentCategory.value != BrowseCategory.ALL && _selectedItem.value == null) {
            val items = when (_currentCategory.value) {
                BrowseCategory.FOLDERS -> all.mapNotNull { it.folderName }.distinct().sorted()
                BrowseCategory.ALBUMS -> all.map { it.album }.distinct().sorted()
                BrowseCategory.ARTISTS -> all.map { it.artist }.distinct().sorted()
                else -> emptyList()
            }
            _categoryItems.value = items
            _tracks.value = emptyList()
            return
        }

        _categoryItems.value = emptyList()
        var list = when (_currentCategory.value) {
            BrowseCategory.ALL -> all
            BrowseCategory.FOLDERS -> all.filter { it.folderName == _selectedItem.value }
            BrowseCategory.ALBUMS -> all.filter { it.album == _selectedItem.value }
            BrowseCategory.ARTISTS -> all.filter { it.artist == _selectedItem.value }
        }
        
        list = when (_sortOrder.value) {
            SortOrder.TITLE -> list.sortedBy { it.title.lowercase() }
            SortOrder.ARTIST -> list.sortedBy { it.artist.lowercase() }
            SortOrder.ALBUM -> list.sortedBy { it.album.lowercase() }
            SortOrder.DATE_ADDED -> list.sortedByDescending { it.dateAdded }
        }
        
        _tracks.value = list
    }

    fun setCategory(category: BrowseCategory) {
        _currentCategory.value = category
        _selectedItem.value = null
        applyFilterAndSort()
    }

    fun selectItem(item: String?) {
        _selectedItem.value = item
        applyFilterAndSort()
    }

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
        applyFilterAndSort()
    }

    fun playTrack(track: MusicTrack) {
        val controller = controller ?: return
        val mediaItems = _tracks.value.map {
            MediaItem.Builder().setUri(it.contentUri).setMediaId(it.contentUri.toString()).build()
        }
        val startIndex = _tracks.value.indexOf(track)
        controller.setMediaItems(mediaItems, startIndex, 0)
        controller.prepare()
        controller.play()
    }

    fun togglePlayPause() {
        val controller = controller ?: return
        if (controller.isPlaying) controller.pause() else controller.play()
    }
    
    fun toggleShuffle() {
        val controller = controller ?: return
        controller.shuffleModeEnabled = !controller.shuffleModeEnabled
    }

    fun toggleRepeat() {
        val controller = controller ?: return
        controller.repeatMode = when (controller.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_OFF
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun seekTo(position: Long) {
        controller?.seekTo(position)
    }

    fun toggleTheme() { 
        _isDarkMode.value = !_isDarkMode.value
        prefs.edit().putBoolean("is_dark_mode", _isDarkMode.value).apply()
    }
    
    fun setVJStyle(style: VJStyle) { 
        _vjStyle.value = style
        prefs.edit().putString("vj_style", style.name).apply()
    }
    
    fun setVJColorMode(mode: VJColorMode) { 
        _vjColorMode.value = mode
        prefs.edit().putString("vj_color_mode", mode.name).apply()
    }
    
    fun setSingleColor(color: Color) {
        _singleColor.value = color
        prefs.edit().putInt("single_color", color.toArgb()).apply()
    }

    fun setBackgroundMode(mode: BackgroundMode) {
        _backgroundMode.value = mode
        prefs.edit().putString("background_mode", mode.name).apply()
    }

    fun setCustomBackgroundUri(uri: String?) {
        _customBackgroundUri.value = uri
        prefs.edit().putString("custom_background_uri", uri).apply()
    }

    fun setBackgroundOpacity(opacity: Float) {
        _backgroundOpacity.value = opacity
        prefs.edit().putFloat("background_opacity", opacity).apply()
    }

    fun setZoomOnKick(enabled: Boolean) {
        _zoomOnKick.value = enabled
        prefs.edit().putBoolean("zoom_on_kick", enabled).apply()
    }

    override fun onCleared() {
        super.onCleared()
        releaseVisualizer()
        stopPositionUpdate()
        controllerFuture?.let {
            MediaController.releaseFuture(it)
        }
    }
}
