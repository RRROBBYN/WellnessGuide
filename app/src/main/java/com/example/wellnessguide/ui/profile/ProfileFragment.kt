package com.example.wellnessguide.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.wellnessguide.MainActivity
import com.example.wellnessguide.R

class ProfileFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        view.findViewById<TextView>(R.id.btnMenuProfile).setOnClickListener {
            (requireActivity() as MainActivity).openDrawer()
        }

        return view
    }
}