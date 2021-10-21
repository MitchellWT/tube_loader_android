package com.mitchelltsutsulis.tube_loader

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.mitchelltsutsulis.tube_loader.fragment.DownloadedFragment
import com.mitchelltsutsulis.tube_loader.fragment.QueueFragment

class MainActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Get BNV and instantiate fragments
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation_bar)
        val searchFragment       = SearchFragment()
        val queueFragment        = QueueFragment()
        val downloadedFragment   = DownloadedFragment()
        // Set search as our initial fragment
        setFrame(searchFragment)
        // Connect fragments to BNV buttons/items
        bottomNavigationView.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.bnv_search     -> setFrame(searchFragment)
                R.id.bnv_queue      -> setFrame(queueFragment)
                R.id.bnv_downloaded -> setFrame(downloadedFragment)
            }

            true
        }
    }
    // Set fragment for activity
    private fun setFrame(fragment: Fragment) {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.application_frame, fragment)
            .commit()
    }
}