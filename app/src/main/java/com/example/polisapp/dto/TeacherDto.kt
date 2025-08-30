package com.example.polisapp.dto

import java.io.Serializable

data class TeacherDto(
    val id: Long?,
    val firstName: String?,
    val lastName: String?,
    val title: String?,
    val courses: List<CourseDto>?
) : Serializable