package com.example.polisapp.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.polisapp.dto.Pagination
import com.example.polisapp.dto.SimpleStringFilterDto
import com.example.polisapp.dto.TeacherDto
import com.example.polisapp.network.RetrofitClient
import kotlinx.coroutines.launch

class TeachersViewModel : ViewModel() {

    private val _teachersList = MutableLiveData<MutableList<TeacherDto>>(mutableListOf())
    val teachersList: LiveData<MutableList<TeacherDto>> = _teachersList

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
        private const val TAG = "TeachersViewModel"
    }

    init {
        fetchTeachers(query = null, isInitialLoad = true)
    }

    fun fetchTeachers(query: String? = null, isInitialLoad: Boolean = false) {
        if (query != currentQuery || isInitialLoad) {
            currentQuery = query
            currentPage = 0
            canLoadMore = true
            _teachersList.value?.clear()
            _teachersList.postValue(_teachersList.value) 
            _isLoading.value = true
            _isLoadingMore.value = false
            Log.d(TAG, "New search/refresh. Query: '$currentQuery'. Resetting page to 0.")
        } else if (!canLoadMore || _isLoading.value == true || _isLoadingMore.value == true) {
            Log.d(TAG, "Fetch declined: canLoadMore=$canLoadMore, isLoading=${_isLoading.value}, isLoadingMore=${_isLoadingMore.value}")
            return
        }

        if (!isInitialLoad) {
            _isLoadingMore.value = true
        }
        _errorMessage.value = null

        val pagination = Pagination(pageNumber = currentPage, pageSize = pageSize, sort = null)
        val filterDto = SimpleStringFilterDto(filter = currentQuery?.trim().takeIf { !it.isNullOrBlank() }, pagination = pagination)

        Log.d(TAG, "Fetching teachers page: $currentPage, query: '$currentQuery', DTO: $filterDto")

        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.filterTeachers(filterDto)
                if (response.isSuccessful) {
                    val responseBody = response.body()
                    val serverSlice = responseBody?.slice

                    Log.d(TAG, "Server Slice Details: " +
                            "content.size=${serverSlice?.content?.size}, " +
                            "hasNext=${serverSlice?.hasNext}, isLast=${serverSlice?.last}, " + 
                            "number=${serverSlice?.number}, size=${serverSlice?.size}, " +
                            "numberOfElements=${serverSlice?.numberOfElements}")

                    val newTeachers = serverSlice?.content

                    
                    canLoadMore = when {
                        serverSlice?.hasNext != null -> serverSlice.hasNext!! 
                        serverSlice?.last != null -> !serverSlice.last!!      
                        else -> false                                         
                    }
                    

                    if (!newTeachers.isNullOrEmpty()) {
                        val currentList = _teachersList.value ?: mutableListOf()
                        if (isInitialLoad || currentPage == 0 && (currentQuery != query || isInitialLoad) ) { 
                            currentList.clear()
                        }
                        currentList.addAll(newTeachers)
                        _teachersList.postValue(currentList)
                        currentPage++
                        Log.i(TAG, "Page ${serverSlice?.number ?: currentPage-1} (0-indexed response) teachers fetched. " +
                                "Added ${newTeachers.size}. Total now: ${currentList.size}. Next page to fetch: $currentPage. Can load more: $canLoadMore")
                    } else {
                        if (currentPage == 0) {
                            _teachersList.postValue(mutableListOf())
                            Log.w(TAG, "No teachers found for query '$currentQuery' on first page.")
                        }
                        
                        Log.d(TAG, "No new teachers on this page (server page ${serverSlice?.number}). Effective canLoadMore: $canLoadMore")
                    }

                    if (responseBody?.status?.isNotEmpty() == true && responseBody.status.any{ it.code != com.example.polisapp.dto.ServerErrorEnum.OK}) {
                        _errorMessage.postValue(responseBody.status.joinToString { it.message ?: "Unknown server error" })
                    }

                } else {
                    val errorMsg = "API Error: ${response.code()} - ${response.errorBody()?.string()}"
                    Log.e(TAG, errorMsg)
                    _errorMessage.postValue("Failed to load teachers. Please try again.")
                    canLoadMore = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception fetching teachers: query '$currentQuery', page $currentPage: ${e.message}", e)
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

    fun refreshTeachersList() {
        Log.d(TAG, "refreshTeachersList called. Current query: '$currentQuery'")
        fetchTeachers(query = currentQuery, isInitialLoad = true)
    }

    fun loadMoreTeachers() {
        Log.d(TAG, "loadMoreTeachers called. Conditions: canLoadMore=$canLoadMore, isLoading=${_isLoading.value}, isLoadingMore=${_isLoadingMore.value}")
        if (canLoadMore && _isLoading.value == false && _isLoadingMore.value == false) {
            fetchTeachers(query = currentQuery, isInitialLoad = false)
        } else {
            Log.d(TAG, "Cannot load more. Effective canLoadMore: $canLoadMore")
        }
    }

    fun onErrorMessageShown() {
        _errorMessage.value = null
    }

    fun getCurrentQuery(): String? = currentQuery
}