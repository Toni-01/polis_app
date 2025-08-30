package com.example.polisapp.fragments

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment

import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.polisapp.AddEditTeacherActivity
import com.example.polisapp.R
import com.example.polisapp.TeacherAdapter
import com.example.polisapp.TeacherDetailActivity
import com.example.polisapp.ui.custom.CustomSearchBarView 
import com.example.polisapp.viewmodel.TeachersViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton

class TeachersFragment : Fragment() {

    private val TAG = "TeachersFragment"
    private lateinit var recyclerViewTeachers: RecyclerView
    private lateinit var teacherAdapter: TeacherAdapter
    private lateinit var fabAddTeacher: FloatingActionButton
    private lateinit var progressBarTeachers: ProgressBar
    private lateinit var textViewEmptyTeachers: TextView
    private lateinit var customSearchBar: CustomSearchBarView 

    private lateinit var viewModel: TeachersViewModel
    private lateinit var layoutManager: LinearLayoutManager

    
    private val addEditTeacherResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                Log.d(TAG, "Returned from AddEditTeacherActivity. Refreshing list.")
                viewModel.refreshTeachersList()
            }
        }

    private val viewTeacherResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                Log.d(TAG, "Returned from TeacherDetailActivity. Refreshing list.")
                viewModel.refreshTeachersList()
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        viewModel = ViewModelProvider(this).get(TeachersViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_teachers, container, false)

        customSearchBar = view.findViewById(R.id.customSearchBarTeachers) 
        recyclerViewTeachers = view.findViewById(R.id.recyclerViewTeachers)
        fabAddTeacher = view.findViewById(R.id.fabAddTeacher)
        progressBarTeachers = view.findViewById(R.id.progressBarTeachers)
        textViewEmptyTeachers = view.findViewById(R.id.textViewEmptyTeachers)

        teacherAdapter = TeacherAdapter(mutableListOf()) { selectedTeacher ->
            val intent = Intent(activity, TeacherDetailActivity::class.java)
            intent.putExtra(TeacherDetailActivity.EXTRA_TEACHER, selectedTeacher)
            viewTeacherResultLauncher.launch(intent)
        }
        layoutManager = LinearLayoutManager(context)
        recyclerViewTeachers.layoutManager = layoutManager
        recyclerViewTeachers.adapter = teacherAdapter

        setupCustomSearchBar() 
        setupScrollListener()

        fabAddTeacher.setOnClickListener {
            val intent = Intent(activity, AddEditTeacherActivity::class.java)
            addEditTeacherResultLauncher.launch(intent)
        }
        return view
    }

    private fun setupCustomSearchBar() {
        customSearchBar.setSearchHint("Search teachers...")
        customSearchBar.setCurrentQuery(viewModel.getCurrentQuery()) 

        customSearchBar.setSearchQuerySubmittedListener { query ->
            viewModel.fetchTeachers(query, isInitialLoad = true)
            customSearchBar.clearSearchViewFocus() 
        }
        
        customSearchBar.setOnSearchClearedListener {
            
            
            
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as? AppCompatActivity)?.supportActionBar?.title = "Teachers"
        observeViewModel()
    }

    
    

    private fun observeViewModel() {
        viewModel.teachersList.observe(viewLifecycleOwner) { accumulatedTeachers ->
            Log.d(TAG, "Observer: Updating adapter with ${accumulatedTeachers.size} teachers.")
            teacherAdapter.setData(accumulatedTeachers)

            updateListVisibility()
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            Log.d(TAG, "IsLoading Observer: $isLoading")
            
            updateListVisibility()
        }

        viewModel.isLoadingMore.observe(viewLifecycleOwner) { isLoadingMore ->
            Log.d(TAG, "IsLoadingMore Observer: $isLoadingMore")
            if (isLoadingMore) {
                if (teacherAdapter.itemCount > 0) {
                    teacherAdapter.addLoadingFooter()
                }
            } else {
                teacherAdapter.removeLoadingFooter()
            }
            
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { errorMessage ->
            errorMessage?.let {
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                viewModel.onErrorMessageShown()
                updateListVisibility() 
            }
        }
    }

    private fun updateListVisibility() {
        val isLoading = viewModel.isLoading.value ?: false
        
        val currentList = viewModel.teachersList.value ?: emptyList()

        if (isLoading) {
            progressBarTeachers.visibility = View.VISIBLE
            recyclerViewTeachers.visibility = View.GONE
            textViewEmptyTeachers.visibility = View.GONE
        } else {
            progressBarTeachers.visibility = View.GONE
            if (currentList.isEmpty()) {
                recyclerViewTeachers.visibility = View.GONE
                textViewEmptyTeachers.visibility = View.VISIBLE
            } else {
                recyclerViewTeachers.visibility = View.VISIBLE
                textViewEmptyTeachers.visibility = View.GONE
            }
        }
    }
    private fun setupScrollListener() {
        recyclerViewTeachers.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                if (viewModel.isLoading.value == false && viewModel.isLoadingMore.value == false) {
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount &&
                        firstVisibleItemPosition >= 0 ) {
                        Log.d(TAG, "Teachers Scroll near bottom. Triggering loadMoreTeachers().")
                        viewModel.loadMoreTeachers()
                    }
                }
            }
        })
    }


    
    
    
}