package com.example.polisapp.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.polisapp.dto.* 
import com.example.polisapp.network.RetrofitClient
import kotlinx.coroutines.launch

sealed class CourseSaveResult {
    data class Success(val course: CourseDto) : CourseSaveResult()
    data class Error(val messages: List<String>) : CourseSaveResult()
    object Loading : CourseSaveResult()
    object Idle : CourseSaveResult()
}


class AddEditCourseViewModel : ViewModel() {

    private val _saveResult = MutableLiveData<CourseSaveResult>(CourseSaveResult.Idle)
    val saveResult: LiveData<CourseSaveResult> = _saveResult

    private val _allTeachers = MutableLiveData<List<TeacherDto>>()
    val allTeachers: LiveData<List<TeacherDto>> = _allTeachers

    private val _teachersLoadingError = MutableLiveData<String?>()
    val teachersLoadingError: LiveData<String?> = _teachersLoadingError

    private var originalCourseForEdit: CourseDto? = null

    companion object {
        private const val TAG = "AddEditCourseVM"
    }

    fun setExistingCourse(course: CourseDto?) {
        originalCourseForEdit = course
        if (course != null) {
            Log.d(TAG, "ViewModel: setExistingCourse called with ID: ${course.id}, Title: ${course.title}")
        } else {
            Log.d(TAG, "ViewModel: setExistingCourse called with null (new course)")
        }
    }

    fun fetchTeachersForSpinner() {
        viewModelScope.launch {
            _teachersLoadingError.value = null
            try {
                val pagination = Pagination(pageNumber = 0, pageSize = 200, sort = null) 
                val filterDto = SimpleStringFilterDto(filter = null, pagination = pagination)
                val response = RetrofitClient.apiService.filterTeachers(filterDto)

                if (response.isSuccessful && response.body()?.slice?.content != null) {
                    _allTeachers.value = response.body()!!.slice!!.content!!
                } else {
                    Log.e(TAG, "Failed to fetch teachers for spinner. Code: ${response.code()}")
                    _teachersLoadingError.value = "Error fetching teachers list."
                    _allTeachers.value = emptyList() 
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception fetching teachers for spinner: ${e.message}", e)
                _teachersLoadingError.value = "Network error fetching teachers."
                _allTeachers.value = emptyList() 
            }
        }
    }

    fun saveCourse(code: String, title: String, description: String, yearString: String, selectedTeacher: TeacherDto?) {
        if (code.isBlank() || title.isBlank() || yearString.isBlank()) {
            _saveResult.value = CourseSaveResult.Error(listOf("Course Code, Title, and Year are required."))
            return
        }
        val year = yearString.toIntOrNull()
        if (year == null) {
            _saveResult.value = CourseSaveResult.Error(listOf("Invalid year format."))
            return
        }

        _saveResult.value = CourseSaveResult.Loading

        
        val teacherForSaving: TeacherDto? = selectedTeacher?.let {
            TeacherDto(id = it.id, firstName = it.firstName, lastName = it.lastName, title = it.title, courses = null)
        }

        val studentsForSaving: List<StudentDto>? = originalCourseForEdit?.students?.map { studentDto ->
            studentDto.copy(course = null) 
        }

        val courseToSave = CourseDto(
            id = originalCourseForEdit?.id,
            code = code.trim(),
            title = title.trim(),
            description = description.trim(),
            year = year,
            teacher = teacherForSaving,
            students = studentsForSaving 
        )

        Log.d(TAG, "ViewModel attempting to save course: $courseToSave")

        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.upsertCourse(courseToSave)
                if (response.isSuccessful && response.body() != null) {
                    val responseData = response.body()!!
                    if (responseData.data != null && (responseData.status.isNullOrEmpty() || responseData.status.all { it.code == com.example.polisapp.dto.ServerErrorEnum.OK })) {
                        Log.i(TAG, "Course saved/updated successfully: ${responseData.data}")
                        _saveResult.value = CourseSaveResult.Success(responseData.data)
                    } else {
                        val errorMessages = responseData.status
                            ?.filterNot { it.code == com.example.polisapp.dto.ServerErrorEnum.OK }
                            ?.mapNotNull { it.message }
                            ?: listOf("Unknown server error after successful call.")
                        Log.w(TAG, "Server returned non-OK status or no data: $errorMessages")
                        _saveResult.value = CourseSaveResult.Error(if (errorMessages.isEmpty()) listOf("Failed to save course.") else errorMessages)
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown API error"
                    Log.e(TAG, "API Error saving course. Code: ${response.code()}, Body: $errorBody")
                    _saveResult.value = CourseSaveResult.Error(listOf("Error saving course (Code: ${response.code()}). Please try again."))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Network exception while saving course: ${e.message}", e)
                _saveResult.value = CourseSaveResult.Error(listOf("Network error: ${e.message}"))
            }
        }
    }

    fun onSaveResultConsumed() {
        _saveResult.value = CourseSaveResult.Idle
    }

    fun onTeacherLoadingErrorShown() {
        _teachersLoadingError.value = null
    }
}