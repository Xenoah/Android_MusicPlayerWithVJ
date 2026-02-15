package com.example.android_musicplayerwithvj

import android.net.Uri

data class MusicTrack(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val folderName: String?,
    val contentUri: Uri
) {
    // MediaStore common album art
    val albumArtUri: Uri get() = Uri.parse("content://media/external/audio/albumart/$albumId")
    
    // Priority track art (itself)
    val trackArtUri: Uri get() = contentUri
}
