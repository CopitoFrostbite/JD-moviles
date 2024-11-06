package com.example.app1.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
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
            isAscending = if (wasSelected) !isAscending else true
            selectedOption = option
            onOptionSelected(option, isAscending)
            notifyDataSetChanged() // Actualiza el fondo e íconos
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
            itemView.setBackgroundColor(
                if (isSelected) itemView.context.getColor(R.color.secondary_text_color)
                else itemView.context.getColor(android.R.color.transparent)
            )
            iconSortDirection.visibility = if (isSelected) View.VISIBLE else View.GONE
            iconSortDirection.setImageResource(if (isAscending) R.drawable.ic_arrow_upward else R.drawable.ic_arrow_downward)
        }
    }
}