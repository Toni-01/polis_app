package com.example.polisapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.polisapp.fragments.CoursesFragment
import com.example.polisapp.fragments.StudentsFragment
import com.example.polisapp.fragments.TeachersFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) // This layout now has FragmentContainerView and BottomNavigationView

        bottomNavigationView = findViewById(R.id.bottom_navigation_view)

        // Set default fragment
        if (savedInstanceState == null) { // Load default only on initial create
            loadFragment(TeachersFragment()) // Load TeachersFragment by default
            bottomNavigationView.selectedItemId = R.id.nav_teachers // Highlight default item
        }

        bottomNavigationView.setOnItemSelectedListener { item ->
            var selectedFragment: Fragment? = null
            when (item.itemId) {
                R.id.nav_teachers -> {
                    selectedFragment = TeachersFragment()
                }
                R.id.nav_courses -> {
                    selectedFragment = CoursesFragment()
                }
                R.id.nav_students -> {
                    selectedFragment = StudentsFragment()
                }
            }
            if (selectedFragment != null) {
                loadFragment(selectedFragment)
            }
            true
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment_container, fragment)
            .commit()
    }

    // The onCreateOptionsMenu is now handled by each Fragment if setHasOptionsMenu(true) is called.
    // MainActivity itself might not need to inflate a general menu unless it has global actions.
}