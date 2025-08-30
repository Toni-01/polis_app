package com.example.polisapp.dto

data class RespSingleDto<T>(
    val data: T?,
    val status: List<ServerStatus>?
)