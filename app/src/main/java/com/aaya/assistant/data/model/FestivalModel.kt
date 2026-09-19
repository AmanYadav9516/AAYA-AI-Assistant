package com.aaya.assistant.data.model

data class FestivalModel(
    val id: String,
    val name: String,
    val hindiName: String,
    val month: Int, // 1-12
    val dayOfMonth: Int,
    val isMajor: Boolean = true,
    val description: String,
    val familyWish: String,
    val siblingWish: String,
    val friendWish: String,
    val generalWishHindi: String,
    val generalWishEnglish: String
)
