package com.example.app1.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object ImageUtils {

    fun downloadImage(context: Context, imageUrl: String, callback: (String?) -> Unit) {
        Glide.with(context)
            .load(imageUrl)
            .into(object : CustomTarget<Drawable>() {
                override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                    val fileName = "profile_image_${System.currentTimeMillis()}.png"
                    val file = File(context.filesDir, fileName)
                    try {
                        val outputStream = FileOutputStream(file)
                        val bitmap = (resource as BitmapDrawable).bitmap
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                        outputStream.close()
                        callback(file.absolutePath)
                    } catch (e: IOException) {
                        e.printStackTrace()
                        callback(null)
                    }
                }

                override fun onLoadCleared(placeholder: Drawable?) {

                }
            })
    }
}