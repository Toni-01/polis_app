package com.example.polisapp.dto

data class Sorting(
    val field: String?,
    val direction: String?,
    val ignoreCase: Boolean?,
    val nullHandling: String?
)