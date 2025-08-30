package com.example.polisapp.dto

data class RespSliceDto<T>(
    val slice: KotlinSlice<T>?,
    val status: List<ServerStatus>?
)