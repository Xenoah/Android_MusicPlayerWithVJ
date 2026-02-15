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
    // Coil (AsyncImage) will extract the embedded artwork directly from this URI.
    val artworkUri: Uri get() = contentUri
    
    // MediaStore album art fallback
    val albumArtUri: Uri get() = Uri.parse("content://media/external/audio/albumart/$albumId")
}
