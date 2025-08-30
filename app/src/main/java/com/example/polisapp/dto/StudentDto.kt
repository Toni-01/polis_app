package com.example.polisapp.dto

import java.io.Serializable

data class StudentDto(
    val id: Long?,
    val firstName: String?,
    val lastName: String?,
    val serialNumber: String?,
    val course: CourseDto?
) :Serializable