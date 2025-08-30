
package com.example.polisapp.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.polisapp.dto.ServerStatus
import com.example.polisapp.dto.TeacherDto
import com.example.polisapp.network.RetrofitClient
import kotlinx.coroutines.launch


sealed class SaveResult {
    data class Success(val teacher: TeacherDto) : SaveResult()
    data class Error(val messages: List<String>) : SaveResult() 
    object Loading : SaveResult()
    object Idle : SaveResult() 
}

class AddEditTeacherViewModel : ViewModel() {

    private val _saveResult = MutableLiveData<SaveResult>(SaveResult.Idle)
    val saveResult: LiveData<SaveResult> = _saveResult

    private var existingTeacherId: Long? = null
    private var existingTeacherCourses: List<com.example.polisapp.dto.CourseDto>? = null


    companion object {
        private const val TAG = "AddEditTeacherVM"
    }

    fun setExistingTeacher(teacher: TeacherDto?) {
        existingTeacherId = teacher?.id
        existingTeacherCourses = teacher?.courses 
    }

    fun saveTeacher(firstName: String, lastName: String, title: String) {
        if (firstName.isBlank() || lastName.isBlank() || title.isBlank()) {
            _saveResult.value = SaveResult.Error(listOf("All fields are required."))
            return
        }

        _saveResult.value = SaveResult.Loading

        val teacherToSave = TeacherDto(
            id = existingTeacherId,
            firstName = firstName.trim(),
            lastName = lastName.trim(),
            title = title.trim(),
            courses = existingTeacherCourses 
        )

        Log.d(TAG, "Attempting to save teacher: $teacherToSave")

        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.upsertTeacher(teacherToSave)
                if (response.isSuccessful && response.body() != null) {
                    val responseData = response.body()!!
                    if (responseData.data != null && (responseData.status.isNullOrEmpty() || responseData.status.all { it.code == com.example.polisapp.dto.ServerErrorEnum.OK })) {
                        Log.i(TAG, "Teacher saved/updated successfully: ${responseData.data}")
                        _saveResult.value = SaveResult.Success(responseData.data)
                    } else {
                        val errorMessages = responseData.status
                            ?.filterNot { it.code == com.example.polisapp.dto.ServerErrorEnum.OK }
                            ?.mapNotNull { it.message }
                            ?: listOf("Unknown server error after successful call.")
                        Log.w(TAG, "Server returned non-OK status or no data: $errorMessages")
                        _saveResult.value = SaveResult.Error(if (errorMessages.isEmpty()) listOf("Failed to save teacher.") else errorMessages)
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown API error"
                    Log.e(TAG, "API Error saving teacher. Code: ${response.code()}, Body: $errorBody")
                    
                    _saveResult.value = SaveResult.Error(listOf("Error saving teacher (Code: ${response.code()}). Please try again."))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Network exception while saving teacher: ${e.message}", e)
                _saveResult.value = SaveResult.Error(listOf("Network error: ${e.message}"))
            }
        }
    }

    /**
     * Call this to reset the save result state, e.g., after the user has navigated away or
     * the message has been shown.
     */
    fun onSaveResultConsumed() {
        _saveResult.value = SaveResult.Idle
    }
}