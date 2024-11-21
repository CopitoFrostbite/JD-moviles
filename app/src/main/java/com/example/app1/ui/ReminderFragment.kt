package com.example.app1.ui

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.app1.R
import com.example.app1.viewmodel.ReminderViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@AndroidEntryPoint
class ReminderFragment : Fragment() {

    private val reminderViewModel: ReminderViewModel by viewModels()

    private lateinit var selectedTimeTextView: TextView
    private lateinit var btnSetHour: Button
    private lateinit var btnSetAlarm: Button
    private lateinit var btnCancelAlarm: Button

    private var selectedTime: Calendar = Calendar.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_reminder, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        selectedTimeTextView = view.findViewById(R.id.selectedTime)
        btnSetHour = view.findViewById(R.id.btnSetHour)
        btnSetAlarm = view.findViewById(R.id.btnSetAlarm)
        btnCancelAlarm = view.findViewById(R.id.btnCancelAlarm)

        setupListeners()
        observeViewModel()
    }

    private fun setupListeners() {
        btnSetHour.setOnClickListener {
            showTimePickerDialog()
        }

        btnSetAlarm.setOnClickListener {
            saveReminder()
        }

        btnCancelAlarm.setOnClickListener {
            cancelReminder()
        }
    }

    private fun observeViewModel() {
        reminderViewModel.operationStatus.observe(viewLifecycleOwner) { status ->
            Toast.makeText(requireContext(), status, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showTimePickerDialog() {
        val currentTime = Calendar.getInstance()
        val hour = currentTime.get(Calendar.HOUR_OF_DAY)
        val minute = currentTime.get(Calendar.MINUTE)

        val timePicker = TimePickerDialog(
            requireContext(),
            { _, selectedHour, selectedMinute ->
                selectedTime.set(Calendar.HOUR_OF_DAY, selectedHour)
                selectedTime.set(Calendar.MINUTE, selectedMinute)
                updateSelectedTimeDisplay()
            },
            hour,
            minute,
            false
        )
        timePicker.show()
    }

    private fun updateSelectedTimeDisplay() {
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        selectedTimeTextView.text = timeFormat.format(selectedTime.time)
    }

    private fun saveReminder() {
        val userId = "currentUserId" // Reemplaza esto con la obtención del ID del usuario actual
        val description = "Create your journal" // Descripción del recordatorio
        val date = selectedTime.timeInMillis
        val time = selectedTimeTextView.text.toString()

        reminderViewModel.addReminder(userId, description, date, time)
    }

    private fun cancelReminder() {
        Toast.makeText(requireContext(), "Función de cancelar no implementada", Toast.LENGTH_SHORT).show()
        // Aquí puedes añadir la lógica para cancelar un recordatorio
    }
}