package com.example.app1.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.example.app1.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ImageViewerFragment : DialogFragment() {

    companion object {
        private const val ARG_IMAGE_PATH = "image_path"

        fun newInstance(imagePath: String): ImageViewerFragment {
            val fragment = ImageViewerFragment()
            val args = Bundle()
            args.putString(ARG_IMAGE_PATH, imagePath)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_image_viewer, container, false)

        val imageView = view.findViewById<ImageView>(R.id.imageViewFull)
        val closeButton = view.findViewById<ImageButton>(R.id.btnCloseImageViewer)

        // Cargar la imagen desde el argumento
        val imagePath = arguments?.getString(ARG_IMAGE_PATH)
        Glide.with(this)
            .load(imagePath) // Ruta local o URL
            .into(imageView)

        // Configurar botón para cerrar
        closeButton.setOnClickListener {
            dismiss()
        }

        return view
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        dialog?.window?.setBackgroundDrawableResource(android.R.color.black)
    }
}