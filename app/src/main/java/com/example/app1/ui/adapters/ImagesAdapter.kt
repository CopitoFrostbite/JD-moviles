package com.example.app1.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.app1.R
import com.example.app1.data.model.Image

class ImagesAdapter(
    private var images: List<Image>,
    private val onImageClick: (Image) -> Unit,
    private val isEditable: Boolean
) : RecyclerView.Adapter<ImagesAdapter.ImageViewHolder>() {

    inner class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_image, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val image = images[position]

        // Cargar la imagen desde su ruta local o URL
        Glide.with(holder.itemView.context)
            .load(image.filePath) // Usa filePath como fuente de la imagen
            .thumbnail(0.1f) // Cargar como miniatura
            .into(holder.imageView)

        // Configurar clic según el modo
        holder.imageView.setOnClickListener {
            if (isEditable) {
                // En modo edición (NewJournalFragment), elimina la imagen
                onImageClick(image)
            } else {
                // En modo visualización (JournalDetailFragment), abre la imagen
                onImageClick(image)
            }
        }
    }



    fun updateImages(newImages: List<Image>) {
        images = newImages
        notifyDataSetChanged()
    }

    override fun getItemCount() = images.size
}