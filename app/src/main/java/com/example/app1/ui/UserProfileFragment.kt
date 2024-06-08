package com.example.app1.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.bumptech.glide.Glide
import com.example.app1.R
import com.google.android.material.textfield.TextInputEditText
import com.example.app1.viewmodel.UserViewModel


class UserProfileFragment : Fragment() {

    private lateinit var profileImage: ImageView
    private lateinit var btnChangeProfileImage: Button
    private lateinit var username: TextInputEditText
    private lateinit var email: TextInputEditText
    private lateinit var fullName: TextInputEditText
    private lateinit var journalCount: TextInputEditText
    private lateinit var btnEditProfile: Button
    private val userViewModel: UserViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_user_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mainView: View = view.findViewById(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(mainView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        profileImage = view.findViewById(R.id.profile_image)
        btnChangeProfileImage = view.findViewById(R.id.btn_change_profile_image)
        username = view.findViewById(R.id.username)
        email = view.findViewById(R.id.email)
        fullName = view.findViewById(R.id.full_name)
        journalCount = view.findViewById(R.id.journal_count)
        btnEditProfile = view.findViewById(R.id.btn_edit_profile)

        btnChangeProfileImage.setOnClickListener {
            selectImage()
        }

        btnEditProfile.setOnClickListener {
            editProfile()
        }

        userViewModel.getCurrentUser().observe(viewLifecycleOwner) { user ->
            if (user != null) {
                // Actualiza la UI con los datos del usuario
                username.text = Editable.Factory.getInstance().newEditable(user.username)
                email.text = Editable.Factory.getInstance().newEditable(user.email)
                fullName.text =
                    Editable.Factory.getInstance().newEditable("${user.name} ${user.lastname}")

                 Glide.with(this).load(user.profilePicture).into(profileImage)
            }
        }
    }

    private fun selectImage() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_IMAGE_PICK)
    }

    private fun editProfile() {
        Toast.makeText(requireContext(), "La info se guardo correctamente", Toast.LENGTH_SHORT).show()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_PICK && resultCode == AppCompatActivity.RESULT_OK) {
            val imageUri: Uri? = data?.data
            profileImage.setImageURI(imageUri)
            Log.d("SelectedImageUri", imageUri.toString())
        }
    }

    companion object {
        private const val REQUEST_IMAGE_PICK = 1
    }
}
