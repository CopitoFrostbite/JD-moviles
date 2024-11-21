package com.example.app1.ui.main

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.example.app1.R

class CustomConfirmationDialogFragment(
    private val title: String,
    private val message: String,
    private val onConfirm: () -> Unit
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext(), R.style.ThemeOverlay_Material3_Dialog)
        builder.setTitle(title)
        builder.setMessage(message)
        builder.setPositiveButton("Sí") { dialog, _ ->
            onConfirm()
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.dismiss()
        }
        return builder.create()
    }
}