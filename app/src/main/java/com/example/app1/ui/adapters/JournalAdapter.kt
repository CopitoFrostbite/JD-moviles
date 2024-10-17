package com.example.app1.ui.adapters

import android.annotation.SuppressLint
import android.icu.text.SimpleDateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
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
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JournalViewHolder {
        // Inflar el layout journal_item_layout.xml
        val view = LayoutInflater.from(parent.context).inflate(R.layout.journal_item_layout, parent, false)
        return JournalViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: JournalViewHolder, position: Int) {
        val journal = journals[position]
        holder.title.text = journal.title
        holder.date.text = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(journal.date)
        holder.time.text = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(journal.date)
        //holder.mood.text = "Mood: " + journal.mood

        // Acciones para los botones Edit y Delete
        holder.btnEdit.setOnClickListener {
            // Acción para editar el journal
        }

        holder.btnDelete.setOnClickListener {
            // Acción para eliminar el journal
        }
    }

    override fun getItemCount(): Int {
        return journals.size
    }

    fun updateJournals(newJournals: List<JournalEntry>) {
        journals = newJournals
        notifyDataSetChanged()
    }
}