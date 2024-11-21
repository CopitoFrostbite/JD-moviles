package com.example.app1

import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.icu.util.Calendar
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.app1.ui.ReminderReceiver

class SetReminder : AppCompatActivity() {



    private lateinit var etReminderMessage: EditText
    private lateinit var btnPickDate: Button
    private lateinit var btnPickTime: Button
    private lateinit var btnSetReminder: Button
    private var calendar: Calendar = Calendar.getInstance()



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.fragment_set_reminder)

        etReminderMessage = findViewById(R.id.etReminderMessage)
        btnPickDate = findViewById(R.id.btnPickDate)
        btnPickTime = findViewById(R.id.btnPickTime)
        btnSetReminder = findViewById(R.id.btnSetReminder)


        btnPickDate.setOnClickListener {
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)
            DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
                calendar.set(selectedYear, selectedMonth, selectedDay)
            }, year, month, day).show()
        }

        btnPickTime.setOnClickListener {
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)
            TimePickerDialog(this, { _, selectedHour, selectedMinute ->
                calendar.set(Calendar.HOUR_OF_DAY, selectedHour)
                calendar.set(Calendar.MINUTE, selectedMinute)
            }, hour, minute, true).show()
        }

        btnSetReminder.setOnClickListener {
            setReminder()
        }


    }


    private fun setReminder() {
        val message = etReminderMessage.text.toString()
        if (message.isEmpty()) {
            Toast.makeText(this, "Please enter a reminder message", Toast.LENGTH_SHORT).show()
            return
        }

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, ReminderReceiver::class.java).apply {
            putExtra("MESSAGE", message)
        }
        val pendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0)

        alarmManager.setExact(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )

        Toast.makeText(this, "Reminder set", Toast.LENGTH_SHORT).show()
    }

}