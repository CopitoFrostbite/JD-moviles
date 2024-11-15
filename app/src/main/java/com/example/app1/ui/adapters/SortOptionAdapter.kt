package com.example.app1.ui.adapters

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.app1.R
import com.example.app1.SortOption

class SortOptionAdapter(
    private val options: List<SortOption>,
    private var selectedOption: SortOption?,
    private var isAscending: Boolean,
    private val onOptionSelected: (SortOption, Boolean) -> Unit
) : RecyclerView.Adapter<SortOptionAdapter.SortOptionViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SortOptionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_sort_option, parent, false)
        return SortOptionViewHolder(view)
    }

    override fun onBindViewHolder(holder: SortOptionViewHolder, position: Int) {
        val option = options[position]
        holder.bind(option, option == selectedOption, isAscending)

        holder.itemView.setOnClickListener {
            val wasSelected = option == selectedOption
            val previousSelectedPosition = options.indexOf(selectedOption)

            // Cambia la dirección solo si el elemento ya estaba seleccionado
            isAscending = if (wasSelected) !isAscending else true
            selectedOption = option

            // Llama al callback y actualiza solo los elementos específicos
            onOptionSelected(option, isAscending)
            notifyItemChanged(previousSelectedPosition) // Actualiza el anterior seleccionado
            notifyItemChanged(position) // Actualiza el nuevo seleccionado
        }
    }

    override fun getItemCount() = options.size

    inner class SortOptionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val iconOption: ImageView = itemView.findViewById(R.id.iconOption)
        private val textOptionName: TextView = itemView.findViewById(R.id.textOptionName)
        private val iconSortDirection: ImageView = itemView.findViewById(R.id.iconSortDirection)

        fun bind(option: SortOption, isSelected: Boolean, isAscending: Boolean) {
            iconOption.setImageResource(option.iconResId)
            textOptionName.text = option.name
            textOptionName.setTypeface(null, if (isSelected) Typeface.BOLD else Typeface.NORMAL)

            itemView.setBackgroundColor(
                if (isSelected) ContextCompat.getColor(itemView.context, R.color.secondary_text_color)
                else ContextCompat.getColor(itemView.context, android.R.color.transparent)
            )

            iconSortDirection.visibility = if (isSelected) View.VISIBLE else View.GONE
            iconSortDirection.setImageResource(
                if (isAscending) R.drawable.ic_arrow_upward else R.drawable.ic_arrow_downward
            )
        }
    }
}