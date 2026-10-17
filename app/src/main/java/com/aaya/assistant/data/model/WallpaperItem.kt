package com.aaya.assistant.data.model

data class WallpaperItem(
    val id: String,
    val title: String,
    val category: String,
    val imageUrl: String,
    val previewUrl: String = imageUrl,
    val tags: String = ""
)
