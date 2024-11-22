package com.example.app1.ui.main

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.widget.ProgressBar
import androidx.fragment.app.DialogFragment

class LoadingDialogFragment : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val progressBar = ProgressBar(requireContext()).apply {
            isIndeterminate = true
        }

        return AlertDialog.Builder(requireContext())
            .setView(progressBar)
            .setCancelable(false)
            .create()
    }
}