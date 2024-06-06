package com.example.app1

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat


class UserProfile : AppCompatActivity() {

    private lateinit var profileImage: ImageView
    private lateinit var btnChangeProfileImage: Button
    private lateinit var username: TextInputEditText
    private lateinit var email: TextInputEditText
    private lateinit var fullName: TextInputEditText
    private lateinit var journalCount: TextInputEditText
    private lateinit var btnEditProfile: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_user_profile)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        profileImage = findViewById(R.id.profile_image)
        btnChangeProfileImage = findViewById(R.id.btn_change_profile_image)
        username = findViewById(R.id.username)
        email = findViewById(R.id.email)
        fullName = findViewById(R.id.full_name)
        journalCount = findViewById(R.id.journal_count)
        btnEditProfile = findViewById(R.id.btn_edit_profile)

        btnChangeProfileImage.setOnClickListener {
            selectImage()
        }

        btnEditProfile.setOnClickListener {
            editProfile()
        }
    }

    private fun selectImage() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_IMAGE_PICK)
    }

    private fun editProfile() {
        Toast.makeText(this, "La info se guardo correctamente", Toast.LENGTH_SHORT).show()

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_PICK && resultCode == RESULT_OK) {
            val imageUri: Uri? = data?.data
            profileImage.setImageURI(imageUri)
            Log.d("SelectedImageUri", imageUri.toString())
        }
    }

    companion object {
        private const val REQUEST_IMAGE_PICK = 1
    }
}
