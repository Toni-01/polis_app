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
import com.example.polisapp.dto.StudentDto
import com.example.polisapp.viewmodel.AddEditStudentViewModel
import com.example.polisapp.viewmodel.StudentSaveResult 
import com.google.android.material.textfield.TextInputEditText

class AddEditStudentActivity : AppCompatActivity() {

    private lateinit var editTextStudentFirstName: TextInputEditText
    private lateinit var editTextStudentLastName: TextInputEditText
    private lateinit var editTextStudentSerialNumber: TextInputEditText
    private lateinit var spinnerSelectCourse: Spinner
    private lateinit var buttonSaveStudent: Button
    

    private lateinit var viewModel: AddEditStudentViewModel
    private var allCoursesListForSpinner: List<CourseDto> = emptyList()
    private var selectedCourseFromSpinner: CourseDto? = null

    companion object {
        private const val TAG = "AddEditStudentActivity"
        const val EXTRA_EDIT_STUDENT = "extra_edit_student"
        const val EXTRA_REPLY_STUDENT = "extra_reply_student"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_edit_student)

        viewModel = ViewModelProvider(this).get(AddEditStudentViewModel::class.java)

        editTextStudentFirstName = findViewById(R.id.editTextStudentFirstName)
        editTextStudentLastName = findViewById(R.id.editTextStudentLastName)
        editTextStudentSerialNumber = findViewById(R.id.editTextStudentSerialNumber)
        spinnerSelectCourse = findViewById(R.id.spinnerSelectCourse)
        buttonSaveStudent = findViewById(R.id.buttonSaveStudent)
        

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val studentToEdit = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra(EXTRA_EDIT_STUDENT, StudentDto::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra(EXTRA_EDIT_STUDENT) as? StudentDto
        }

        viewModel.setExistingStudent(studentToEdit)
        if (studentToEdit != null) {
            supportActionBar?.title = "Edit Student"
            populateFields(studentToEdit)
        } else {
            supportActionBar?.title = "Add New Student"
        }

        buttonSaveStudent.setOnClickListener {
            val firstName = editTextStudentFirstName.text.toString()
            val lastName = editTextStudentLastName.text.toString()
            val serialNumber = editTextStudentSerialNumber.text.toString()
            viewModel.saveStudent(firstName, lastName, serialNumber, selectedCourseFromSpinner)
        }

        observeViewModel()
        viewModel.fetchCoursesForSpinner() 
    }

    private fun populateFields(student: StudentDto) {
        editTextStudentFirstName.setText(student.firstName)
        editTextStudentLastName.setText(student.lastName)
        editTextStudentSerialNumber.setText(student.serialNumber)
        
    }

    private fun setupCourseSpinner(courses: List<CourseDto>) {
        allCoursesListForSpinner = courses
        val courseDisplayNames = mutableListOf("No Course (Optional)")
        courseDisplayNames.addAll(courses.map { "${it.title ?: "Untitled"} (Code: ${it.code ?: "N/A"})" })

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, courseDisplayNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerSelectCourse.adapter = adapter

        val initialStudentData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra(EXTRA_EDIT_STUDENT, StudentDto::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra(EXTRA_EDIT_STUDENT) as? StudentDto
        }

        initialStudentData?.course?.id?.let { existingCourseId ->
            val position = allCoursesListForSpinner.indexOfFirst { it.id == existingCourseId }
            if (position != -1) {
                spinnerSelectCourse.setSelection(position + 1)
            } else {
                spinnerSelectCourse.setSelection(0)
            }
        } ?: spinnerSelectCourse.setSelection(0)


        spinnerSelectCourse.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedCourseFromSpinner = if (position == 0) null else allCoursesListForSpinner[position - 1]
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
                selectedCourseFromSpinner = null
            }
        }
    }

    private fun observeViewModel() {
        viewModel.allCourses.observe(this) { courses ->
            setupCourseSpinner(courses)
        }

        viewModel.coursesLoadingError.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                viewModel.onCourseLoadingErrorShown()
            }
        }

        viewModel.saveResult.observe(this) { result ->
            when (result) {
                is StudentSaveResult.Loading -> {
                    buttonSaveStudent.isEnabled = false
                    
                    Log.d(TAG, "Saving student... Loading state.")
                }
                is StudentSaveResult.Success -> {
                    buttonSaveStudent.isEnabled = true
                    
                    Toast.makeText(this, "Student saved!", Toast.LENGTH_SHORT).show()
                    Log.i(TAG, "Student saved successfully: ${result.student}")

                    val resultIntent = Intent()
                    resultIntent.putExtra(EXTRA_REPLY_STUDENT, result.student)
                    setResult(Activity.RESULT_OK, resultIntent)
                    finish()
                    viewModel.onSaveResultConsumed()
                }
                is StudentSaveResult.Error -> {
                    buttonSaveStudent.isEnabled = true
                    
                    val errorMessage = result.messages.joinToString("\n")
                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                    Log.e(TAG, "Error saving student: $errorMessage")
                    viewModel.onSaveResultConsumed()
                }
                is StudentSaveResult.Idle -> {
                    buttonSaveStudent.isEnabled = true
                    
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        if (viewModel.saveResult.value is StudentSaveResult.Loading) {
            Toast.makeText(this, "Save in progress...", Toast.LENGTH_SHORT).show()
            return true
        }
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}