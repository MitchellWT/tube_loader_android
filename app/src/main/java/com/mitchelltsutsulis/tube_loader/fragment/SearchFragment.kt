package com.mitchelltsutsulis.tube_loader

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.fragment.app.Fragment
import com.mitchelltsutsulis.tube_loader.fragment.SearchResultFragment

class SearchFragment : Fragment() {
    private lateinit var searchField: EditText

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.search_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        searchField = view.findViewById(R.id.search_field)

        searchField?.setOnEditorActionListener { _, actionId, _ ->
            return@setOnEditorActionListener when (actionId) {
                EditorInfo.IME_ACTION_SEARCH -> {
                    val newFragmentBundle = Bundle()
                    val newSearchResultFragment =
                        SearchResultFragment()
                    val inputMethodManager = activity?.getSystemService(Activity.INPUT_METHOD_SERVICE)
                                             as InputMethodManager

                    inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
                    newFragmentBundle.putString("searchString", searchField.text.toString())
                    newSearchResultFragment.arguments = newFragmentBundle

                    activity?.supportFragmentManager
                        ?.beginTransaction()
                        ?.replace(R.id.search_result_fragment_container, newSearchResultFragment)
                        ?.commit()

                    true
                }
                else -> false
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        searchField.text.clear()
        super.onSaveInstanceState(outState)
    }
}