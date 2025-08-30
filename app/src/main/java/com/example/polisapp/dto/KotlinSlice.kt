package com.example.polisapp.dto

data class KotlinSlice<T>(
    val content: List<T>?,
    val hasNext: Boolean?,
    val number: Int?,
    val size: Int?,
    val numberOfElements: Int?,
    val last: Boolean?
)