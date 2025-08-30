package com.example.polisapp.dto

data class ServerStatus(
    val code: ServerErrorEnum?,
    val severity: ErrorSeverityEnum?,
    val message: String?,
    val action: String?,
    val helpReference: String?,
    val traceId: String?
)