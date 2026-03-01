package com.example.android_musicplayerwithvj

import android.Manifest
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.android_musicplayerwithvj.ui.theme.Android_MusicPlayerWithVJTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import java.util.*

@UnstableApi
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MusicViewModel = viewModel()
            val isDarkMode by viewModel.isDarkMode.collectAsState()
            
            Android_MusicPlayerWithVJTheme(darkTheme = isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MusicPlayerApp(viewModel)
                }
            }
        }
    }
}

@UnstableApi
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MusicPlayerApp(viewModel: MusicViewModel) {
    val permissions = remember {
        val storagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        listOf(storagePermission, Manifest.permission.RECORD_AUDIO)
    }

    val permissionsState = rememberMultiplePermissionsState(permissions)

    LaunchedEffect(permissionsState.allPermissionsGranted) {
        if (permissionsState.allPermissionsGranted) {
            viewModel.loadMusic()
        }
    }

    if (permissionsState.allPermissionsGranted) {
        MusicPlayerScreen(viewModel)
    } else {
        PermissionRequestScreen { permissionsState.launchMultiplePermissionRequest() }
    }
}

@Composable
fun PermissionRequestScreen(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = stringResource(R.string.permission_required), fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = stringResource(R.string.permission_message))
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRequestPermission) {
            Text(text = stringResource(R.string.grant_permission))
        }
    }
}

@UnstableApi
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicPlayerScreen(viewModel: MusicViewModel) {
    val context = LocalContext.current
    val tracks by viewModel.tracks.collectAsState()
    val categoryItems by viewModel.categoryItems.collectAsState()
    val selectedItem by viewModel.selectedItem.collectAsState()
    val currentTrack by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isShuffle by viewModel.isShuffle.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val fftData by viewModel.fftData.collectAsState()
    val waveData by viewModel.waveData.collectAsState()
    val vjStyle by viewModel.vjStyle.collectAsState()
    val vjColorMode by viewModel.vjColorMode.collectAsState()
    val singleColor by viewModel.singleColor.collectAsState()
    val backgroundMode by viewModel.backgroundMode.collectAsState()
    val customBackgroundUri by viewModel.customBackgroundUri.collectAsState()
    val backgroundOpacity by viewModel.backgroundOpacity.collectAsState()
    val zoomOnKick by viewModel.zoomOnKick.collectAsState()
    val trackPeakLow by viewModel.trackPeakLow.collectAsState()
    val trackPeakAll by viewModel.trackPeakAll.collectAsState()
    val currentPosition by viewModel.currentPosition.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val vjUiScale by viewModel.vjUiScale.collectAsState()
    val vjEffectScale by viewModel.vjEffectScale.collectAsState()
    // Audio Effects State
    val eqEnabled by viewModel.eqEnabled.collectAsState()
    val eqBandLevels by viewModel.eqBandLevels.collectAsState()
    val eqBandCount by viewModel.eqBandCount.collectAsState()
    val eqBandFreqs by viewModel.eqBandFreqs.collectAsState()
    val eqLevelRange by viewModel.eqLevelRange.collectAsState()
    val bassBoostEnabled by viewModel.bassBoostEnabled.collectAsState()
    val bassBoostStrength by viewModel.bassBoostStrength.collectAsState()
    val virtualizerEnabled by viewModel.virtualizerEnabled.collectAsState()
    val virtualizerStrength by viewModel.virtualizerStrength.collectAsState()
    val reverbPreset by viewModel.reverbPreset.collectAsState()
    
    val currentCategory by viewModel.currentCategory.collectAsState()
    val currentSortOrder by viewModel.sortOrder.collectAsState()

    var showEqPanel by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        viewModel.setCustomBackgroundUri(uri?.toString())
    }

    if (showEqPanel) {
        AudioEffectPanel(
            eqEnabled = eqEnabled,
            eqBandLevels = eqBandLevels,
            eqBandCount = eqBandCount,
            eqBandFreqs = eqBandFreqs,
            eqLevelRange = eqLevelRange,
            bassBoostEnabled = bassBoostEnabled,
            bassBoostStrength = bassBoostStrength,
            virtualizerEnabled = virtualizerEnabled,
            virtualizerStrength = virtualizerStrength,
            reverbPreset = reverbPreset,
            activeColor = singleColor,
            onDismiss = { showEqPanel = false },
            onEqEnabled = { viewModel.setEqEnabled(it) },
            onEqBandLevel = { band, level -> viewModel.setEqBandLevel(band, level) },
            onBassBoostEnabled = { viewModel.setBassBoostEnabled(it) },
            onBassBoostStrength = { viewModel.setBassBoostStrength(it) },
            onVirtualizerEnabled = { viewModel.setVirtualizerEnabled(it) },
            onVirtualizerStrength = { viewModel.setVirtualizerStrength(it) },
            onReverbPreset = { viewModel.setReverbPreset(it) }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold) },
                actions = {
                    var showCustomizeMenu by remember { mutableStateOf(false) }
                    IconButton(onClick = { showCustomizeMenu = true }) {
                        Icon(Icons.Default.Checkroom, contentDescription = "Customize", tint = singleColor)
                    }
                    DropdownMenu(
                        expanded = showCustomizeMenu, 
                        onDismissRequest = { showCustomizeMenu = false },
                        modifier = Modifier.width(260.dp)
                    ) {
                        DropdownMenuItem(
                            text = { Text(if (isDarkMode) stringResource(R.string.light_mode) else stringResource(R.string.dark_mode)) },
                            onClick = { viewModel.toggleTheme(); showCustomizeMenu = false },
                            leadingIcon = { Icon(if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode, null) }
                        )
                        HorizontalDivider()
                        
                        Text(stringResource(R.string.visualizer_settings), modifier = Modifier.padding(16.dp, 8.dp, 16.dp, 4.dp), style = MaterialTheme.typography.labelSmall)
                        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            VJColorMode.entries.forEach { mode ->
                                val isSelected = vjColorMode == mode
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) singleColor.copy(alpha = 0.2f) else Color.Transparent)
                                        .border(1.dp, if (isSelected) singleColor else Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                        .clickable { viewModel.setVJColorMode(mode) }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(mode.name, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isSelected) singleColor else MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                        
                        if (vjColorMode == VJColorMode.SINGLE) {
                            val colors = listOf(Color.Cyan, Color.Magenta, Color.Yellow, Color.Green, Color.Red, Color.White)
                            Row(modifier = Modifier.padding(16.dp, 8.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                colors.forEach { color ->
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                            .border(if (singleColor == color) 2.dp else 0.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                            .clickable { viewModel.setSingleColor(color) }
                                    )
                                }
                            }
                        }

                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.zoom_on_kick)) },
                            onClick = { viewModel.setZoomOnKick(!zoomOnKick) },
                            trailingIcon = { Checkbox(checked = zoomOnKick, onCheckedChange = null) }
                        )

                        HorizontalDivider()

                        // Per-visualizer: Size & Effect sliders
                        Text(
                            text = "${vjStyle.name} パラメータ",
                            modifier = Modifier.padding(16.dp, 8.dp, 16.dp, 4.dp),
                            style = MaterialTheme.typography.labelSmall
                        )
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                            val currentUiScale = vjUiScale[vjStyle] ?: 1.0f
                            val currentEffectScale = vjEffectScale[vjStyle] ?: 1.0f

                            Text("サイズ: ×${ "%.1f".format(currentUiScale) }", style = MaterialTheme.typography.labelSmall)
                            Slider(
                                value = currentUiScale,
                                onValueChange = { viewModel.setVJUiScale(vjStyle, it) },
                                valueRange = 0.5f..2.0f,
                                colors = SliderDefaults.colors(activeTrackColor = singleColor, thumbColor = singleColor)
                            )

                            Text("エフェクト: ×${ "%.1f".format(currentEffectScale) }", style = MaterialTheme.typography.labelSmall)
                            Slider(
                                value = currentEffectScale,
                                onValueChange = { viewModel.setVJEffectScale(vjStyle, it) },
                                valueRange = 0.1f..10.0f,
                                colors = SliderDefaults.colors(activeTrackColor = singleColor, thumbColor = singleColor)
                            )
                        }
                        
                        HorizontalDivider()
                        
                        Text(stringResource(R.string.background_mode_label), modifier = Modifier.padding(16.dp, 8.dp, 16.dp, 4.dp), style = MaterialTheme.typography.labelSmall)
                        Column(modifier = Modifier.padding(horizontal = 8.dp).fillMaxWidth()) {
                            BackgroundMode.entries.forEach { mode ->
                                DropdownMenuItem(
                                    text = { Text(mode.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }) },
                                    onClick = { viewModel.setBackgroundMode(mode) },
                                    leadingIcon = { if (backgroundMode == mode) Icon(Icons.Default.Check, null) }
                                )
                            }
                        }
                        
                        if (backgroundMode == BackgroundMode.CUSTOM) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.select_custom_image)) },
                                onClick = { launcher.launch("image/*"); showCustomizeMenu = false },
                                leadingIcon = { Icon(Icons.Default.Image, null) }
                            )
                        }
                        
                        if (backgroundMode != BackgroundMode.NONE) {
                            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                Text(stringResource(R.string.opacity_format, (backgroundOpacity * 100).toInt()), style = MaterialTheme.typography.labelSmall)
                                Slider(
                                    value = backgroundOpacity,
                                    onValueChange = { viewModel.setBackgroundOpacity(it) },
                                    valueRange = 0f..1f,
                                    colors = SliderDefaults.colors(activeTrackColor = singleColor, thumbColor = singleColor)
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // VJ Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.2f)
                    .background(Color.Black),
                contentAlignment = Alignment.BottomCenter
            ) {
                // Background Rendering
                when (backgroundMode) {
                    BackgroundMode.ALBUM_ART -> {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(currentTrack?.artworkUri)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().alpha(backgroundOpacity),
                            contentScale = ContentScale.Crop
                        )
                    }
                    BackgroundMode.CUSTOM -> {
                        if (customBackgroundUri != null) {
                            AsyncImage(
                                model = customBackgroundUri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize().alpha(backgroundOpacity),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                    BackgroundMode.NONE -> {}
                }

                VJCanvas(
                    isPlaying = isPlaying,
                    fftData = fftData,
                    waveData = waveData,
                    style = vjStyle,
                    colorMode = vjColorMode,
                    singleColor = singleColor,
                    artworkUri = currentTrack?.artworkUri,
                    zoomOnKickEnabled = zoomOnKick,
                    trackPeakLow = trackPeakLow,
                    trackPeakAll = trackPeakAll,
                    uiScale = vjUiScale[vjStyle] ?: 1.0f,
                    effectScale = vjEffectScale[vjStyle] ?: 1.0f
                )
                
                // Overlay Controls
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(bottom = 8.dp).background(Color.Black.copy(alpha = 0.3f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(currentTrack?.artworkUri)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(4.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(text = currentTrack?.title ?: stringResource(R.string.unknown_title), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            Text(text = currentTrack?.artist ?: "", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp, maxLines = 1)
                        }
                    }
                    
                    Slider(
                        value = if (duration > 0) currentPosition.toFloat() / duration else 0f,
                        onValueChange = { viewModel.seekTo((it * duration).toLong()) },
                        modifier = Modifier.padding(horizontal = 24.dp),
                        colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = singleColor)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewModel.toggleShuffle() }) {
                            Icon(Icons.Default.Shuffle, null, tint = if (isShuffle) singleColor else Color.White)
                        }
                        IconButton(onClick = { viewModel.togglePlayPause() }) {
                            Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(48.dp))
                        }
                        IconButton(onClick = { viewModel.toggleRepeat() }) {
                            Icon(if (repeatMode == MusicRepeatMode.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat, null, tint = if (repeatMode != MusicRepeatMode.NONE) singleColor else Color.White)
                        }
                        IconButton(onClick = { showEqPanel = true }) {
                            Icon(Icons.Default.Equalizer, null, tint = if (eqEnabled || bassBoostEnabled || virtualizerEnabled) singleColor else Color.White)
                        }
                    }
                }
            }

            // Category & Sort Row
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                ScrollableTabRow(
                    modifier = Modifier.weight(1f),
                    selectedTabIndex = currentCategory.ordinal,
                    edgePadding = 16.dp,
                    containerColor = Color.Transparent,
                    divider = {}
                ) {
                    BrowseCategory.entries.forEach { category ->
                        Tab(
                            selected = currentCategory == category,
                            onClick = { viewModel.setCategory(category) },
                            text = { Text(if (category == BrowseCategory.ALL) stringResource(R.string.all_songs) else category.name.lowercase().replaceFirstChar { it.uppercase() }, fontSize = 12.sp, color = if (currentCategory == category) singleColor else MaterialTheme.colorScheme.onSurface) }
                        )
                    }
                }
                
                var showSortMenu by remember { mutableStateOf(false) }
                IconButton(onClick = { showSortMenu = true }) {
                    Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort")
                }
                DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                    SortOrder.entries.forEach { order ->
                        DropdownMenuItem(
                            text = { Text(order.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }) },
                            onClick = {
                                viewModel.setSortOrder(order)
                                showSortMenu = false
                            },
                            leadingIcon = { if (currentSortOrder == order) Icon(Icons.Default.Check, null) }
                        )
                    }
                }
            }

            // Skin / VJ Style Selector
            Text(stringResource(R.string.select_visualizer), modifier = Modifier.padding(8.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(VJStyle.entries.toTypedArray()) { style ->
                    VJSkinItem(style = style, isSelected = vjStyle == style, activeColor = singleColor) {
                        viewModel.setVJStyle(style)
                    }
                }
            }

            // List Area
            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                if (categoryItems.isNotEmpty()) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(categoryItems) { item ->
                            ListItem(
                                headlineContent = { Text(item, fontWeight = FontWeight.Medium) },
                                modifier = Modifier.clickable { viewModel.selectItem(item) }
                            )
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                        }
                    }
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        if (selectedItem != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().padding(8.dp).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            ) {
                                IconButton(onClick = { viewModel.selectItem(null) }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                                }
                                Text(selectedItem!!, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                            items(tracks) { track ->
                                TrackItem(track = track, isSelected = track == currentTrack, activeColor = singleColor) { viewModel.playTrack(track) }
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VJSkinItem(style: VJStyle, isSelected: Boolean, activeColor: Color, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(80.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(2.dp, if (isSelected) activeColor else Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.size(40.dp).background(Color.Black, RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = when(style) {
                    VJStyle.LIQUID -> Icons.Default.Opacity
                    VJStyle.FLOWER -> Icons.Default.FilterVintage
                    VJStyle.NEON_WAVES -> Icons.Default.Waves
                    VJStyle.AURA_HEAT -> Icons.Default.Whatshot
                    VJStyle.SPEKTRO -> Icons.Default.GraphicEq
                    VJStyle.ALCHEMY -> Icons.Default.AllInclusive
                    VJStyle.SPIKE -> Icons.Default.BrightnessHigh
                    VJStyle.BARS -> Icons.Default.BarChart
                },
                contentDescription = null,
                tint = if (isSelected) activeColor else Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        Text(text = style.name, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1, color = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun TrackItem(track: MusicTrack, isSelected: Boolean, activeColor: Color, onClick: () -> Unit) {
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).background(if (isSelected) activeColor.copy(alpha = 0.2f) else Color.Transparent).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(track.artworkUri)
                .crossfade(true)
                .build(),
            contentDescription = null,
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(4.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = track.title, fontWeight = FontWeight.Bold, maxLines = 1, color = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurface)
            Text(text = "${track.artist} • ${track.album}", style = MaterialTheme.typography.bodySmall, maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioEffectPanel(
    eqEnabled: Boolean,
    eqBandLevels: IntArray,
    eqBandCount: Int,
    eqBandFreqs: IntArray,
    eqLevelRange: Pair<Int, Int>,
    bassBoostEnabled: Boolean,
    bassBoostStrength: Int,
    virtualizerEnabled: Boolean,
    virtualizerStrength: Int,
    reverbPreset: Short,
    activeColor: Color,
    onDismiss: () -> Unit,
    onEqEnabled: (Boolean) -> Unit,
    onEqBandLevel: (Int, Int) -> Unit,
    onBassBoostEnabled: (Boolean) -> Unit,
    onBassBoostStrength: (Int) -> Unit,
    onVirtualizerEnabled: (Boolean) -> Unit,
    onVirtualizerStrength: (Int) -> Unit,
    onReverbPreset: (Short) -> Unit
) {
    val reverbNames = listOf("NONE", "SmallRoom", "MedRoom", "LargeRoom", "MedHall", "LargeHall", "Plate")

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text("EQ & エフェクト", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(bottom = 16.dp))

            // ---- Equalizer ----
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("イコライザー (EQ)", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Switch(checked = eqEnabled, onCheckedChange = onEqEnabled, colors = SwitchDefaults.colors(checkedThumbColor = activeColor, checkedTrackColor = activeColor.copy(alpha = 0.5f)))
            }
            if (eqEnabled) {
                val minMb = eqLevelRange.first.toFloat()
                val maxMb = eqLevelRange.second.toFloat()
                for (band in 0 until eqBandCount) {
                    val freqHz = (eqBandFreqs.getOrElse(band) { 0 } / 1000)
                    val freqLabel = if (freqHz >= 1000) "${freqHz / 1000}kHz" else "${freqHz}Hz"
                    val levelMb = eqBandLevels.getOrElse(band) { 0 }.toFloat()
                    val gainDb = "%.1f".format(levelMb / 100f)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text(freqLabel, fontSize = 11.sp, modifier = Modifier.width(46.dp))
                        Slider(
                            value = levelMb,
                            onValueChange = { onEqBandLevel(band, it.toInt()) },
                            valueRange = minMb..maxMb,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(activeTrackColor = activeColor, thumbColor = activeColor)
                        )
                        Text("${gainDb}dB", fontSize = 11.sp, modifier = Modifier.width(52.dp))
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // ---- Bass Boost ----
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Bass Boost", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Switch(checked = bassBoostEnabled, onCheckedChange = onBassBoostEnabled, colors = SwitchDefaults.colors(checkedThumbColor = activeColor, checkedTrackColor = activeColor.copy(alpha = 0.5f)))
            }
            if (bassBoostEnabled) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("強度", fontSize = 12.sp, modifier = Modifier.width(40.dp))
                    Slider(
                        value = bassBoostStrength.toFloat(),
                        onValueChange = { onBassBoostStrength(it.toInt()) },
                        valueRange = 0f..1000f,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(activeTrackColor = activeColor, thumbColor = activeColor)
                    )
                    Text(bassBoostStrength.toString(), fontSize = 12.sp, modifier = Modifier.width(40.dp))
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // ---- Virtualizer ----
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("立体音響 (Virtualizer)", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Switch(checked = virtualizerEnabled, onCheckedChange = onVirtualizerEnabled, colors = SwitchDefaults.colors(checkedThumbColor = activeColor, checkedTrackColor = activeColor.copy(alpha = 0.5f)))
            }
            if (virtualizerEnabled) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("強度", fontSize = 12.sp, modifier = Modifier.width(40.dp))
                    Slider(
                        value = virtualizerStrength.toFloat(),
                        onValueChange = { onVirtualizerStrength(it.toInt()) },
                        valueRange = 0f..1000f,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(activeTrackColor = activeColor, thumbColor = activeColor)
                    )
                    Text(virtualizerStrength.toString(), fontSize = 12.sp, modifier = Modifier.width(40.dp))
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // ---- Reverb ----
            Text("リバーブ (空間エフェクト)", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(reverbNames.size) { idx ->
                    val isSelected = reverbPreset.toInt() == idx
                    FilterChip(
                        selected = isSelected,
                        onClick = { onReverbPreset(idx.toShort()) },
                        label = { Text(reverbNames[idx], fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = activeColor.copy(alpha = 0.2f),
                            selectedLabelColor = activeColor
                        )
                    )
                }
            }
        }
    }
}
