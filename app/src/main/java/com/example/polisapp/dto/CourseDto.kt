package com.example.polisapp.dto

import java.io.Serializable

data class CourseDto(
    val id: Long?,
    val code: String?,
    val title: String?,
    val description: String?,
    val year: Int?,
    val teacher: TeacherDto?,
    val students: List<StudentDto>?
) : Serializable