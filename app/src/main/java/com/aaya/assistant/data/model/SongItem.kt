package com.aaya.assistant.data.model

data class SongItem(
    val id: String = "",
    val title: String = "",
    val artist: String = "",
    val category: String = "Chill",
    val audioUrl: String = "",
    val coverUrl: String = "",
    val duration: String = "03:30",
    val isLocal: Boolean = false
)
