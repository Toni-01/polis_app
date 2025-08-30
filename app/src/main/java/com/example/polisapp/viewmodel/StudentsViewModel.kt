package com.example.polisapp.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.polisapp.dto.Pagination
import com.example.polisapp.dto.SimpleStringFilterDto
import com.example.polisapp.dto.StudentDto
import com.example.polisapp.network.RetrofitClient
import kotlinx.coroutines.launch

class StudentsViewModel : ViewModel() {

    private val _studentsList = MutableLiveData<MutableList<StudentDto>>(mutableListOf())
    val studentsList: LiveData<MutableList<StudentDto>> = _studentsList

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _isLoadingMore = MutableLiveData<Boolean>()
    val isLoadingMore: LiveData<Boolean> = _isLoadingMore

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private var currentQuery: String? = null
    private var currentPage = 0
    private var canLoadMore = true
    private val pageSize = 20

    companion object {
        private const val TAG = "StudentsViewModel"
    }

    init {
        fetchStudents(query = null, isInitialLoad = true)
    }

    fun fetchStudents(query: String? = null, isInitialLoad: Boolean = false) {
        if (query != currentQuery || isInitialLoad) {
            currentQuery = query
            currentPage = 0
            canLoadMore = true
            _studentsList.value?.clear()
            _studentsList.postValue(_studentsList.value) 
            _isLoading.value = true
            _isLoadingMore.value = false
            Log.d(TAG, "New search/refresh for Students. Query: '$currentQuery'. Resetting page to 0.")
        } else if (!canLoadMore || _isLoading.value == true || _isLoadingMore.value == true) {
            Log.d(TAG, "Student fetch declined: canLoadMore=$canLoadMore, isLoading=${_isLoading.value}, isLoadingMore=${_isLoadingMore.value}")
            return
        }

        if (!isInitialLoad) {
            _isLoadingMore.value = true
        }
        _errorMessage.value = null

        val pagination = Pagination(pageNumber = currentPage, pageSize = pageSize, sort = null)
        val filterDto = SimpleStringFilterDto(filter = currentQuery?.trim().takeIf { !it.isNullOrBlank() }, pagination = pagination)

        Log.d(TAG, "Fetching students page: $currentPage, query: '$currentQuery', DTO: $filterDto")

        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.filterStudents(filterDto)
                if (response.isSuccessful) {
                    val responseBody = response.body()
                    val serverSlice = responseBody?.slice

                    Log.d(TAG, "Students Server Slice Details: " +
                            "content.size=${serverSlice?.content?.size}, " +
                            "hasNext=${serverSlice?.hasNext}, isLast=${serverSlice?.last}, " +
                            "number=${serverSlice?.number}, size=${serverSlice?.size}, " +
                            "numberOfElements=${serverSlice?.numberOfElements}")

                    val newStudents = serverSlice?.content
                    canLoadMore = when {
                        serverSlice?.hasNext != null -> serverSlice.hasNext!!
                        serverSlice?.last != null -> !serverSlice.last!!
                        else -> false
                    }

                    if (!newStudents.isNullOrEmpty()) {
                        val currentList = _studentsList.value ?: mutableListOf()
                        currentList.addAll(newStudents)
                        _studentsList.postValue(currentList)
                        currentPage++
                        Log.i(TAG, "Page ${serverSlice?.number ?: currentPage-1} (0-indexed) students fetched. " +
                                "Added ${newStudents.size}. Total now: ${currentList.size}. Next page to fetch: $currentPage. Can load more: $canLoadMore")
                    } else {
                        if (currentPage == 0) {
                            _studentsList.postValue(mutableListOf())
                            Log.w(TAG, "No students found for query '$currentQuery' on first page.")
                        }
                        Log.d(TAG, "No new students on this page (server page ${serverSlice?.number}). Effective canLoadMore: $canLoadMore")
                    }
                    if (responseBody?.status?.isNotEmpty() == true && responseBody.status.any{ it.code != com.example.polisapp.dto.ServerErrorEnum.OK}) {
                        _errorMessage.postValue(responseBody.status.joinToString { it.message ?: "Unknown server error" })
                    }
                } else {
                    val errorMsg = "API Error fetching students: ${response.code()} - ${response.errorBody()?.string()}"
                    Log.e(TAG, errorMsg)
                    _errorMessage.postValue("Failed to load students. Please try again.")
                    canLoadMore = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception fetching students: query '$currentQuery', page $currentPage: ${e.message}", e)
                _errorMessage.postValue("Network error. Please check your connection.")
                canLoadMore = false
            } finally {
                if (isInitialLoad || _isLoading.value == true) {
                    _isLoading.postValue(false)
                }
                if (_isLoadingMore.value == true) {
                    _isLoadingMore.postValue(false)
                }
            }
        }
    }

    fun refreshStudentsList() {
        Log.d(TAG, "refreshStudentsList called. Current query: '$currentQuery'")
        fetchStudents(query = currentQuery, isInitialLoad = true)
    }

    fun loadMoreStudents() {
        Log.d(TAG, "loadMoreStudents called. Conditions: canLoadMore=$canLoadMore, isLoading=${_isLoading.value}, isLoadingMore=${_isLoadingMore.value}")
        if (canLoadMore && _isLoading.value == false && _isLoadingMore.value == false) {
            fetchStudents(query = currentQuery, isInitialLoad = false)
        } else {
            Log.d(TAG, "Cannot load more students. Effective canLoadMore: $canLoadMore")
        }
    }

    fun onErrorMessageShown() {
        _errorMessage.value = null
    }

    fun getCurrentQuery(): String? = currentQuery
}