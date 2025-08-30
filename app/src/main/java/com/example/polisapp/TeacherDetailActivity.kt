package com.example.polisapp // Your package

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog // Import AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope // Import lifecycleScope
import com.example.polisapp.dto.LongIdDto
import com.example.polisapp.dto.TeacherDto
import com.example.polisapp.network.RetrofitClient // Import RetrofitClient
import kotlinx.coroutines.launch // Import launch

class TeacherDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TEACHER = "extra_teacher"
        private const val TAG = "TeacherDetailActivity" // Add a TAG for logging
    }

    private var currentTeacher: TeacherDto? = null

    private val editTeacherResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                Log.d(TAG, "Returned from Edit with RESULT_OK.")
                val updatedTeacher = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    result.data?.getSerializableExtra(AddEditTeacherActivity.EXTRA_REPLY_TEACHER, TeacherDto::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    result.data?.getSerializableExtra(AddEditTeacherActivity.EXTRA_REPLY_TEACHER) as? TeacherDto
                }

                if (updatedTeacher != null) {
                    this.currentTeacher = updatedTeacher
                    populateTeacherDetails(updatedTeacher)
                    // Signal MainActivity that data might have changed.
                    // MainActivity's viewTeacherResultLauncher will catch this.
                    setResult(Activity.RESULT_OK)
                }
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_teacher_detail)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val teacher = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra(EXTRA_TEACHER, TeacherDto::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra(EXTRA_TEACHER) as? TeacherDto
        }

        if (teacher != null) {
            currentTeacher = teacher
            populateTeacherDetails(teacher)
        } else {
            findViewById<TextView>(R.id.textViewDetailTeacherId).text = "Error: Teacher data not found."
            Toast.makeText(this, "Teacher data not found.", Toast.LENGTH_LONG).show() // Added Toast
            finish() // Finish if no teacher data
        }
    }

    private fun populateTeacherDetails(teacher: TeacherDto) {
        supportActionBar?.title = "${teacher.firstName} ${teacher.lastName}"
        findViewById<TextView>(R.id.textViewDetailTeacherId).text = teacher.id?.toString() ?: "N/A"
        findViewById<TextView>(R.id.textViewDetailFirstName).text = teacher.firstName ?: "N/A"
        findViewById<TextView>(R.id.textViewDetailLastName).text = teacher.lastName ?: "N/A"
        findViewById<TextView>(R.id.textViewDetailTitle).text = teacher.title ?: "N/A"
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_teacher_detail, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed() // Or finish()
                true
            }
            R.id.action_edit_teacher -> {
                currentTeacher?.let { teacherToEdit ->
                    val intent = Intent(this, AddEditTeacherActivity::class.java)
                    intent.putExtra(AddEditTeacherActivity.EXTRA_EDIT_TEACHER, teacherToEdit)
                    editTeacherResultLauncher.launch(intent)
                }
                true
            }
            R.id.action_delete_teacher -> { // Handle delete
                currentTeacher?.let { showDeleteConfirmationDialog(it) }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showDeleteConfirmationDialog(teacher: TeacherDto) {
        AlertDialog.Builder(this)
            .setTitle("Delete Teacher")
            .setMessage("Are you sure you want to delete ${teacher.firstName ?: ""} ${teacher.lastName ?: ""}?")
            .setPositiveButton("Delete") { dialog, which ->
                performDeleteTeacher(teacher)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performDeleteTeacher(teacher: TeacherDto) {
        val teacherId = teacher.id
        if (teacherId == null) {
            Toast.makeText(this, "Teacher ID is missing, cannot delete.", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                Log.d(TAG, "Attempting to delete teacher with ID: $teacherId")
                val response = RetrofitClient.apiService.deleteTeacher(LongIdDto(teacherId))

                if (response.isSuccessful && response.body() != null) {
                    val serverStatuses = response.body()!!.status
                    if (serverStatuses != null && serverStatuses.any { it.code == com.example.polisapp.dto.ServerErrorEnum.DELETE_TEACHER_NOT_ALLOWED }) {
                        val errorMessage = serverStatuses.find { it.code == com.example.polisapp.dto.ServerErrorEnum.DELETE_TEACHER_NOT_ALLOWED }?.message
                            ?: "Cannot delete teacher due to existing relationships."
                        Log.w(TAG, "Server denied teacher deletion: $errorMessage")
                        Toast.makeText(this@TeacherDetailActivity, errorMessage, Toast.LENGTH_LONG).show()
                    } else if (serverStatuses.isNullOrEmpty() || serverStatuses.all {it.code == com.example.polisapp.dto.ServerErrorEnum.OK } ) {
                        Log.i(TAG, "Teacher deleted successfully from server.")
                        Toast.makeText(this@TeacherDetailActivity, "Teacher deleted successfully.", Toast.LENGTH_SHORT).show()
                        setResult(Activity.RESULT_OK)
                        finish()
                    } else {
                        val errorMessages = serverStatuses.joinToString("\n") { "${it.code?.name}: ${it.message}" } // Use .name for enum
                        Log.w(TAG, "Teacher deletion response with non-OK statuses: $errorMessages")
                        Toast.makeText(this@TeacherDetailActivity, "Could not delete teacher: \n$errorMessages", Toast.LENGTH_LONG).show()
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error during delete"
                    Log.e(TAG, "API Error during delete. Code: ${response.code()}, Body: $errorBody")
                    // Attempt to parse your custom error structure from errorBody if it's JSON
                    // For now, just show the raw body or a generic message.
                    Toast.makeText(this@TeacherDetailActivity, "Error deleting teacher (HTTP ${response.code()})", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Network/Exception during delete: ${e.message}", e)
                Toast.makeText(this@TeacherDetailActivity, "Network error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}