
package com.example.polisapp

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar 
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.polisapp.dto.TeacherDto
import com.example.polisapp.viewmodel.AddEditTeacherViewModel
import com.example.polisapp.viewmodel.SaveResult 
import com.google.android.material.textfield.TextInputEditText

class AddEditTeacherActivity : AppCompatActivity() {

    private lateinit var editTextFirstName: TextInputEditText
    private lateinit var editTextLastName: TextInputEditText
    private lateinit var editTextTitle: TextInputEditText
    private lateinit var buttonSaveTeacher: Button
    

    private lateinit var viewModel: AddEditTeacherViewModel

    companion object {
        private const val TAG = "AddEditTeacherActivity"
        const val EXTRA_EDIT_TEACHER = "extra_edit_teacher"
        const val EXTRA_REPLY_TEACHER = "extra_reply_teacher"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_edit_teacher) 

        viewModel = ViewModelProvider(this).get(AddEditTeacherViewModel::class.java)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        editTextFirstName = findViewById(R.id.editTextFirstName)
        editTextLastName = findViewById(R.id.editTextLastName)
        editTextTitle = findViewById(R.id.editTextTitle)
        buttonSaveTeacher = findViewById(R.id.buttonSaveTeacher)
        

        val teacherToEdit = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra(EXTRA_EDIT_TEACHER, TeacherDto::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra(EXTRA_EDIT_TEACHER) as? TeacherDto
        }

        if (teacherToEdit != null) {
            supportActionBar?.title = "Edit Teacher"
            viewModel.setExistingTeacher(teacherToEdit) 
            editTextFirstName.setText(teacherToEdit.firstName)
            editTextLastName.setText(teacherToEdit.lastName)
            editTextTitle.setText(teacherToEdit.title)
        } else {
            supportActionBar?.title = "Add New Teacher"
            viewModel.setExistingTeacher(null) 
        }

        buttonSaveTeacher.setOnClickListener {
            val firstName = editTextFirstName.text.toString()
            val lastName = editTextLastName.text.toString()
            val title = editTextTitle.text.toString()
            viewModel.saveTeacher(firstName, lastName, title)
        }

        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.saveResult.observe(this) { result ->
            when (result) {
                is SaveResult.Loading -> {
                    
                    buttonSaveTeacher.isEnabled = false
                    
                    Log.d(TAG, "Saving teacher... Loading state.")
                }
                is SaveResult.Success -> {
                    
                    buttonSaveTeacher.isEnabled = true
                    
                    Toast.makeText(this, "Teacher saved!", Toast.LENGTH_SHORT).show()
                    Log.i(TAG, "Teacher saved successfully: ${result.teacher}")

                    val resultIntent = Intent()
                    resultIntent.putExtra(EXTRA_REPLY_TEACHER, result.teacher)
                    setResult(Activity.RESULT_OK, resultIntent)
                    finish()
                    viewModel.onSaveResultConsumed() 
                }
                is SaveResult.Error -> {
                    
                    buttonSaveTeacher.isEnabled = true
                    
                    val errorMessage = result.messages.joinToString("\n")
                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                    Log.e(TAG, "Error saving teacher: $errorMessage")
                    viewModel.onSaveResultConsumed() 
                }
                is SaveResult.Idle -> {
                    buttonSaveTeacher.isEnabled = true
                    
                    Log.d(TAG, "Save state is Idle.")
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        
        if (viewModel.saveResult.value is SaveResult.Loading) {
            Toast.makeText(this, "Save in progress...", Toast.LENGTH_SHORT).show()
            return true 
        }
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}