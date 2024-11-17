package com.example.app1.data.model

import androidx.room.Embedded
import androidx.room.Relation

data class JournalWithImages(
    @Embedded val journal: JournalEntry,
    @Relation(
        parentColumn = "journalId",
        entityColumn = "journalId"
    )
    val images: List<Image>
)