package com.mitchelltsutsulis.tube_loader

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.mitchelltsutsulis.tube_loader.fragment.DownloadedFragment
import com.mitchelltsutsulis.tube_loader.fragment.QueueFragment
import com.mitchelltsutsulis.tube_loader.fragment.SearchFragment

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val authToken = (application as App).getAuthToken()
        if (authToken.isEmpty()) {
            val loginIntent = Intent(this, LoginActivity::class.java)
            startActivity(loginIntent)
        }
        setContentView(R.layout.activity_main)

        try {
            val bottomNavigationView =
                findViewById<BottomNavigationView>(R.id.bottom_navigation_bar)
            val searchFragment = SearchFragment()
            val queueFragment = QueueFragment()
            val downloadedFragment = DownloadedFragment()

            setFrame(searchFragment)
            bottomNavigationView.setOnItemSelectedListener {
                when (it.itemId) {
                    R.id.bnv_search -> setFrame(searchFragment)
                    R.id.bnv_queue -> setFrame(queueFragment)
                    R.id.bnv_downloaded -> setFrame(downloadedFragment)
                    R.id.bnv_logout -> logout()
                }
                true
            }
        } catch (e: Exception) {
            Log.i("EXCEPTION", e.message.toString())
        }
    }

    private fun setFrame(fragment: Fragment) = supportFragmentManager
        .beginTransaction()
        .replace(R.id.application_frame, fragment)
        .commit()

    private fun logout() {
        (application as App).deleteData()
        val loginIntent = Intent(this, LoginActivity::class.java)
        startActivity(loginIntent)
    }
}