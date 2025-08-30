package com.example.polisapp.ui.custom

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageButton
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.doAfterTextChanged
import com.example.polisapp.R

class CustomSearchBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val editTextSearchQuery: EditText
    private val buttonClearSearch: ImageButton

    private var onSearchQuerySubmittedListener: ((String) -> Unit)? = null
    private var onSearchClearedListener: (() -> Unit)? = null

    init {
        LayoutInflater.from(context).inflate(R.layout.layout_custom_search_bar, this, true)
        editTextSearchQuery = findViewById(R.id.editTextSearchQuery)
        buttonClearSearch = findViewById(R.id.buttonClearSearch)
        setupListeners()
    }

    private fun setupListeners() {
        editTextSearchQuery.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = editTextSearchQuery.text.toString().trim()
                onSearchQuerySubmittedListener?.invoke(query)
                return@setOnEditorActionListener true
            }
            false
        }

        editTextSearchQuery.doAfterTextChanged { text ->
            buttonClearSearch.visibility = if (text.isNullOrEmpty()) View.GONE else View.VISIBLE
        }

        buttonClearSearch.setOnClickListener {
            editTextSearchQuery.setText("")
            
            
            onSearchQuerySubmittedListener?.invoke("") 
            onSearchClearedListener?.invoke()          
        }
    }

    fun setSearchQuerySubmittedListener(listener: (String) -> Unit) {
        onSearchQuerySubmittedListener = listener
    }

    fun setOnSearchClearedListener(listener: () -> Unit) {
        onSearchClearedListener = listener
    }

    fun setSearchHint(hint: String) {
        editTextSearchQuery.hint = hint
    }

    fun getCurrentQuery(): String {
        return editTextSearchQuery.text.toString().trim()
    }

    fun setCurrentQuery(query: String?) {
        editTextSearchQuery.setText(query ?: "")
        
        buttonClearSearch.visibility = if (query.isNullOrEmpty()) View.GONE else View.VISIBLE
    }

    fun requestSearchViewFocus() {
        editTextSearchQuery.requestFocus()
    }

    fun clearSearchViewFocus() {
        editTextSearchQuery.clearFocus()
    }
}