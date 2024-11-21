package com.example.app1.ui.adapters

import com.google.gson.JsonParseException
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class LongDateTypeAdapter : TypeAdapter<Long>() {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    override fun write(out: JsonWriter, value: Long?) {
        // Convierte el timestamp a una cadena ISO al escribir
        out.value(value?.let { dateFormat.format(Date(it)) })
    }

    override fun read(`in`: JsonReader): Long? {
        return try {
            val jsonValue = `in`.nextString()
            // Verifica si el valor es un número largo o una cadena en formato ISO
            if (jsonValue.matches(Regex("\\d+"))) {
                // Si es un número, conviértelo directamente a Long
                jsonValue.toLong()
            } else {
                // Si es una cadena ISO, parsea la fecha y obtén el timestamp
                dateFormat.parse(jsonValue)?.time
            }
        } catch (e: Exception) {
            throw JsonParseException("Error al parsear la fecha o timestamp: ${e.message}", e)
        }
    }
}