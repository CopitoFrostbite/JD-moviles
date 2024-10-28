package com.example.app1.ui.adapters

import android.annotation.SuppressLint
import android.graphics.Color
import android.icu.text.SimpleDateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.app1.R
import com.example.app1.data.model.JournalEntry

import java.util.Locale

class JournalAdapter(private var journals: List<JournalEntry>) : RecyclerView.Adapter<JournalAdapter.JournalViewHolder>() {

    class JournalViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.tvJournalTitle1)
        val date: TextView = itemView.findViewById(R.id.tvJournalDate1)
        val time: TextView = itemView.findViewById(R.id.tvJournalTime1)
        val mood: TextView = itemView.findViewById(R.id.tvJournalMood1)
        val btnEdit: Button = itemView.findViewById(R.id.btnEdit1)
        val btnDelete: Button = itemView.findViewById(R.id.btnDelete1)
        val containerLayout: View = itemView.findViewById(R.id.containerLayout) // Asegúrate de que el id sea correcto
        val draftLabel: View = itemView.findViewById(R.id.tvDraftLabel)
        val btnPublish: View = itemView.findViewById(R.id.btnPublish)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JournalViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.journal_item_layout, parent, false)
        return JournalViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: JournalViewHolder, position: Int) {
        val journal = journals[position]

        // Configurar título, fecha y hora
        holder.title.text = journal.title
        holder.date.text = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(journal.date)
        holder.time.text = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(journal.date)

        // Mostrar el estado de ánimo basado en el valor de `mood`
        holder.mood.text = "Mood: " + getMoodText(journal.mood)

        // Obtener los colores desde el contexto
        val draftBackgroundColor = ContextCompat.getColor(holder.itemView.context, R.color.draft_background)
        val regularBackgroundColor = ContextCompat.getColor(holder.itemView.context, R.color.regular_background)

        // Cambiar el color de fondo si es borrador
        if (journal.isDraft) {
            holder.containerLayout.setBackgroundColor(draftBackgroundColor)
            holder.draftLabel.visibility = View.VISIBLE
            holder.btnPublish.visibility = View.VISIBLE
        } else {
            holder.containerLayout.setBackgroundColor(regularBackgroundColor)
            holder.draftLabel.visibility = View.GONE
            holder.btnPublish.visibility = View.GONE
        }

        // Configurar acciones de botones Edit y Delete
        holder.btnEdit.setOnClickListener {
            // Acción para editar el journal
        }

        holder.btnDelete.setOnClickListener {
            // Acción para eliminar el journal
        }

        // Acción para el botón "Publicar"
        holder.btnPublish.setOnClickListener {
            // Lógica para publicar el journal (cambia `isDraft` a false y actualiza en ViewModel o DB)
        }
    }

    override fun getItemCount(): Int = journals.size

    fun updateJournals(newJournals: List<JournalEntry>) {
        journals = newJournals
        notifyDataSetChanged()
    }

    // Método auxiliar para convertir `mood` en texto
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