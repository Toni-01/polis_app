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

class CourseDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_COURSE = "extra_course"
        private const val TAG = "CourseDetailActivity"
    }

    private var currentCourse: CourseDto? = null
    private var allTeachers: List<TeacherDto> = emptyList() // To store fetched teachers

    // Views
    private lateinit var textViewDetailCourseId: TextView
    private lateinit var textViewDetailCourseCode: TextView
    private lateinit var textViewDetailCourseTitle: TextView
    private lateinit var textViewDetailCourseDescription: TextView
    private lateinit var textViewDetailCourseYear: TextView
    private lateinit var textViewDetailCourseTeacher: TextView
    private lateinit var textViewDetailCourseStudents: TextView
    private lateinit var buttonManageTeacherAssociation: Button // New Button

    private val editCourseResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val updatedCourseData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    result.data?.getSerializableExtra(AddEditCourseActivity.EXTRA_REPLY_COURSE, CourseDto::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    result.data?.getSerializableExtra(AddEditCourseActivity.EXTRA_REPLY_COURSE) as? CourseDto
                }
                // For robust update, re-fetch the course from server as AddEditCourseActivity might only return basic fields
                // or if the server DTO from upsertCourse is comprehensive enough, use that.
                // For now, let's assume we need to re-fetch for full details if teacher association was changed there.
                currentCourse?.id?.let { fetchCourseDetails(it) } // Re-fetch to get latest state
                setResult(Activity.RESULT_OK)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_course_detail)

        initializeViews()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val courseFromIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra(EXTRA_COURSE, CourseDto::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra(EXTRA_COURSE) as? CourseDto
        }

        if (courseFromIntent?.id != null) {
            fetchCourseDetails(courseFromIntent.id!!) // Fetch fresh details
            fetchAllTeachersForSelection() // Fetch teachers for dialog
        } else {
            Toast.makeText(this, "Error: Course ID not found.", Toast.LENGTH_LONG).show()
            finish()
        }

        buttonManageTeacherAssociation.setOnClickListener {
            handleManageTeacherAssociation()
        }
    }

    private fun initializeViews() {
        textViewDetailCourseId = findViewById(R.id.textViewDetailCourseId)
        textViewDetailCourseCode = findViewById(R.id.textViewDetailCourseCode)
        textViewDetailCourseTitle = findViewById(R.id.textViewDetailCourseTitle)
        textViewDetailCourseDescription = findViewById(R.id.textViewDetailCourseDescription)
        textViewDetailCourseYear = findViewById(R.id.textViewDetailCourseYear)
        textViewDetailCourseTeacher = findViewById(R.id.textViewDetailCourseTeacher)
        textViewDetailCourseStudents = findViewById(R.id.textViewDetailCourseStudents)
        buttonManageTeacherAssociation = findViewById(R.id.buttonManageTeacherAssociation)
    }

    private fun fetchCourseDetails(courseId: Long) {
        lifecycleScope.launch {
            try {
                Log.d(TAG, "Fetching details for course ID: $courseId")
                val response = RetrofitClient.apiService.getCourse(LongIdDto(courseId))
                if (response.isSuccessful && response.body()?.data != null) {
                    currentCourse = response.body()!!.data!!
                    populateCourseDetails(currentCourse!!)
                    setResult(Activity.RESULT_OK) // Indicate data might have changed for MainActivity
                } else {
                    Log.e(TAG, "Failed to fetch course details. Code: ${response.code()}, Msg: ${response.errorBody()?.string()}")
                    Toast.makeText(this@CourseDetailActivity, "Error fetching course details.", Toast.LENGTH_SHORT).show()
                    // Optionally finish if details can't be loaded
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception fetching course details: ${e.message}", e)
                Toast.makeText(this@CourseDetailActivity, "Network error.", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun populateCourseDetails(course: CourseDto) {
        supportActionBar?.title = course.title ?: "Course Details"
        textViewDetailCourseId.text = course.id?.toString() ?: "N/A"
        textViewDetailCourseCode.text = course.code ?: "N/A"
        textViewDetailCourseTitle.text = course.title ?: "N/A"
        textViewDetailCourseDescription.text = course.description.takeIf { !it.isNullOrBlank() } ?: "No description."
        textViewDetailCourseYear.text = course.year?.toString() ?: "N/A"

        updateTeacherAssociationUI(course.teacher)

        val studentNames = course.students?.joinToString("\n") { // Changed to newline for better readability
            "- ${it.firstName ?: ""} ${it.lastName ?: ""}".trim()
        }
        textViewDetailCourseStudents.text = if (!studentNames.isNullOrBlank()) studentNames else "No students enrolled."
    }

    private fun updateTeacherAssociationUI(teacher: TeacherDto?) {
        if (teacher != null) {
            val teacherInfo = "${teacher.firstName ?: ""} ${teacher.lastName ?: ""}".trim()
            textViewDetailCourseTeacher.text = if (teacherInfo.isNotBlank()) teacherInfo else "Unknown Teacher"
            buttonManageTeacherAssociation.text = "Change/Unassign Teacher"
        } else {
            textViewDetailCourseTeacher.text = "Not assigned"
            buttonManageTeacherAssociation.text = "Assign Teacher"
        }
    }

    private fun fetchAllTeachersForSelection() {
        lifecycleScope.launch {
            try {
                val pagination = Pagination(pageNumber = 0, pageSize = 200, sort = null) // Fetch a good number
                val filterDto = SimpleStringFilterDto(filter = null, pagination = pagination)
                val response = RetrofitClient.apiService.filterTeachers(filterDto)
                if (response.isSuccessful && response.body()?.slice?.content != null) {
                    allTeachers = response.body()!!.slice!!.content!!
                } else {
                    Log.e(TAG, "Failed to fetch all teachers for selection.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception fetching all teachers: ${e.message}", e)
            }
        }
    }

    private fun handleManageTeacherAssociation() {
        if (currentCourse == null) return

        if (currentCourse?.teacher != null) {
            // Options: Change or Unassign
            AlertDialog.Builder(this)
                .setTitle("Manage Teacher")
                .setItems(arrayOf("Change Teacher", "Unassign Teacher")) { dialog, which ->
                    when (which) {
                        0 -> showTeacherSelectionDialog("Change Teacher") // Change
                        1 -> confirmUnassignTeacher() // Unassign
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        } else {
            // Option: Assign
            showTeacherSelectionDialog("Assign Teacher")
        }
    }

    private fun showTeacherSelectionDialog(title: String) {
        if (allTeachers.isEmpty()) {
            Toast.makeText(this, "No teachers available to assign.", Toast.LENGTH_SHORT).show()
            fetchAllTeachersForSelection() // Try to refresh teacher list
            return
        }

        val teacherDisplayNames = allTeachers.map { "${it.firstName ?: ""} ${it.lastName ?: ""} (ID: ${it.id})" }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle(title)
            .setItems(teacherDisplayNames) { dialog, which ->
                val selectedTeacher = allTeachers[which]
                currentCourse?.id?.let { courseId ->
                    associateTeacher(courseId, selectedTeacher.id!!)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmUnassignTeacher() {
        val teacherToUnassign = currentCourse?.teacher ?: return
        val courseId = currentCourse?.id ?: return

        AlertDialog.Builder(this)
            .setTitle("Unassign Teacher")
            .setMessage("Are you sure you want to unassign ${teacherToUnassign.firstName} ${teacherToUnassign.lastName} from this course?")
            .setPositiveButton("Unassign") { _, _ ->
                removeTeacherAssociation(courseId, teacherToUnassign.id!!)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }


    private fun associateTeacher(courseId: Long, teacherId: Long) {
        lifecycleScope.launch {
            try {
                val assocDto = CourseTeacherAssocDto(idTeacher = teacherId, idCourse = courseId)
                val response = RetrofitClient.apiService.associateTeacherToCourse(assocDto)
                if (response.isSuccessful) {
                    Toast.makeText(this@CourseDetailActivity, "Teacher associated successfully.", Toast.LENGTH_SHORT).show()
                    fetchCourseDetails(courseId) // Refresh details
                } else {
                    Log.e(TAG, "Failed to associate teacher. Code: ${response.code()}, Msg: ${response.errorBody()?.string()}")
                    Toast.makeText(this@CourseDetailActivity, "Error associating teacher.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception associating teacher: ${e.message}", e)
                Toast.makeText(this@CourseDetailActivity, "Network error.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun removeTeacherAssociation(courseId: Long, teacherId: Long) {
        lifecycleScope.launch {
            try {
                val assocDto = CourseTeacherAssocDto(idTeacher = teacherId, idCourse = courseId)
                val response = RetrofitClient.apiService.removeTeacherFromCourse(assocDto)
                if (response.isSuccessful) {
                    Toast.makeText(this@CourseDetailActivity, "Teacher unassigned successfully.", Toast.LENGTH_SHORT).show()
                    fetchCourseDetails(courseId) // Refresh details
                } else {
                    Log.e(TAG, "Failed to unassign teacher. Code: ${response.code()}, Msg: ${response.errorBody()?.string()}")
                    Toast.makeText(this@CourseDetailActivity, "Error unassigning teacher.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception unassigning teacher: ${e.message}", e)
                Toast.makeText(this@CourseDetailActivity, "Network error.", Toast.LENGTH_SHORT).show()
            }
        }
    }


    // --- Menu for Edit/Delete Course ---
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_course_detail, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            R.id.action_edit_course -> {
                currentCourse?.let { courseToEdit ->
                    val intent = Intent(this, AddEditCourseActivity::class.java)
                    intent.putExtra(AddEditCourseActivity.EXTRA_EDIT_COURSE, courseToEdit)
                    editCourseResultLauncher.launch(intent)
                }
                true
            }
            R.id.action_delete_course -> {
                currentCourse?.let { showDeleteConfirmationDialog(it) } // Existing delete logic
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // Existing Delete Course Logic (keep as is or integrate if needed)
    private fun showDeleteConfirmationDialog(course: CourseDto) {
        AlertDialog.Builder(this)
            .setTitle("Delete Course")
            .setMessage("Are you sure you want to delete course: ${course.title}?")
            .setPositiveButton("Delete") { _, _ -> performDeleteCourse(course) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performDeleteCourse(course: CourseDto) {
        val courseId = course.id ?: return // Ensure ID exists
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.deleteCourse(LongIdDto(courseId))
                if (response.isSuccessful && response.body() != null) {
                    val serverStatuses = response.body()!!.status
                    if (serverStatuses?.any { it.code == ServerErrorEnum.DELETE_COURSE_NOT_ALLOWED } == true) {
                        val errorMessage = serverStatuses.find { it.code == ServerErrorEnum.DELETE_COURSE_NOT_ALLOWED }?.message ?: "Cannot delete course due to relations."
                        Toast.makeText(this@CourseDetailActivity, errorMessage, Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this@CourseDetailActivity, "Course deleted.", Toast.LENGTH_SHORT).show()
                        setResult(Activity.RESULT_OK)
                        finish()
                    }
                } else {
                    Toast.makeText(this@CourseDetailActivity, "Error deleting course: ${response.errorBody()?.string()}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@CourseDetailActivity, "Network error deleting course.", Toast.LENGTH_LONG).show()
            }
        }
    }
}