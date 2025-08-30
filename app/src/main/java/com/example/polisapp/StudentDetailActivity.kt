package com.example.polisapp

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Button // Import Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.polisapp.dto.* // Import all DTOs
import com.example.polisapp.network.RetrofitClient
import kotlinx.coroutines.launch

class StudentDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_STUDENT = "extra_student"
        private const val TAG = "StudentDetailActivity"
    }

    private var currentStudent: StudentDto? = null
    private var allCourses: List<CourseDto> = emptyList() // To store fetched courses for selection

    // Views
    private lateinit var textViewDetailStudentId: TextView
    private lateinit var textViewDetailStudentFirstName: TextView
    private lateinit var textViewDetailStudentLastName: TextView
    private lateinit var textViewDetailStudentSerialNumber: TextView
    private lateinit var textViewDetailStudentCourse: TextView
    private lateinit var buttonManageCourseEnrollment: Button // New Button

    private val editStudentResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val updatedStudentData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    result.data?.getSerializableExtra(AddEditStudentActivity.EXTRA_REPLY_STUDENT, StudentDto::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    result.data?.getSerializableExtra(AddEditStudentActivity.EXTRA_REPLY_STUDENT) as? StudentDto
                }
                // Re-fetch student to get the latest state, especially if course association changed via AddEdit screen
                currentStudent?.id?.let { fetchStudentDetails(it) }
                setResult(Activity.RESULT_OK)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_detail)

        initializeViews()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val studentFromIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra(EXTRA_STUDENT, StudentDto::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra(EXTRA_STUDENT) as? StudentDto
        }

        if (studentFromIntent?.id != null) {
            fetchStudentDetails(studentFromIntent.id!!) // Fetch fresh details
            fetchAllCoursesForSelection() // Fetch courses for enrollment dialog
        } else {
            Toast.makeText(this, "Error: Student ID not found.", Toast.LENGTH_LONG).show()
            finish()
        }

        buttonManageCourseEnrollment.setOnClickListener {
            handleManageCourseEnrollment()
        }
    }

    private fun initializeViews() {
        textViewDetailStudentId = findViewById(R.id.textViewDetailStudentId)
        textViewDetailStudentFirstName = findViewById(R.id.textViewDetailStudentFirstName)
        textViewDetailStudentLastName = findViewById(R.id.textViewDetailStudentLastName)
        textViewDetailStudentSerialNumber = findViewById(R.id.textViewDetailStudentSerialNumber)
        textViewDetailStudentCourse = findViewById(R.id.textViewDetailStudentCourse)
        buttonManageCourseEnrollment = findViewById(R.id.buttonManageCourseEnrollment)
    }

    private fun fetchStudentDetails(studentId: Long) {
        lifecycleScope.launch {
            try {
                Log.d(TAG, "Fetching details for student ID: $studentId")
                val response = RetrofitClient.apiService.getStudent(LongIdDto(studentId))
                if (response.isSuccessful && response.body()?.data != null) {
                    currentStudent = response.body()!!.data!!
                    populateStudentDetails(currentStudent!!)
                    setResult(Activity.RESULT_OK) // Data might have changed, signal MainActivity
                } else {
                    Log.e(TAG, "Failed to fetch student details. Code: ${response.code()}, Msg: ${response.errorBody()?.string()}")
                    Toast.makeText(this@StudentDetailActivity, "Error fetching student details.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception fetching student details: ${e.message}", e)
                Toast.makeText(this@StudentDetailActivity, "Network error.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun populateStudentDetails(student: StudentDto) {
        supportActionBar?.title = "${student.firstName ?: ""} ${student.lastName ?: ""}"
        textViewDetailStudentId.text = student.id?.toString() ?: "N/A"
        textViewDetailStudentFirstName.text = student.firstName ?: "N/A"
        textViewDetailStudentLastName.text = student.lastName ?: "N/A"
        textViewDetailStudentSerialNumber.text = student.serialNumber ?: "N/A"
        updateCourseEnrollmentUI(student.course)
    }

    private fun updateCourseEnrollmentUI(course: CourseDto?) {
        if (course != null) {
            val courseInfo = "${course.code ?: ""} - ${course.title ?: "Untitled Course"}".trim()
            textViewDetailStudentCourse.text = if (courseInfo.isNotBlank() && courseInfo != "-") courseInfo else "Enrolled (Details N/A)"
            buttonManageCourseEnrollment.text = "Change/Unenroll Course"
        } else {
            textViewDetailStudentCourse.text = "Not enrolled in any course"
            buttonManageCourseEnrollment.text = "Enroll in Course"
        }
    }

    private fun fetchAllCoursesForSelection() {
        lifecycleScope.launch {
            try {
                // Fetch all courses (or a reasonable number)
                val pagination = Pagination(pageNumber = 0, pageSize = 200, sort = null)
                val filterDto = SimpleStringFilterDto(filter = null, pagination = pagination)
                val response = RetrofitClient.apiService.filterCourses(filterDto)
                if (response.isSuccessful && response.body()?.slice?.content != null) {
                    allCourses = response.body()!!.slice!!.content!!
                } else {
                    Log.e(TAG, "Failed to fetch all courses for selection.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception fetching all courses: ${e.message}", e)
            }
        }
    }

    private fun handleManageCourseEnrollment() {
        if (currentStudent == null) return

        if (currentStudent?.course != null) {
            // Options: Change or Unenroll
            AlertDialog.Builder(this)
                .setTitle("Manage Course Enrollment")
                .setItems(arrayOf("Change Course", "Unenroll from Course")) { _, which ->
                    when (which) {
                        0 -> showCourseSelectionDialog("Change Course Enrollment")
                        1 -> confirmUnenrollStudent()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        } else {
            // Option: Enroll
            showCourseSelectionDialog("Enroll Student in Course")
        }
    }

    private fun showCourseSelectionDialog(title: String) {
        if (allCourses.isEmpty()) {
            Toast.makeText(this, "No courses available to select.", Toast.LENGTH_SHORT).show()
            fetchAllCoursesForSelection() // Attempt to refresh
            return
        }

        val courseDisplayNames = allCourses.map { "${it.title ?: "Untitled"} (Code: ${it.code ?: "N/A"})" }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle(title)
            .setItems(courseDisplayNames) { _, which ->
                val selectedCourse = allCourses[which]
                currentStudent?.id?.let { studentId ->
                    associateStudent(studentId, selectedCourse.id!!)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmUnenrollStudent() {
        val courseToUnenrollFrom = currentStudent?.course ?: return
        val studentId = currentStudent?.id ?: return

        AlertDialog.Builder(this)
            .setTitle("Unenroll Student")
            .setMessage("Are you sure you want to unenroll ${currentStudent?.firstName} from ${courseToUnenrollFrom.title}?")
            .setPositiveButton("Unenroll") { _, _ ->
                removeStudentAssociation(studentId, courseToUnenrollFrom.id!!)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun associateStudent(studentId: Long, courseId: Long) {
        lifecycleScope.launch {
            try {
                val assocDto = CourseStudentAssocDto(idStudent = studentId, idCourse = courseId)
                Log.d(TAG, "Attempting to associate student $studentId with course $courseId")
                val response = RetrofitClient.apiService.associateStudentToCourse(assocDto)
                if (response.isSuccessful) {
                    Toast.makeText(this@StudentDetailActivity, "Student enrolled successfully.", Toast.LENGTH_SHORT).show()
                    fetchStudentDetails(studentId) // Refresh details
                } else {
                    Log.e(TAG, "Failed to enroll student. Code: ${response.code()}, Msg: ${response.errorBody()?.string()}, Status: ${response.body()?.status}")
                    Toast.makeText(this@StudentDetailActivity, "Error enrolling student.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception enrolling student: ${e.message}", e)
                Toast.makeText(this@StudentDetailActivity, "Network error.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun removeStudentAssociation(studentId: Long, courseId: Long) {
        lifecycleScope.launch {
            try {
                val assocDto = CourseStudentAssocDto(idStudent = studentId, idCourse = courseId)
                Log.d(TAG, "Attempting to unenroll student $studentId from course $courseId")
                val response = RetrofitClient.apiService.removeStudentFromCourse(assocDto)
                if (response.isSuccessful) {
                    Toast.makeText(this@StudentDetailActivity, "Student unenrolled successfully.", Toast.LENGTH_SHORT).show()
                    fetchStudentDetails(studentId) // Refresh details
                } else {
                    Log.e(TAG, "Failed to unenroll student. Code: ${response.code()}, Msg: ${response.errorBody()?.string()}, Status: ${response.body()?.status}")
                    Toast.makeText(this@StudentDetailActivity, "Error unenrolling student.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception unenrolling student: ${e.message}", e)
                Toast.makeText(this@StudentDetailActivity, "Network error.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // --- Menu for Edit/Delete Student ---
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_student_detail, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // ... (Edit and Delete student logic remains the same as Teacher/Course detail)
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            R.id.action_edit_student -> {
                currentStudent?.let { studentToEdit ->
                    val intent = Intent(this, AddEditStudentActivity::class.java)
                    intent.putExtra(AddEditStudentActivity.EXTRA_EDIT_STUDENT, studentToEdit)
                    editStudentResultLauncher.launch(intent)
                }
                true
            }
            R.id.action_delete_student -> {
                currentStudent?.let { showDeleteConfirmationDialog(it) } // Existing delete logic
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // Existing Delete Student Logic
    private fun showDeleteConfirmationDialog(student: StudentDto) {
        AlertDialog.Builder(this)
            .setTitle("Delete Student")
            .setMessage("Are you sure you want to delete ${student.firstName} ${student.lastName}?")
            .setPositiveButton("Delete") { _, _ -> performDeleteStudent(student) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performDeleteStudent(student: StudentDto) {
        val studentId = student.id ?: return
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.deleteStudent(LongIdDto(studentId))
                if (response.isSuccessful && response.body() != null) {
                    val serverStatuses = response.body()!!.status
                    if (serverStatuses?.any { it.code == ServerErrorEnum.DELETE_STUDENT_NOT_ALLOWED } == true) {
                        val errorMessage = serverStatuses.find { it.code == ServerErrorEnum.DELETE_STUDENT_NOT_ALLOWED }?.message ?: "Cannot delete student."
                        Toast.makeText(this@StudentDetailActivity, errorMessage, Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this@StudentDetailActivity, "Student deleted.", Toast.LENGTH_SHORT).show()
                        setResult(Activity.RESULT_OK)
                        finish()
                    }
                } else {
                    Toast.makeText(this@StudentDetailActivity, "Error deleting student: ${response.errorBody()?.string()}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@StudentDetailActivity, "Network error deleting student.", Toast.LENGTH_LONG).show()
            }
        }
    }
}