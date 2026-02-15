package com.example.android_musicplayerwithvj

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.android_musicplayerwithvj.ui.theme.Android_MusicPlayerWithVJTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Android_MusicPlayerWithVJTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MusicPlayerApp()
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MusicPlayerApp(viewModel: MusicViewModel = viewModel()) {
    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val permissionState = rememberPermissionState(permission)

    LaunchedEffect(permissionState.status.isGranted) {
        if (permissionState.status.isGranted) {
            viewModel.loadMusic()
        }
    }

    if (permissionState.status.isGranted) {
        MusicPlayerScreen(viewModel)
    } else {
        PermissionRequestScreen { permissionState.launchPermissionRequest() }
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

@Composable
fun MusicPlayerScreen(viewModel: MusicViewModel) {
    val tracks by viewModel.tracks.collectAsState()
    val currentTrack by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // VJ & Now Playing Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            VJCanvas(isPlaying = isPlaying)
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.now_playing),
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
                Text(
                    text = currentTrack?.title ?: stringResource(R.string.unknown_title),
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = currentTrack?.artist ?: stringResource(R.string.unknown_artist),
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    maxLines = 1
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = { viewModel.togglePlayPause() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f)),
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier.size(80.dp)
                ) {
                    Text(
                        text = if (isPlaying) "||" else "▶",
                        color = Color.White,
                        fontSize = 32.sp
                    )
                }
            }
        }

        // Track List
        if (tracks.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1.5f), contentAlignment = Alignment.Center) {
                Text(text = stringResource(R.string.no_music_found))
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.5f)
            ) {
                items(tracks) { track ->
                    TrackItem(track = track, isSelected = track == currentTrack) {
                        viewModel.playTrack(track)
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                }
            }
        }
    }
}

@Composable
fun TrackItem(track: MusicTrack, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp)
            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else Color.Transparent),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = track.title, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(text = track.artist, style = MaterialTheme.typography.bodySmall, maxLines = 1)
        }
    }
}