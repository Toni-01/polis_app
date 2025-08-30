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
import com.example.polisapp.AddEditStudentActivity
import com.example.polisapp.R
import com.example.polisapp.StudentAdapter
import com.example.polisapp.StudentDetailActivity
import com.example.polisapp.ui.custom.CustomSearchBarView 
import com.example.polisapp.viewmodel.StudentsViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton

class StudentsFragment : Fragment() {

    private val TAG = "StudentsFragment"
    private lateinit var recyclerViewStudents: RecyclerView
    private lateinit var studentAdapter: StudentAdapter
    private lateinit var fabAddStudent: FloatingActionButton
    private lateinit var progressBarStudents: ProgressBar
    private lateinit var textViewEmptyStudents: TextView
    private lateinit var customSearchBar: CustomSearchBarView

    private lateinit var viewModel: StudentsViewModel
    private lateinit var layoutManager: LinearLayoutManager

    private val addEditStudentResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                viewModel.refreshStudentsList()
            }
        }

    private val viewStudentResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                viewModel.refreshStudentsList()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(StudentsViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_students, container, false)

        customSearchBar = view.findViewById(R.id.customSearchBarStudents)
        recyclerViewStudents = view.findViewById(R.id.recyclerViewStudents)
        fabAddStudent = view.findViewById(R.id.fabAddStudent)
        progressBarStudents = view.findViewById(R.id.progressBarStudents)
        textViewEmptyStudents = view.findViewById(R.id.textViewEmptyStudents)

        studentAdapter = StudentAdapter(mutableListOf()) { selectedStudent ->
            val intent = Intent(activity, StudentDetailActivity::class.java)
            intent.putExtra(StudentDetailActivity.EXTRA_STUDENT, selectedStudent)
            viewStudentResultLauncher.launch(intent)
        }
        layoutManager = LinearLayoutManager(context)
        recyclerViewStudents.layoutManager = layoutManager
        recyclerViewStudents.adapter = studentAdapter

        setupCustomSearchBar()
        setupScrollListener()

        fabAddStudent.setOnClickListener {
            val intent = Intent(activity, AddEditStudentActivity::class.java)
            addEditStudentResultLauncher.launch(intent)
        }
        return view
    }

    private fun setupCustomSearchBar() {
        customSearchBar.setSearchHint("Search students...")
        customSearchBar.setCurrentQuery(viewModel.getCurrentQuery())

        customSearchBar.setSearchQuerySubmittedListener { query ->
            viewModel.fetchStudents(query, isInitialLoad = true)
            customSearchBar.clearSearchViewFocus()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as? AppCompatActivity)?.supportActionBar?.title = "Students"
        observeViewModel()
    }

    private fun observeViewModel() { 
        viewModel.studentsList.observe(viewLifecycleOwner) { accumulatedStudents ->
            Log.d(TAG, "Observer: Updating adapter with ${accumulatedStudents.size} students.")
            studentAdapter.setData(accumulatedStudents)
            updateListVisibility()
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            Log.d(TAG, "IsLoading Observer: $isLoading")
            updateListVisibility()
        }

        viewModel.isLoadingMore.observe(viewLifecycleOwner) { isLoadingMore ->
            Log.d(TAG, "IsLoadingMore Observer: $isLoadingMore")
            if (isLoadingMore) {
                if (studentAdapter.itemCount > 0) {
                    studentAdapter.addLoadingFooter()
                }
            } else {
                studentAdapter.removeLoadingFooter()
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
        val currentList = viewModel.studentsList.value ?: emptyList()

        if (isLoading) {
            progressBarStudents.visibility = View.VISIBLE
            recyclerViewStudents.visibility = View.GONE
            textViewEmptyStudents.visibility = View.GONE
        } else {
            progressBarStudents.visibility = View.GONE
            if (currentList.isEmpty()) {
                recyclerViewStudents.visibility = View.GONE
                textViewEmptyStudents.visibility = View.VISIBLE
            } else {
                recyclerViewStudents.visibility = View.VISIBLE
                textViewEmptyStudents.visibility = View.GONE
            }
        }
    }

    private fun setupScrollListener() { 
        recyclerViewStudents.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                if (viewModel.isLoading.value == false && viewModel.isLoadingMore.value == false) {
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount &&
                        firstVisibleItemPosition >= 0 ) {
                        Log.d(TAG, "Students Scroll near bottom. Triggering loadMoreStudents().")
                        viewModel.loadMoreStudents()
                    }
                }
            }
        })
    }

    
    
    
}