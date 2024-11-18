package com.example.app1.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
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
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.app1.R
import com.example.app1.data.model.Image
import com.example.app1.data.model.JournalEntry
import com.example.app1.ui.adapters.ImagesAdapter
import com.example.app1.utils.FileUtils
import com.example.app1.utils.PreferencesHelper
import com.example.app1.utils.UiState
import com.example.app1.viewmodel.ImageViewModel
import com.example.app1.viewmodel.JournalEntryViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.util.UUID

@AndroidEntryPoint
class NewJournalFragment : Fragment() {

    private val journalEntryViewModel: JournalEntryViewModel by viewModels()
    private val selectedImages = mutableListOf<Uri>()
    private lateinit var imagesAdapter: ImagesAdapter
    private lateinit var selectImagesLauncher: ActivityResultLauncher<Intent>
    private val moodMapping = mapOf(
        "Triste" to 1, "Ira" to 2, "Sorpresa" to 3,
        "Miedo" to 4, "Feliz" to 5, "Inconforme" to 6
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_new_journal, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupImageSelector()
        setupRecyclerView(view)
        setupSpinner(view)
        setupSaveButton(view)
        setupObservers()
    }

    private fun setupImageSelector() {
        selectImagesLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                handleSelectedImages(result.data)
            }
        }
    }

    private fun setupRecyclerView(view: View) {
        val recyclerViewImages = view.findViewById<RecyclerView>(R.id.recyclerViewImages)
        imagesAdapter = ImagesAdapter(selectedImages)
        recyclerViewImages.apply {
            adapter = imagesAdapter
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        }
    }

    private fun setupSpinner(view: View) {
        val spinnerEmotion = view.findViewById<Spinner>(R.id.spinnerEmotion)
        val emotions = moodMapping.keys.toList()
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, emotions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerEmotion.adapter = adapter
    }

    private fun setupSaveButton(view: View) {
        val btnSaveJournal = view.findViewById<Button>(R.id.btnSaveJournal)
        btnSaveJournal.setOnClickListener {
            saveJournal(view)
        }
    }

    private fun saveJournal(view: View) {
        val title = view.findViewById<EditText>(R.id.etJournalTitle).text.toString().trim()
        val content = view.findViewById<EditText>(R.id.etJournalContent).text.toString().trim()
        val emotion = view.findViewById<Spinner>(R.id.spinnerEmotion).selectedItem.toString()

        if (!validateInputs(title, content)) return

        val mood = moodMapping[emotion] ?: 1
        val journalEntry = JournalEntry(
            journalId = UUID.randomUUID().toString(),
            userId = PreferencesHelper.getUserId(requireContext()) ?: return,
            title = title,
            content = content,
            mood = mood,
            isDraft = true,
            isDeleted = false
        )

        journalEntryViewModel.saveDraftJournalEntry(journalEntry)

        if (isConnectedToInternet()) {
            journalEntryViewModel.publishJournalEntry(journalEntry)
        } else {
            Toast.makeText(requireContext(), "No hay conexión. El journal se guardó como borrador.", Toast.LENGTH_SHORT).show()
        }

        navigateToMyJournals()
    }
    private fun validateInputs(title: String, content: String): Boolean {
        if (title.isBlank() || content.isBlank()) {
            Toast.makeText(requireContext(), "Título y contenido son obligatorios", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun handleSelectedImages(data: Intent?) {
        data?.let {
            if (it.clipData != null) {
                for (i in 0 until it.clipData!!.itemCount) {
                    selectedImages.add(it.clipData!!.getItemAt(i).uri)
                }
            } else {
                it.data?.let { uri -> selectedImages.add(uri) }
            }
            imagesAdapter.notifyDataSetChanged()
        }
    }

    private fun setupObservers() {
        journalEntryViewModel.createJournalEntryLiveData.observe(viewLifecycleOwner) { response ->
            val message = if (response.isSuccessful) {
                "Borrador publicado con éxito"
            } else {
                "Error al publicar borrador: ${response.message()}"
            }
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToMyJournals() {
        parentFragmentManager.commit {
            replace(R.id.fragment_container, MyJournalsFragment())
            addToBackStack(null)
        }
    }

    private fun isConnectedToInternet(): Boolean {
        val connectivityManager = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return connectivityManager.activeNetworkInfo?.isConnected == true
    }
}
