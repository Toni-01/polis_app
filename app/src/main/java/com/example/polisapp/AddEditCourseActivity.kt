package com.example.polisapp

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.polisapp.dto.CourseDto
import com.example.polisapp.dto.TeacherDto
import com.example.polisapp.viewmodel.AddEditCourseViewModel
import com.example.polisapp.viewmodel.CourseSaveResult 
import com.google.android.material.textfield.TextInputEditText

class AddEditCourseActivity : AppCompatActivity() {

    private lateinit var editTextCourseCode: TextInputEditText
    private lateinit var editTextCourseTitle: TextInputEditText
    private lateinit var editTextCourseDescription: TextInputEditText
    private lateinit var editTextCourseYear: TextInputEditText
    private lateinit var spinnerSelectTeacher: Spinner
    private lateinit var buttonSaveCourse: Button
    

    private lateinit var viewModel: AddEditCourseViewModel
    private var allTeachersListForSpinner: List<TeacherDto> = emptyList()
    private var selectedTeacherFromSpinner: TeacherDto? = null


    companion object {
        private const val TAG = "AddEditCourseActivity"
        const val EXTRA_EDIT_COURSE = "extra_edit_course"
        const val EXTRA_REPLY_COURSE = "extra_reply_course"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_edit_course)

        viewModel = ViewModelProvider(this).get(AddEditCourseViewModel::class.java)

        editTextCourseCode = findViewById(R.id.editTextCourseCode)
        editTextCourseTitle = findViewById(R.id.editTextCourseTitle)
        editTextCourseDescription = findViewById(R.id.editTextCourseDescription)
        editTextCourseYear = findViewById(R.id.editTextCourseYear)
        spinnerSelectTeacher = findViewById(R.id.spinnerSelectTeacher)
        buttonSaveCourse = findViewById(R.id.buttonSaveCourse)
        

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val courseToEdit = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra(EXTRA_EDIT_COURSE, CourseDto::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra(EXTRA_EDIT_COURSE) as? CourseDto
        }

        viewModel.setExistingCourse(courseToEdit)
        if (courseToEdit != null) {
            supportActionBar?.title = "Edit Course"
            populateFields(courseToEdit)
        } else {
            supportActionBar?.title = "Add New Course"
        }

        buttonSaveCourse.setOnClickListener {
            val code = editTextCourseCode.text.toString()
            val title = editTextCourseTitle.text.toString()
            val description = editTextCourseDescription.text.toString()
            val yearString = editTextCourseYear.text.toString()
            viewModel.saveCourse(code, title, description, yearString, selectedTeacherFromSpinner)
        }

        observeViewModel()
        viewModel.fetchTeachersForSpinner() 
    }

    private fun populateFields(course: CourseDto) {
        editTextCourseCode.setText(course.code)
        editTextCourseTitle.setText(course.title)
        editTextCourseDescription.setText(course.description)
        editTextCourseYear.setText(course.year?.toString() ?: "")
        
    }

    private fun setupTeacherSpinner(teachers: List<TeacherDto>) {
        allTeachersListForSpinner = teachers
        val teacherDisplayNames = mutableListOf("No Teacher (Optional)")
        teacherDisplayNames.addAll(teachers.map { "${it.firstName ?: ""} ${it.lastName ?: ""} (ID: ${it.id ?: "N/A"})" })

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, teacherDisplayNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerSelectTeacher.adapter = adapter

        
        val initialCourseData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra(EXTRA_EDIT_COURSE, CourseDto::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra(EXTRA_EDIT_COURSE) as? CourseDto
        }

        
        initialCourseData?.teacher?.id?.let { existingTeacherId ->
            val position = allTeachersListForSpinner.indexOfFirst { it.id == existingTeacherId }
            if (position != -1) {
                spinnerSelectTeacher.setSelection(position + 1) 
            } else {
                spinnerSelectTeacher.setSelection(0) 
            }
        } ?: spinnerSelectTeacher.setSelection(0) 


        spinnerSelectTeacher.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedTeacherFromSpinner = if (position == 0) null else allTeachersListForSpinner[position - 1]
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
                selectedTeacherFromSpinner = null
            }
        }
    }


    private fun observeViewModel() {
        viewModel.allTeachers.observe(this) { teachers ->
            setupTeacherSpinner(teachers)
        }

        viewModel.teachersLoadingError.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                viewModel.onTeacherLoadingErrorShown()
            }
        }

        viewModel.saveResult.observe(this) { result ->
            when (result) {
                is CourseSaveResult.Loading -> {
                    buttonSaveCourse.isEnabled = false
                    
                    Log.d(TAG, "Saving course... Loading state.")
                }
                is CourseSaveResult.Success -> {
                    buttonSaveCourse.isEnabled = true
                    
                    Toast.makeText(this, "Course saved!", Toast.LENGTH_SHORT).show()
                    Log.i(TAG, "Course saved successfully: ${result.course}")

                    val resultIntent = Intent()
                    resultIntent.putExtra(EXTRA_REPLY_COURSE, result.course)
                    setResult(Activity.RESULT_OK, resultIntent)
                    finish()
                    viewModel.onSaveResultConsumed()
                }
                is CourseSaveResult.Error -> {
                    buttonSaveCourse.isEnabled = true
                    
                    val errorMessage = result.messages.joinToString("\n")
                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                    Log.e(TAG, "Error saving course: $errorMessage")
                    viewModel.onSaveResultConsumed()
                }
                is CourseSaveResult.Idle -> {
                    buttonSaveCourse.isEnabled = true
                    
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        if (viewModel.saveResult.value is CourseSaveResult.Loading) {
            Toast.makeText(this, "Save in progress...", Toast.LENGTH_SHORT).show()
            return true
        }
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}