package com.example.polisapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.polisapp.dto.CourseDto

class CourseAdapter(
    private var courses: MutableList<CourseDto>,
    private val onItemClicked: (CourseDto) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var isLoadingAdded = false

    companion object {
        private const val ITEM_VIEW_TYPE_COURSE = 0
        private const val ITEM_VIEW_TYPE_LOADING = 1
    }

    class CourseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.textViewCourseTitle)
        private val codeTextView: TextView = itemView.findViewById(R.id.textViewCourseCode)
        private val teacherTextView: TextView = itemView.findViewById(R.id.textViewCourseTeacher)

        fun bind(course: CourseDto, onItemClicked: (CourseDto) -> Unit) {
            titleTextView.text = course.title ?: "N/A"
            codeTextView.text = course.code ?: "N/A"

            val teacherName = if (course.teacher != null) {
                "${course.teacher.firstName ?: ""} ${course.teacher.lastName ?: ""}".trim()
            } else {
                "N/A"
            }
            teacherTextView.text = "Taught by: ${teacherName.ifEmpty { "N/A" }}"
            teacherTextView.visibility = if (teacherName.isEmpty() || teacherName == "N/A") View.GONE else View.VISIBLE

            itemView.setOnClickListener {
                onItemClicked(course)
            }
        }
    }

    class LoadingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // val progressBar: ProgressBar = itemView.findViewById(R.id.progressBarItemLoading) // From item_loading_footer.xml
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == ITEM_VIEW_TYPE_COURSE) {
            val itemView = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_course, parent, false)
            CourseViewHolder(itemView)
        } else {
            val itemView = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_loading_footer, parent, false)
            LoadingViewHolder(itemView)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is CourseViewHolder) {
            holder.bind(courses[position], onItemClicked)
        }
    }

    override fun getItemViewType(position: Int): Int {
        // Check if it's the last item and isLoadingAdded is true
        return if (position == courses.size - 1 && isLoadingAdded) {
            ITEM_VIEW_TYPE_LOADING
        } else {
            ITEM_VIEW_TYPE_COURSE
        }
    }

    override fun getItemCount(): Int = courses.size

    fun addLoadingFooter() {
        if (!isLoadingAdded) {
            isLoadingAdded = true
            // Add a dummy placeholder to trigger the loading view type for the last item
            courses.add(CourseDto(null, "LOADING_PLACEHOLDER", null, null, null, null, null))
            notifyItemInserted(courses.size - 1)
        }
    }

    fun removeLoadingFooter() {
        if (isLoadingAdded) {
            isLoadingAdded = false
            if (courses.isNotEmpty()) {
                val lastPosition = courses.size - 1
                // Check if the last item is our placeholder
                if (courses[lastPosition].code == "LOADING_PLACEHOLDER") {
                    courses.removeAt(lastPosition)
                    notifyItemRemoved(lastPosition)
                }
            }
        }
    }

    fun setData(newCourses: List<CourseDto>) {
        removeLoadingFooter() // Ensure footer is removed before setting new data
        courses.clear()
        courses.addAll(newCourses)
        notifyDataSetChanged()
    }

    fun clear() {
        removeLoadingFooter()
        courses.clear()
        notifyDataSetChanged()
    }
}