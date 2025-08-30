package com.example.polisapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.polisapp.dto.StudentDto

class StudentAdapter(
    private var students: MutableList<StudentDto>,
    private val onItemClicked: (StudentDto) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var isLoadingAdded = false

    companion object {
        private const val ITEM_VIEW_TYPE_STUDENT = 0
        private const val ITEM_VIEW_TYPE_LOADING = 1
    }

    class StudentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.textViewStudentName)
        private val serialTextView: TextView = itemView.findViewById(R.id.textViewStudentSerialNumber)
        private val courseTextView: TextView = itemView.findViewById(R.id.textViewStudentCourse)

        fun bind(student: StudentDto, onItemClicked: (StudentDto) -> Unit) {
            nameTextView.text = "${student.firstName ?: ""} ${student.lastName ?: ""}".trim().ifEmpty{"Unknown Student"}
            serialTextView.text = "Serial: ${student.serialNumber ?: "N/A"}"

            val courseTitle = student.course?.title ?: "Not Enrolled"
            val courseCode = student.course?.code
            val courseInfo = if (courseCode != null) "$courseCode - $courseTitle" else courseTitle

            courseTextView.text = "Course: $courseInfo"
            courseTextView.visibility = if (student.course == null) View.GONE else View.VISIBLE


            itemView.setOnClickListener {
                onItemClicked(student)
            }
        }
    }

    class LoadingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // val progressBar: ProgressBar = itemView.findViewById(R.id.progressBarItemLoading) // from item_loading_footer.xml
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == ITEM_VIEW_TYPE_STUDENT) {
            val itemView = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_student, parent, false)
            StudentViewHolder(itemView)
        } else {
            val itemView = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_loading_footer, parent, false)
            LoadingViewHolder(itemView)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is StudentViewHolder) {
            holder.bind(students[position], onItemClicked)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == students.size - 1 && isLoadingAdded) {
            ITEM_VIEW_TYPE_LOADING
        } else {
            ITEM_VIEW_TYPE_STUDENT
        }
    }

    override fun getItemCount(): Int = students.size

    fun addLoadingFooter() {
        if (!isLoadingAdded) {
            isLoadingAdded = true
            students.add(StudentDto(null, "LOADING_PLACEHOLDER", null, null, null)) // Placeholder
            notifyItemInserted(students.size - 1)
        }
    }

    fun removeLoadingFooter() {
        if (isLoadingAdded) {
            isLoadingAdded = false
            if (students.isNotEmpty()) {
                val lastPosition = students.size - 1
                if (students[lastPosition].firstName == "LOADING_PLACEHOLDER") {
                    students.removeAt(lastPosition)
                    notifyItemRemoved(lastPosition)
                }
            }
        }
    }

    fun setData(newStudents: List<StudentDto>) {
        removeLoadingFooter()
        students.clear()
        students.addAll(newStudents)
        notifyDataSetChanged()
    }

    fun clear() {
        removeLoadingFooter()
        students.clear()
        notifyDataSetChanged()
    }
}