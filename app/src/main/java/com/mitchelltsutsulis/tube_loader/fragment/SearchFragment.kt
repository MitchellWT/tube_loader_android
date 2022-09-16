package com.mitchelltsutsulis.tube_loader.fragment

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.fragment.app.Fragment
import com.mitchelltsutsulis.tube_loader.R

class SearchFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.search_fragment, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        try {
            val searchField = view.findViewById<EditText>(R.id.search_field)
            searchField.setOnEditorActionListener { _, actionId, _ ->
                return@setOnEditorActionListener when (actionId) {
                    EditorInfo.IME_ACTION_SEARCH -> {
                        val fragmentBundle = Bundle()
                        val searchResultFragment = SearchResultFragment()
                        val inputMethodManager =
                            requireActivity().getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
                        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
                        fragmentBundle.putString("searchString", searchField.text.toString())
                        searchResultFragment.arguments = fragmentBundle
                        requireActivity().supportFragmentManager
                            .beginTransaction()
                            .replace(R.id.search_result_fragment_container, searchResultFragment)
                            .commit()
                        true
                    }
                    else -> false
                }
            }
        } catch (e: Exception) {
            Log.i("EXCEPTION", e.message.toString())
        }
    }

    override fun onStop() {
        requireView().findViewById<EditText>(R.id.search_field).text.clear()
        super.onStop()
    }
}