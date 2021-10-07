package com.mitchelltsutsulis.tube_loader

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import com.mitchelltsutsulis.tube_loader.fragment.SearchResultFragment

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val searchField = findViewById<EditText>(R.id.search_field)

        searchField.setOnEditorActionListener { view, actionId, event ->
            return@setOnEditorActionListener when (actionId) {
                EditorInfo.IME_ACTION_SEARCH -> {
                    val newFragmentBundle = Bundle()
                    val newSearchResultFragment =
                        SearchResultFragment()

                    newFragmentBundle.putString("searchString", searchField.text.toString())
                    newSearchResultFragment.arguments = newFragmentBundle

                    supportFragmentManager
                        .beginTransaction()
                        .replace(R.id.search_result_fragment_container, newSearchResultFragment)
                        .commit()

                    true
                }
                else -> false
            }
        }
    }
}