package com.example.app1.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.app1.R
import com.example.app1.data.model.JournalEntry
import com.example.app1.ui.adapters.JournalAdapter
import com.example.app1.utils.PreferencesHelper
import com.example.app1.viewmodel.JournalEntryViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MyJournalsFragment : Fragment() {

    private val journalViewModel: JournalEntryViewModel by viewModels()
    private lateinit var journalAdapter: JournalAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_my_journals, container, false)

        // Configurar RecyclerView
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(context)
        journalAdapter = JournalAdapter(listOf())  // Iniciar con una lista vacía
        recyclerView.adapter = journalAdapter

        // Obtener el userId del usuario actual
        val userId = PreferencesHelper.getUserId(requireContext())

        // Observar los journals y actualizar la UI
        userId?.let {
            journalViewModel.getUserJournals(it).observe(viewLifecycleOwner) { journals ->
                journalAdapter.updateJournals(journals)
            }
        }

        return view
    }
}