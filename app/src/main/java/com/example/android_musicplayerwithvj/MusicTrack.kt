package com.example.android_musicplayerwithvj

import android.net.Uri

data class MusicTrack(
    val id: Long,
    val title: String,
    val artist: String,
    val contentUri: Uri
)