package com.example.polisapp.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.polisapp.dto.* 
import com.example.polisapp.network.RetrofitClient
import kotlinx.coroutines.launch

sealed class StudentSaveResult {
    data class Success(val student: StudentDto) : StudentSaveResult()
    data class Error(val messages: List<String>) : StudentSaveResult()
    object Loading : StudentSaveResult()
    object Idle : StudentSaveResult()
}

class AddEditStudentViewModel : ViewModel() {

    private val _saveResult = MutableLiveData<StudentSaveResult>(StudentSaveResult.Idle)
    val saveResult: LiveData<StudentSaveResult> = _saveResult

    private val _allCourses = MutableLiveData<List<CourseDto>>()
    val allCourses: LiveData<List<CourseDto>> = _allCourses

    private val _coursesLoadingError = MutableLiveData<String?>()
    val coursesLoadingError: LiveData<String?> = _coursesLoadingError

    private var originalStudentForEdit: StudentDto? = null

    companion object {
        private const val TAG = "AddEditStudentVM"
    }

    fun setExistingStudent(student: StudentDto?) {
        originalStudentForEdit = student
        if (student != null) {
            Log.d(TAG, "ViewModel: setExistingStudent called with ID: ${student.id}, Name: ${student.firstName}")
        } else {
            Log.d(TAG, "ViewModel: setExistingStudent called with null (new student)")
        }
    }

    fun fetchCoursesForSpinner() {
        viewModelScope.launch {
            _coursesLoadingError.value = null
            try {
                val pagination = Pagination(pageNumber = 0, pageSize = 200, sort = null)
                val filterDto = SimpleStringFilterDto(filter = null, pagination = pagination)
                val response = RetrofitClient.apiService.filterCourses(filterDto)

                if (response.isSuccessful && response.body()?.slice?.content != null) {
                    _allCourses.value = response.body()!!.slice!!.content!!
                } else {
                    Log.e(TAG, "Failed to fetch courses for spinner. Code: ${response.code()}")
                    _coursesLoadingError.value = "Error fetching courses list."
                    _allCourses.value = emptyList()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception fetching courses: ${e.message}", e)
                _coursesLoadingError.value = "Network error fetching courses."
                _allCourses.value = emptyList()
            }
        }
    }

    fun saveStudent(firstName: String, lastName: String, serialNumber: String, selectedCourse: CourseDto?) {
        if (firstName.isBlank() || lastName.isBlank() || serialNumber.isBlank()) {
            _saveResult.value = StudentSaveResult.Error(listOf("First Name, Last Name, and Serial Number are required."))
            return
        }

        _saveResult.value = StudentSaveResult.Loading

        
        val courseForSaving: CourseDto? = selectedCourse?.let {
            CourseDto(id = it.id, code = it.code, title = it.title, description = null, year = null, teacher = null, students = null)
        }

        val studentToSave = StudentDto(
            id = originalStudentForEdit?.id,
            firstName = firstName.trim(),
            lastName = lastName.trim(),
            serialNumber = serialNumber.trim(),
            course = courseForSaving
        )

        Log.d(TAG, "Attempting to save student: $studentToSave")

        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.upsertStudent(studentToSave)
                if (response.isSuccessful && response.body() != null) {
                    val responseData = response.body()!!
                    if (responseData.data != null && (responseData.status.isNullOrEmpty() || responseData.status.all { it.code == com.example.polisapp.dto.ServerErrorEnum.OK })) {
                        Log.i(TAG, "Student saved/updated successfully: ${responseData.data}")
                        _saveResult.value = StudentSaveResult.Success(responseData.data)
                    } else {
                        val errorMessages = responseData.status
                            ?.filterNot { it.code == com.example.polisapp.dto.ServerErrorEnum.OK }
                            ?.mapNotNull { it.message }
                            ?: listOf("Unknown server error after successful call.")
                        Log.w(TAG, "Server returned non-OK status or no data: $errorMessages")
                        _saveResult.value = StudentSaveResult.Error(if (errorMessages.isEmpty()) listOf("Failed to save student.") else errorMessages)
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown API error"
                    Log.e(TAG, "API Error saving student. Code: ${response.code()}, Body: $errorBody")
                    _saveResult.value = StudentSaveResult.Error(listOf("Error saving student (Code: ${response.code()}). Please try again."))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Network exception while saving student: ${e.message}", e)
                _saveResult.value = StudentSaveResult.Error(listOf("Network error: ${e.message}"))
            }
        }
    }

    fun onSaveResultConsumed() {
        _saveResult.value = StudentSaveResult.Idle
    }

    fun onCourseLoadingErrorShown() {
        _coursesLoadingError.value = null
    }
}