package com.example.traveldiary.model

data class TravelRecord(
    val no: Int = 0,
    val place: String = "",
    val visitDate: String = "",
    val memo: String = "",
    val photoUri: String = "",
    val isPinned: Int = 0  // 0 = 일반, 1 = 고정
)