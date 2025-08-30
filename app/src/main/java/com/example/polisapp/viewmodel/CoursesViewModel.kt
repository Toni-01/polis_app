package com.example.polisapp.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.polisapp.dto.CourseDto
import com.example.polisapp.dto.Pagination
import com.example.polisapp.dto.SimpleStringFilterDto
import com.example.polisapp.network.RetrofitClient
import kotlinx.coroutines.launch

class CoursesViewModel : ViewModel() {

    private val _coursesList = MutableLiveData<MutableList<CourseDto>>(mutableListOf())
    val coursesList: LiveData<MutableList<CourseDto>> = _coursesList

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
        private const val TAG = "CoursesViewModel"
    }

    init {
        fetchCourses(query = null, isInitialLoad = true)
    }

    fun fetchCourses(query: String? = null, isInitialLoad: Boolean = false) {
        if (query != currentQuery || isInitialLoad) {
            currentQuery = query
            currentPage = 0
            canLoadMore = true
            _coursesList.value?.clear()
            _coursesList.postValue(_coursesList.value) 
            _isLoading.value = true
            _isLoadingMore.value = false
            Log.d(TAG, "New search/refresh for Courses. Query: '$currentQuery'. Resetting page to 0.")
        } else if (!canLoadMore || _isLoading.value == true || _isLoadingMore.value == true) {
            Log.d(TAG, "Course fetch declined: canLoadMore=$canLoadMore, isLoading=${_isLoading.value}, isLoadingMore=${_isLoadingMore.value}")
            return
        }

        if (!isInitialLoad) {
            _isLoadingMore.value = true
        }
        _errorMessage.value = null

        val pagination = Pagination(pageNumber = currentPage, pageSize = pageSize, sort = null)
        val filterDto = SimpleStringFilterDto(filter = currentQuery?.trim().takeIf { !it.isNullOrBlank() }, pagination = pagination)

        Log.d(TAG, "Fetching courses page: $currentPage, query: '$currentQuery', DTO: $filterDto")

        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.filterCourses(filterDto)
                if (response.isSuccessful) {
                    val responseBody = response.body()
                    val serverSlice = responseBody?.slice

                    Log.d(TAG, "Courses Server Slice Details: " +
                            "content.size=${serverSlice?.content?.size}, " +
                            "hasNext=${serverSlice?.hasNext}, isLast=${serverSlice?.last}, " +
                            "number=${serverSlice?.number}, size=${serverSlice?.size}, " +
                            "numberOfElements=${serverSlice?.numberOfElements}")

                    val newCourses = serverSlice?.content
                    canLoadMore = when {
                        serverSlice?.hasNext != null -> serverSlice.hasNext!!
                        serverSlice?.last != null -> !serverSlice.last!!
                        else -> false
                    }

                    if (!newCourses.isNullOrEmpty()) {
                        val currentList = _coursesList.value ?: mutableListOf()
                        
                        currentList.addAll(newCourses)
                        _coursesList.postValue(currentList)
                        currentPage++
                        Log.i(TAG, "Page ${serverSlice?.number ?: currentPage-1} (0-indexed) courses fetched. " +
                                "Added ${newCourses.size}. Total now: ${currentList.size}. Next page to fetch: $currentPage. Can load more: $canLoadMore")
                    } else {
                        if (currentPage == 0) {
                            _coursesList.postValue(mutableListOf())
                            Log.w(TAG, "No courses found for query '$currentQuery' on first page.")
                        }
                        Log.d(TAG, "No new courses on this page (server page ${serverSlice?.number}). Effective canLoadMore: $canLoadMore")
                    }

                    if (responseBody?.status?.isNotEmpty() == true && responseBody.status.any{ it.code != com.example.polisapp.dto.ServerErrorEnum.OK}) {
                        _errorMessage.postValue(responseBody.status.joinToString { it.message ?: "Unknown server error" })
                    }
                } else {
                    val errorMsg = "API Error fetching courses: ${response.code()} - ${response.errorBody()?.string()}"
                    Log.e(TAG, errorMsg)
                    _errorMessage.postValue("Failed to load courses. Please try again.")
                    canLoadMore = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception fetching courses: query '$currentQuery', page $currentPage: ${e.message}", e)
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

    fun refreshCoursesList() {
        Log.d(TAG, "refreshCoursesList called. Current query: '$currentQuery'")
        fetchCourses(query = currentQuery, isInitialLoad = true)
    }

    fun loadMoreCourses() {
        Log.d(TAG, "loadMoreCourses called. Conditions: canLoadMore=$canLoadMore, isLoading=${_isLoading.value}, isLoadingMore=${_isLoadingMore.value}")
        if (canLoadMore && _isLoading.value == false && _isLoadingMore.value == false) {
            fetchCourses(query = currentQuery, isInitialLoad = false)
        } else {
            Log.d(TAG, "Cannot load more courses. Effective canLoadMore: $canLoadMore")
        }
    }

    fun onErrorMessageShown() {
        _errorMessage.value = null
    }

    fun getCurrentQuery(): String? = currentQuery
}