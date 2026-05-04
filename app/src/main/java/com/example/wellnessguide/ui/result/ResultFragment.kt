package com.example.wellnessguide.ui.result

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.wellnessguide.MainActivity
import com.example.wellnessguide.R

class ResultFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_result, container, false)

        view.findViewById<TextView>(R.id.btnMenuResult).setOnClickListener {
            (requireActivity() as MainActivity).openDrawer()
        }

        val resultText = arguments?.getString("resultText") ?: "No result available."
        view.findViewById<TextView>(R.id.tvResult).text = resultText

        view.findViewById<Button>(R.id.btnBackHome).setOnClickListener {
            findNavController().navigate(R.id.homeFragment)
        }

        return view
    }
}