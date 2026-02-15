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
    // Priority: The audio file itself (embedded art)
    val artworkUri: Uri get() = contentUri
    
    // Fallback: The album art from MediaStore
    val albumArtUri: Uri get() = Uri.parse("content://media/external/audio/albumart/$albumId")
}
