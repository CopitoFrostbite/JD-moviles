package com.example.app1.ui.adapters

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LongDateTypeAdapter : TypeAdapter<Long>() {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())

    override fun write(out: JsonWriter, value: Long?) {
        out.value(value?.let { dateFormat.format(Date(it)) })
    }

    override fun read(`in`: JsonReader): Long? {
        val dateStr = `in`.nextString()
        return dateFormat.parse(dateStr)?.time  // Devuelve el tiempo en milisegundos
    }
}