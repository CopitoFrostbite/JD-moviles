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
import com.example.app1.data.model.JournalEntry
import com.example.app1.ui.adapters.ImagesAdapter
import com.example.app1.utils.PreferencesHelper
import com.example.app1.viewmodel.JournalEntryViewModel
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class NewJournalFragment : Fragment() {

    private val journalEntryViewModel: JournalEntryViewModel by viewModels()
    private var selectedEmotion: String? = null
    private val selectedImages = mutableListOf<Uri>()  // Lista para almacenar URIs de imágenes seleccionadas
    private lateinit var imagesAdapter: ImagesAdapter
    private lateinit var selectImagesLauncher: ActivityResultLauncher<Intent>

    companion object {
        private const val REQUEST_CODE_SELECT_IMAGES = 101  // Código de solicitud para el Intent de selección de imágenes
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_new_journal, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Obtener el userId de las preferencias compartidas
        val userId = PreferencesHelper.getUserId(requireContext()) ?: return
        // Configuración del ActivityResultLauncher para seleccionar imágenes
        selectImagesLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.let { data ->
                    if (data.clipData != null) { // Varias imágenes seleccionadas
                        for (i in 0 until data.clipData!!.itemCount) {
                            val imageUri = data.clipData!!.getItemAt(i).uri
                            selectedImages.add(imageUri)
                        }
                    } else if (data.data != null) { // Solo una imagen seleccionada
                        val imageUri = data.data!!
                        selectedImages.add(imageUri)
                    }
                    imagesAdapter.notifyDataSetChanged() // Actualizar el RecyclerView con las imágenes seleccionadas
                }
            }
        }

        // Configuración del RecyclerView para mostrar miniaturas de imágenes seleccionadas
        val recyclerViewImages = view.findViewById<RecyclerView>(R.id.recyclerViewImages)
        imagesAdapter = ImagesAdapter(selectedImages)
        recyclerViewImages.apply {
            adapter = imagesAdapter
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        }

        // Ajustes de insets para la vista principal
        ViewCompat.setOnApplyWindowInsetsListener(view.findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Configurar el Spinner de emociones
        val spinnerEmotion = view.findViewById<Spinner>(R.id.spinnerEmotion)
        val emotions = arrayOf("Triste", "Ira", "Sorpresa", "Miedo", "Feliz", "Inconforme")
        val moodMapping = mapOf("Triste" to 1, "Ira" to 2, "Sorpresa" to 3, "Miedo" to 4, "Feliz" to 5, "Inconforme" to 6)
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, emotions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerEmotion.adapter = adapter

        // Obtener referencias a los campos de título, contenido y botón de guardar
        val etJournalTitle = view.findViewById<EditText>(R.id.etJournalTitle)
        val etJournalContent = view.findViewById<EditText>(R.id.etJournalContent)
        val btnSaveJournal = view.findViewById<Button>(R.id.btnSaveJournal)

        // Configurar el botón para agregar imágenes
        val btnAddImage = view.findViewById<Button>(R.id.btnAddImage)
        btnAddImage.setOnClickListener {
            val intent = Intent().apply {
                type = "image/*"
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)  // Permitir selección múltiple de imágenes
                action = Intent.ACTION_GET_CONTENT
            }
            selectImagesLauncher.launch(Intent.createChooser(intent, "Selecciona imágenes"))
        }

        // Configuración del botón de guardar
        btnSaveJournal.setOnClickListener {
            val title = etJournalTitle.text.toString().trim()
            val content = etJournalContent.text.toString().trim()
            val emotion = spinnerEmotion.selectedItem.toString()

            if (validateJournalInputs(title, content)) {
                val mood = moodMapping[emotion] ?: 1
                saveDraftJournalEntry(userId, title, content, mood)

                // Intenta publicar el journal solo si hay conexión
                if (isConnectedToInternet()) {
                    journalEntryViewModel.createJournalEntry()
                } else {
                    Toast.makeText(requireContext(), "No hay conexión. El journal se guardó como borrador.", Toast.LENGTH_SHORT).show()
                }

                // Navegar al fragmento de diarios después de guardar
                navigateToMyJournals()
            }
        }

        // Observador para el estado de creación de entradas
        journalEntryViewModel.createJournalEntryLiveData.observe(viewLifecycleOwner, Observer { response ->
            if (response.isSuccessful) {
                Toast.makeText(requireContext(), "Entrada de diario creada con éxito", Toast.LENGTH_SHORT).show()
                navigateToMyJournals()
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
        saveImagesLocally(journalId)
    }

    private fun saveImagesLocally(journalId: String) {
        selectedImages.forEach { imageUri ->
            journalEntryViewModel.saveImageForJournal(journalId, imageUri)
        }
    }



    private fun navigateToMyJournals() {
        // Reemplaza el fragmento actual con MyJournalsFragment y limpia el backstack
        parentFragmentManager.commit {
            replace(R.id.fragment_container, MyJournalsFragment())
            addToBackStack(null)
        }
    }

    private fun isConnectedToInternet(): Boolean {
        val connectivityManager = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetworkInfo
        return activeNetwork != null && activeNetwork.isConnected
    }

    private fun validateJournalInputs(title: String, content: String): Boolean {
        if (title.isBlank() || content.isBlank()) {
            Toast.makeText(requireContext(), "Título y contenido son obligatorios", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }
}
