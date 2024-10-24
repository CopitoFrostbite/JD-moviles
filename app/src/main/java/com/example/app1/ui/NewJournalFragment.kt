package com.example.app1.ui

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.example.app1.R
import com.example.app1.data.model.JournalEntry
import com.example.app1.viewmodel.JournalEntryViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class NewJournalFragment : Fragment() {

    private val journalEntryViewModel: JournalEntryViewModel by viewModels()
    private var selectedEmotion: String? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_new_journal, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        var userId = arguments?.getString("USER_ID")
        ViewCompat.setOnApplyWindowInsetsListener(view.findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val spinnerEmotion = view.findViewById<Spinner>(R.id.spinnerEmotion)
        val emotions = arrayOf("Triste", "Ira", "Sorpresa", "Miedo", "Feliz", "Inconforme")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, emotions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerEmotion.adapter = adapter

        val etJournalTitle = view.findViewById<EditText>(R.id.etJournalTitle)
        val etJournalContent = view.findViewById<EditText>(R.id.etJournalContent)
        val btnSaveJournal = view.findViewById<Button>(R.id.btnSaveJournal)

        btnSaveJournal.setOnClickListener {
            val title = etJournalTitle.text.toString().trim()
            val content = etJournalContent.text.toString().trim()
            val emotion = spinnerEmotion.selectedItem.toString()

            if (title.isNotEmpty() && content.isNotEmpty()) {
                createJournalEntry(title, content, emotion)
            } else {
                // Manejar el caso donde los campos están vacíos
            }
        }

        journalEntryViewModel.createJournalEntryLiveData.observe(viewLifecycleOwner, Observer { response ->
            if (response.isSuccessful) {
                // Manejar la creación exitosa de la entrada de diario
            } else {
                // Manejar el error en la creación de la entrada de diario
            }
        })
    }

    private fun createJournalEntry(title: String, content: String, emotion: String) {
        val userId = "user_id_example" // Deberías obtener el userId del usuario logueado
        val date = System.currentTimeMillis()
        val isEdited = false

        journalEntryViewModel.createJournalEntry(userId, title, content, date, isEdited)
    }
}