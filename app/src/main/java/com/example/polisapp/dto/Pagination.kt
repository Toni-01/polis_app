package com.example.polisapp.dto

data class Pagination(
    val pageNumber: Int,
    val pageSize: Int,
    val sort: List<Sorting>?
)