package com.example.android_musicplayerwithvj

import android.net.Uri

data class MusicTrack(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val folderName: String?,
    val contentUri: Uri,
    val dateAdded: Long = 0L
) {
    // MediaStore album art URI (reliable for most devices)
    val artworkUri: Uri get() = Uri.parse("content://media/external/audio/albumart/$albumId")
}
