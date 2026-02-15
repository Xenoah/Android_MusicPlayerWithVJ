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
    // Standard MediaStore album art URI - the most reliable way to get art in Android
    val artworkUri: Uri get() = Uri.parse("content://media/external/audio/albumart/$albumId")
}
