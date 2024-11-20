package com.example.app1.ui.adapters

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.icu.text.SimpleDateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.app1.R
import com.example.app1.data.model.JournalEntry
import com.example.app1.utils.JournalDiffCallback

import java.util.Locale

class JournalAdapter(
    private var journals: List<JournalEntry>,
    private val onPublishDraft: (JournalEntry) -> Unit,
    private val onSyncImages: (journalId: String) -> Unit,
    private val onJournalClick: (journalId: String) -> Unit,
    private val onDelete: (journalId: String) -> Unit,
    private val onEdit: (journalId: String) -> Unit,
    private val context: Context

) : RecyclerView.Adapter<JournalAdapter.JournalViewHolder>() {

    private var selectedPosition = -1
    class JournalViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.tvJournalTitle1)
        val date: TextView = itemView.findViewById(R.id.tvJournalDate1)
        val time: TextView = itemView.findViewById(R.id.tvJournalTime1)
        val mood: TextView = itemView.findViewById(R.id.tvJournalMood1)
        val btnEdit: Button = itemView.findViewById(R.id.btnEdit1)
        val btnDelete: Button = itemView.findViewById(R.id.btnDelete1)
        val btnPublish: Button = itemView.findViewById(R.id.btnPublish)
        val draftLabel: TextView = itemView.findViewById(R.id.tvDraftLabel)
        val containerLayout: View = itemView.findViewById(R.id.containerLayout)
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JournalViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.journal_item_layout, parent, false)
        return JournalViewHolder(view)
    }

    override fun onBindViewHolder(holder: JournalViewHolder, @SuppressLint("RecyclerView") position: Int) {
        val journal = journals[position]

        // Configurar título, fecha y hora
        holder.title.text = journal.title
        holder.date.text = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(journal.date)
        holder.time.text = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(journal.date)
        holder.mood.text = "Mood: " + getMoodText(journal.mood)

        // Cambiar el color de fondo y la visibilidad del label de borrador
                if (journal.isDraft) {
                    holder.containerLayout.isSelected = true // Aplica el color de fondo de borrador
                    holder.draftLabel.visibility = View.VISIBLE
                    holder.btnPublish.visibility = View.VISIBLE
                } else {
                    holder.containerLayout.isSelected = false // Aplica el color de fondo regular
                    holder.draftLabel.visibility = View.GONE
                    holder.btnPublish.visibility = View.GONE
                }

        // Configura el fondo según si está seleccionada
        holder.containerLayout.isSelected = position == selectedPosition

        // Configura el OnClickListener para seleccionar la tarjeta
        holder.containerLayout.setOnClickListener {
            // Actualiza la posición seleccionada y notifica el cambio
            val previousPosition = selectedPosition
            selectedPosition = position
            notifyItemChanged(previousPosition)
            notifyItemChanged(selectedPosition)

            // Llama a onJournalClick para mostrar detalles o ejecutar alguna acción
            onJournalClick(journal.journalId)
        }

        // Acciones de botones Editar, Eliminar y Publicar
        holder.btnPublish.setOnClickListener {
            onPublishDraft(journal)
            onSyncImages(journal.journalId)
        }
        holder.btnEdit.setOnClickListener {
            onEdit(journal.journalId)
        }
        holder.btnDelete.setOnClickListener {
            showDeleteConfirmationDialog(journal.journalId)
        }
    }

    override fun getItemCount(): Int = journals.size

    private fun showDeleteConfirmationDialog(journalId: String) {
        AlertDialog.Builder(context)
            .setTitle("Confirmación")
            .setMessage("¿Estás seguro de que quieres borrar este diario?")
            .setPositiveButton("Sí") { _, _ ->
                onDelete(journalId)
            }
            .setNegativeButton("No", null) // No hacer nada si el usuario cancela
            .show()
    }

    fun updateJournals(newJournals: List<JournalEntry>) {
        val diffCallback = JournalDiffCallback(journals, newJournals)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        journals = newJournals
        diffResult.dispatchUpdatesTo(this)
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