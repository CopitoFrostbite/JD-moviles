package com.example.app1.ui

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.app1.R
import com.example.app1.data.model.Image
import com.example.app1.data.model.JournalEntry
import com.example.app1.ui.adapters.ImagesAdapter
import com.example.app1.utils.UiState
import com.example.app1.viewmodel.JournalEntryViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Locale

@AndroidEntryPoint
class JournalDetailFragment : DialogFragment() {

    private val journalViewModel: JournalEntryViewModel by viewModels()
    private lateinit var journalId: String

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

        // Recuperar el ID del journal desde los argumentos
        arguments?.getString("journalId")?.let {
            journalId = it
            loadJournalDetails(journalId)
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

        return view
    }

    private fun loadJournalDetails(journalId: String) {
        // Observa el JournalEntry desde el ViewModel
        journalViewModel.getJournalEntryById(journalId).observe(viewLifecycleOwner) { journal ->
            journal?.let {
                // Muestra los detalles en los TextViews
                view?.findViewById<TextView>(R.id.tvJournalTitle)?.text = it.title
                view?.findViewById<TextView>(R.id.tvJournalDate)?.text = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it.date)
                view?.findViewById<TextView>(R.id.tvJournalContent)?.text = it.content
                view?.findViewById<TextView>(R.id.tvJournalMood)?.text = getMoodText(it.mood)
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
}
