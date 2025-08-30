package com.example.polisapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.polisapp.dto.TeacherDto

class TeacherAdapter(
    private var teachers: MutableList<TeacherDto>, // Changed to MutableList
    private val onItemClicked: (TeacherDto) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() { // Changed to RecyclerView.ViewHolder

    private var isLoadingAdded = false // To track if loading footer is shown

    companion object {
        private const val ITEM_VIEW_TYPE_TEACHER = 0
        private const val ITEM_VIEW_TYPE_LOADING = 1
    }

    // ViewHolder for Teacher items
    class TeacherViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.textViewTeacherName)
        val titleTextView: TextView = itemView.findViewById(R.id.textViewTeacherTitle)

        fun bind(teacher: TeacherDto, onItemClicked: (TeacherDto) -> Unit) {
            nameTextView.text = "${teacher.firstName ?: ""} ${teacher.lastName ?: ""}".trim()
                .ifEmpty { "Unknown Teacher" }
            titleTextView.text = teacher.title ?: "N/A"
            itemView.setOnClickListener {
                onItemClicked(teacher)
            }
        }
    }

    // ViewHolder for Loading item
    class LoadingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val progressBar: ProgressBar = itemView.findViewById(R.id.progressBarItemLoading) // Ensure this ID exists in the new layout
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == ITEM_VIEW_TYPE_TEACHER) {
            val itemView = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_teacher, parent, false)
            TeacherViewHolder(itemView)
        } else { // ITEM_VIEW_TYPE_LOADING
            val itemView = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_loading_footer, parent, false) // Create this new layout
            LoadingViewHolder(itemView)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is TeacherViewHolder) {
            val currentTeacher = teachers[position]
            holder.bind(currentTeacher, onItemClicked)
        } else if (holder is LoadingViewHolder) {
            // Nothing specific to bind for the loading indicator, it's just a ProgressBar
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == teachers.size - 1 && isLoadingAdded) {
            ITEM_VIEW_TYPE_LOADING
        } else {
            ITEM_VIEW_TYPE_TEACHER
        }
    }

    override fun getItemCount(): Int {
        return teachers.size
    }

    // --- Helper methods for pagination ---

    fun addLoadingFooter() {
        if (!isLoadingAdded) {
            isLoadingAdded = true
            // Add a dummy null or a special object to represent the loading item
            // For simplicity with TeacherDto, we might not add a real item,
            // but rely on getItemViewType and adjust itemCount if we did.
            // A common way is to add a null and handle it in onBind.
            // For now, let's just use the flag to change view type of last item.
            // A more robust way is to add a null item to teachers list and remove it.
            // Let's try adding a placeholder:
            teachers.add(TeacherDto(null, null, null, null, null)) // Placeholder
            notifyItemInserted(teachers.size - 1)
        }
    }

    fun removeLoadingFooter() {
        if (isLoadingAdded) {
            isLoadingAdded = false
            if (teachers.isNotEmpty()) {
                val position = teachers.size - 1
                // Check if the last item is the placeholder
                if (teachers[position].id == null && teachers[position].firstName == null) { // our placeholder condition
                    teachers.removeAt(position)
                    notifyItemRemoved(position)
                }
            }
        }
    }

    fun addAll(newTeachers: List<TeacherDto>) {
        val oldSize = teachers.size
        teachers.addAll(newTeachers)
        notifyItemRangeInserted(oldSize, newTeachers.size)
    }

    fun clear() {
        isLoadingAdded = false // Reset loading footer state
        teachers.clear()
        notifyDataSetChanged()
    }

    // Replaces updateTeachers for full list replacement (e.g., new search)
    fun setData(newTeachers: List<TeacherDto>) {
        this.teachers.clear()
        this.teachers.addAll(newTeachers)
        notifyDataSetChanged() // This is okay for now, DiffUtil is better for performance
    }
}