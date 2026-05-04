package com.example.wellnessguide.ui.dynamic

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.wellnessguide.MainActivity
import com.example.wellnessguide.R

class DynamicDrawerFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_dynamic_drawer, container, false)

        val title = arguments?.getString("title") ?: "Page"
        val description = arguments?.getString("description") ?: "Content will appear here."

        view.findViewById<TextView>(R.id.txtDynamicTopTitle).text = title
        view.findViewById<TextView>(R.id.txtDynamicTitle).text = title
        view.findViewById<TextView>(R.id.txtDynamicDescription).text = description

        view.findViewById<TextView>(R.id.btnMenuDynamic).setOnClickListener {
            (requireActivity() as MainActivity).openDrawer()
        }

        return view
    }
}