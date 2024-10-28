package com.example.app1.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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

        // Configura el adaptador pasando la función `onPublishDraft`
        journalAdapter = JournalAdapter(listOf()) { draft ->
            journalViewModel.publishJournalEntry(draft)  // Publicar borrador individual
        }
        recyclerView.adapter = journalAdapter

        // Obtener el userId del usuario logueado desde PreferencesHelper
        val userId = PreferencesHelper.getUserId(requireContext()) ?: return view

        // Observar los journals y actualizar la UI
        journalViewModel.getUserJournals(userId).observe(viewLifecycleOwner) { journals ->
            journalAdapter.updateJournals(journals)
        }

        // Observar el estado de publicación para notificar al usuario
        journalViewModel.createJournalEntryLiveData.observe(viewLifecycleOwner) { response ->
            if (response.isSuccessful) {
                Toast.makeText(requireContext(), "Borrador publicado con éxito", Toast.LENGTH_SHORT).show()
                // Recargar la lista de journals después de la publicación
                journalViewModel.getUserJournals(userId).observe(viewLifecycleOwner) { journals ->
                    journalAdapter.updateJournals(journals)
                }
            } else {
                Toast.makeText(requireContext(), "Error al publicar borrador: ${response.message()}", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }
}