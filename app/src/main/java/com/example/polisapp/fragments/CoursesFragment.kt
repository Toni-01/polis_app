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
// import androidx.appcompat.widget.SearchView // Remove this
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.polisapp.AddEditCourseActivity
import com.example.polisapp.CourseAdapter
import com.example.polisapp.CourseDetailActivity
import com.example.polisapp.R
import com.example.polisapp.ui.custom.CustomSearchBarView // Import
import com.example.polisapp.viewmodel.CoursesViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton

class CoursesFragment : Fragment() {

    private val TAG = "CoursesFragment"
    private lateinit var recyclerViewCourses: RecyclerView
    private lateinit var courseAdapter: CourseAdapter
    private lateinit var fabAddCourse: FloatingActionButton
    private lateinit var progressBarCourses: ProgressBar
    private lateinit var textViewEmptyCourses: TextView
    private lateinit var customSearchBar: CustomSearchBarView

    private lateinit var viewModel: CoursesViewModel
    private lateinit var layoutManager: LinearLayoutManager

    private val addEditCourseResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                viewModel.refreshCoursesList()
            }
        }

    private val viewCourseResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                viewModel.refreshCoursesList()
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(CoursesViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_courses, container, false)

        customSearchBar = view.findViewById(R.id.customSearchBarCourses)
        recyclerViewCourses = view.findViewById(R.id.recyclerViewCourses)
        fabAddCourse = view.findViewById(R.id.fabAddCourse)
        progressBarCourses = view.findViewById(R.id.progressBarCourses)
        textViewEmptyCourses = view.findViewById(R.id.textViewEmptyCourses)

        courseAdapter = CourseAdapter(mutableListOf()) { selectedCourse ->
            val intent = Intent(activity, CourseDetailActivity::class.java)
            intent.putExtra(CourseDetailActivity.EXTRA_COURSE, selectedCourse)
            viewCourseResultLauncher.launch(intent)
        }
        layoutManager = LinearLayoutManager(context)
        recyclerViewCourses.layoutManager = layoutManager
        recyclerViewCourses.adapter = courseAdapter

        setupCustomSearchBar()
        setupScrollListener()

        fabAddCourse.setOnClickListener {
            val intent = Intent(activity, AddEditCourseActivity::class.java)
            addEditCourseResultLauncher.launch(intent)
        }
        return view
    }

    private fun setupCustomSearchBar() {
        customSearchBar.setSearchHint("Search courses...")
        customSearchBar.setCurrentQuery(viewModel.getCurrentQuery())

        customSearchBar.setSearchQuerySubmittedListener { query ->
            viewModel.fetchCourses(query, isInitialLoad = true)
            customSearchBar.clearSearchViewFocus()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as? AppCompatActivity)?.supportActionBar?.title = "Courses"
        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.coursesList.observe(viewLifecycleOwner) { accumulatedCourses ->
            Log.d(TAG, "Observer: Updating adapter with ${accumulatedCourses.size} courses.")
            courseAdapter.setData(accumulatedCourses)
            updateListVisibility()
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            Log.d(TAG, "IsLoading Observer: $isLoading")
            updateListVisibility()
        }

        viewModel.isLoadingMore.observe(viewLifecycleOwner) { isLoadingMore ->
            Log.d(TAG, "IsLoadingMore Observer: $isLoadingMore")
            if (isLoadingMore) {
                if (courseAdapter.itemCount > 0) {
                    courseAdapter.addLoadingFooter()
                }
            } else {
                courseAdapter.removeLoadingFooter()
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
        val currentList = viewModel.coursesList.value ?: emptyList()

        if (isLoading) {
            progressBarCourses.visibility = View.VISIBLE
            recyclerViewCourses.visibility = View.GONE
            textViewEmptyCourses.visibility = View.GONE
        } else {
            progressBarCourses.visibility = View.GONE
            if (currentList.isEmpty()) {
                recyclerViewCourses.visibility = View.GONE
                textViewEmptyCourses.visibility = View.VISIBLE
            } else {
                recyclerViewCourses.visibility = View.VISIBLE
                textViewEmptyCourses.visibility = View.GONE
            }
        }
    }

    private fun setupScrollListener() {
        recyclerViewCourses.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                if (viewModel.isLoading.value == false && viewModel.isLoadingMore.value == false) {
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount &&
                        firstVisibleItemPosition >= 0 ) {
                        Log.d(TAG, "Courses Scroll near bottom. Triggering loadMoreCourses().")
                        viewModel.loadMoreCourses()
                    }
                }
            }
        })
    }

}