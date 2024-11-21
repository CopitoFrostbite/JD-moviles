package com.example.app1.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.app1.R
import com.example.app1.ui.adapters.ImagesAdapter
import com.example.app1.viewmodel.ImageViewModel
import com.example.app1.viewmodel.JournalEntryViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Locale

@AndroidEntryPoint
class JournalDetailFragment : DialogFragment() {

    private val journalViewModel: JournalEntryViewModel by viewModels()
    private val imageViewModel: ImageViewModel by viewModels()

    private lateinit var journalId: String
    private lateinit var imagesAdapter: ImagesAdapter

    companion object {
        fun newInstance(journalId: String): JournalDetailFragment {
            val fragment = JournalDetailFragment()
            val args = Bundle()
            args.putString("journalId", journalId) // Pasamos solo el ID
            fragment.arguments = args
            return fragment
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.85).toInt(), // Ancho 85% de la pantalla
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.journal_detail, container, false)

        arguments?.getString("journalId")?.let {
            journalId = it
            loadJournalDetails(journalId)
            loadJournalImages(journalId)
        }

        view.findViewById<Button>(R.id.btnClose).setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).start()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.animate().scaleX(1f).scaleY(1f).setDuration(100).start()
                    dismiss()
                }
            }
            false
        }

        setupRecyclerView(view)

        return view
    }

    private fun setupRecyclerView(view: View) {
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewImages)

        imagesAdapter = ImagesAdapter(emptyList(), { image ->
            // Callback para abrir la imagen en el visor
            openImageInViewer(image.filePath)
        }, isEditable = false) // Modo visualización

        recyclerView.apply {
            adapter = imagesAdapter
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        }
    }

    private fun loadJournalDetails(journalId: String) {
        journalViewModel.getJournalEntryById(journalId).observe(viewLifecycleOwner) { journal ->
            journal?.let {
                view?.findViewById<TextView>(R.id.tvJournalTitle)?.text = it.title
                view?.findViewById<TextView>(R.id.tvJournalDate)?.text =
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it.date)
                view?.findViewById<TextView>(R.id.tvJournalContent)?.text = it.content
                view?.findViewById<TextView>(R.id.tvJournalMood)?.text = getMoodText(it.mood)
            }
        }
    }

    private fun loadJournalImages(journalId: String) {
        // Obtener imágenes relacionadas con el `journalId` desde el ViewModel
        imageViewModel.getImagesByJournalId(journalId).observe(viewLifecycleOwner) { images ->
            images?.let {
                imagesAdapter.updateImages(it) // Actualizar el adaptador con las imágenes obtenidas
            }
        }
    }

    private fun getMoodText(mood: Int): String {
        return when (mood) {
            1 -> "Triste"
            2 -> "Ira"
            3 -> "Sorpresa"
            4 -> "Miedo"
            5 -> "Feliz"
            6 -> "Inconforme"
            else -> "Desconocido"
        }
    }

    private fun openImageInViewer(filePath: String) {
        val fragmentManager = parentFragmentManager
        ImageViewerFragment.newInstance(filePath).show(fragmentManager, "image_viewer")
    }
}
