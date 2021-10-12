package com.mitchelltsutsulis.tube_loader

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.fragment.app.Fragment
import com.mitchelltsutsulis.tube_loader.fragment.SearchResultFragment

class SearchFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.search_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val searchField = view.findViewById<EditText>(R.id.search_field)

        searchField?.setOnEditorActionListener { _, actionId, _ ->
            return@setOnEditorActionListener when (actionId) {
                EditorInfo.IME_ACTION_SEARCH -> {
                    val newFragmentBundle = Bundle()
                    val newSearchResultFragment =
                        SearchResultFragment()

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
}