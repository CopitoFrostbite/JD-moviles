package com.example.app1.ui

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.example.app1.R
import com.example.app1.data.model.JournalEntry
import com.example.app1.data.remote.JournalApiService
import com.example.app1.utils.PreferencesHelper
import com.example.app1.viewmodel.JournalEntryViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

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

        // Obtenemos el userId, ya sea de los argumentos, preferencia compartida, o fuente de sesión
        val userId = PreferencesHelper.getUserId(requireContext()) ?: return

        ViewCompat.setOnApplyWindowInsetsListener(view.findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        //val btnPublishJournal = view.findViewById<Button>(R.id.btnPublishJournal)
        //btnPublishJournal.setOnClickListener {
        //    journalEntryViewModel.publishJournalEntry(journalId)  // Publicar el borrador
       // }

        val spinnerEmotion = view.findViewById<Spinner>(R.id.spinnerEmotion)
        val emotions = arrayOf("Triste", "Ira", "Sorpresa", "Miedo", "Feliz", "Inconforme")
        val moodMapping = mapOf("Triste" to 1, "Ira" to 2, "Sorpresa" to 3, "Miedo" to 4, "Feliz" to 5, "Inconforme" to 6)
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

            if (validateJournalInputs(title, content)) {
                val mood = moodMapping[emotion] ?: 1
                saveDraftJournalEntry(userId, title, content, mood)

                // Regresar a la actividad principal (MainActivity) o a otro fragmento
                navigateToJournalList()  // Navegación
            }
        }

        journalEntryViewModel.createJournalEntryLiveData.observe(viewLifecycleOwner, Observer { response ->
            if (response.isSuccessful) {
                Toast.makeText(requireContext(), "Entrada de diario creada con éxito", Toast.LENGTH_SHORT).show()
                navigateToMyJournals()  // Navega a MyJournalsFragment tras el éxito
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("NewJournalFragment", "Error al crear entrada: ${response.message()} - ${response.code()} - $errorBody")
                Toast.makeText(requireContext(), "Error al crear entrada de diario: ${response.message()} - $errorBody", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun saveDraftJournalEntry(userId: String, title: String, content: String, mood: Int) {
        val journalId = System.currentTimeMillis().toString()
        val date = System.currentTimeMillis()
        val isEdited = false

        // Crear el JournalEntry para el borrador
        val journalEntry = JournalEntry(
            journalId = journalId,
            userId = userId,
            title = title,
            content = content,
            mood = mood,
            date = date,
            isEdited = isEdited,
            isDraft = true
        )

        // Guardar borrador en el ViewModel
        journalEntryViewModel.saveDraftJournalEntry(journalEntry)
    }

    private fun navigateToJournalList() {
        parentFragmentManager.popBackStack()  // Regresa al fragmento anterior
    }

    private fun validateJournalInputs(title: String, content: String): Boolean {
        if (title.isBlank() || content.isBlank()) {
            Toast.makeText(requireContext(), "Título y contenido son obligatorios", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun createJournalEntry(userId: String, title: String, content: String, mood: Int) {
        val journalId = System.currentTimeMillis().toString()
        val date = System.currentTimeMillis()
        val isEdited = false

        // Crear la solicitud del journal
        val journalRequest = JournalApiService.JournalRequest(
            journalId = journalId,
            userId = userId,
            title = title,
            content = content,
            mood = mood,
            date = date,
            isEdited = isEdited
        )

        // Enviar la solicitud al ViewModel
        journalEntryViewModel.createJournalEntry(journalRequest)
    }

    private fun navigateToMyJournals() {
        parentFragmentManager.commit {
            replace(R.id.fragment_container, MyJournalsFragment())
            addToBackStack(null)
        }
    }
}
