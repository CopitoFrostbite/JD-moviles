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
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.app1.R
import com.example.app1.data.model.Image
import com.example.app1.data.model.JournalEntry
import com.example.app1.ui.adapters.ImagesAdapter
import com.example.app1.utils.PreferencesHelper
import com.example.app1.viewmodel.ImageViewModel
import com.example.app1.viewmodel.JournalEntryViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

@AndroidEntryPoint
class NewJournalFragment : Fragment() {

    private val journalEntryViewModel: JournalEntryViewModel by viewModels()
    private val imageViewModel: ImageViewModel by viewModels()

    private lateinit var journalId: String
    private val selectedImages = mutableListOf<Image>()
    private lateinit var imagesAdapter: ImagesAdapter
    private lateinit var selectImagesLauncher: ActivityResultLauncher<Intent>
    private val existingImageIds = mutableSetOf<String>()
    private val deletedImages = mutableListOf<Image>()
    private val moodMapping = mapOf(
        "Triste" to 1, "Ira" to 2, "Sorpresa" to 3,
        "Miedo" to 4, "Feliz" to 5, "Inconforme" to 6
    )

    companion object {
        private const val ARG_JOURNAL_ID = "journal_id"

        fun newInstance(journalId: String? = null): NewJournalFragment {
            val fragment = NewJournalFragment()
            val args = Bundle()
            args.putString(ARG_JOURNAL_ID, journalId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_new_journal, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.getString(ARG_JOURNAL_ID)?.let {
            journalId = it
            loadJournalData(journalId)
            loadJournalImages(journalId)
        }

        setupImageSelector()
        setupRecyclerView(view)
        setupSpinner(view)
        setupAddImageButton(view)
        setupSaveButton(view)
        setupObservers()
    }

    private fun loadJournalData(journalId: String) {
        journalEntryViewModel.getJournalEntryById(journalId).observe(viewLifecycleOwner) { journal ->
            journal?.let {
                view?.findViewById<EditText>(R.id.etJournalTitle)?.setText(it.title)
                view?.findViewById<EditText>(R.id.etJournalContent)?.setText(it.content)
                val spinnerEmotion = view?.findViewById<Spinner>(R.id.spinnerEmotion)
                spinnerEmotion?.setSelection(getMoodIndex(it.mood))
            }
        }
    }

    private fun getMoodIndex(mood: Int): Int {
        return moodMapping.values.indexOf(mood).takeIf { it >= 0 } ?: 0
    }

    private fun loadJournalImages(journalId: String) {
        imageViewModel.getImagesByJournalId(journalId).observe(viewLifecycleOwner) { images ->
            images?.let {
                selectedImages.clear()
                selectedImages.addAll(it)
                existingImageIds.clear()
                existingImageIds.addAll(it.map { image -> image.imageId }) // Guardar IDs existentes
                imagesAdapter.updateImages(selectedImages)
            }
        }
    }

    private fun setupImageSelector() {
        selectImagesLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                handleSelectedImages(result.data)
            }
        }
    }

    private fun setupAddImageButton(view: View) {
        val btnAddImage = view.findViewById<Button>(R.id.btnAddImage)
        btnAddImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true) // Permitir múltiples selecciones
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            selectImagesLauncher.launch(intent)
        }
    }

    private fun setupRecyclerView(view: View) {
        val recyclerViewImages = view.findViewById<RecyclerView>(R.id.recyclerViewImages)

        imagesAdapter = ImagesAdapter(selectedImages, { image ->
            // Si la imagen ya está guardada, agrégala a la lista de eliminados
            if (image.imageId in existingImageIds) {
                deletedImages.add(image)
            }
            // Eliminar de la lista seleccionada
            selectedImages.remove(image)
            imagesAdapter.updateImages(selectedImages)
        }, isEditable = true)

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
        btnSaveJournal.text = if (::journalId.isInitialized) "Actualizar Diario" else "Guardar Diario"
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
        val isEditMode = ::journalId.isInitialized

        if (!isEditMode) {
            journalId = UUID.randomUUID().toString()
        }

        val journalEntry = JournalEntry(
            journalId = journalId,
            userId = PreferencesHelper.getUserId(requireContext()) ?: return,
            title = title,
            content = content,
            mood = mood,
            isDraft = true,
            isDeleted = false
        )

        if (isEditMode) {
            journalEntryViewModel.updateJournalEntry(journalEntry)
        } else {
            journalEntryViewModel.saveDraftJournalEntry(journalEntry)
        }

        saveImages(journalId) // Guarda solo imágenes nuevas
        deleteImagesFromDatabase()


        Toast.makeText(requireContext(), "El journal se guardó como borrador.", Toast.LENGTH_SHORT).show()


        navigateToMyJournals()
    }

    private fun deleteImagesFromDatabase() {
        deletedImages.forEach { image ->
            imageViewModel.deleteImageById(image.imageId) // Llama al ViewModel para eliminar de la base de datos
            val localFile = File(image.filePath)
            if (localFile.exists()) {
                localFile.delete()
            }
        }
        deletedImages.clear() // Limpia la lista tras procesar las eliminaciones
    }

    private fun saveImages(journalId: String) {
        val newImages = selectedImages.filter { it.imageId !in existingImageIds }

        newImages.forEach { image ->
            val localFilePath = saveImageToLocal(Uri.parse(image.filePath), requireContext(), image.imageId)

            val updatedImage = image.copy(
                journalId = journalId,
                filePath = localFilePath
            )
            Log.d("SaveImages", "Guardando nueva imagen: $updatedImage")

            imageViewModel.addImageToEntry(journalId, updatedImage)
        }
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
            val newUris = mutableListOf<Uri>()

            if (it.clipData != null) {
                for (i in 0 until it.clipData!!.itemCount) {
                    val uri = it.clipData!!.getItemAt(i).uri
                    if (selectedImages.none { image -> image.filePath == uri.toString() }) {
                        newUris.add(uri)
                    }
                }
            } else {
                it.data?.let { uri ->
                    if (selectedImages.none { image -> image.filePath == uri.toString() }) {
                        newUris.add(uri)
                    }
                }
            }

            newUris.forEach { addImageToSelectedList(it) }
            imagesAdapter.notifyDataSetChanged()
        }
    }

    private fun addImageToSelectedList(uri: Uri) {
        val image = Image(
            imageId = UUID.randomUUID().toString(),
            journalId = "", // Se asignará más tarde
            filePath = uri.toString(), // Ruta del URI
            cloudUrl = null
        )
        selectedImages.add(image)
    }

    private fun saveImageToLocal(uri: Uri, context: Context, imageId: String): String {
        val file = File(context.filesDir, "$imageId.jpg")
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            FileOutputStream(file).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        return file.absolutePath // Devuelve la ruta del archivo guardado
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

}
