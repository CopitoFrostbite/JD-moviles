package com.example.app1.utils

import androidx.recyclerview.widget.DiffUtil
import com.example.app1.data.model.JournalEntry

class JournalDiffCallback(
    private val oldList: List<JournalEntry>,
    private val newList: List<JournalEntry>
) : DiffUtil.Callback() {
    override fun getOldListSize(): Int = oldList.size
    override fun getNewListSize(): Int = newList.size
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].journalId == newList[newItemPosition].journalId
    }
    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}