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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.Glide
import com.example.app1.R
import com.example.app1.data.model.User
import com.example.app1.utils.NetworkUtils
import com.example.app1.utils.PreferencesHelper
import com.google.android.material.textfield.TextInputEditText
import com.example.app1.viewmodel.UserViewModel
import dagger.hilt.android.AndroidEntryPoint
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream


@AndroidEntryPoint
class UserProfileFragment : Fragment() {

    private lateinit var profileImage: ImageView
    private lateinit var username: TextInputEditText
    private lateinit var email: TextInputEditText
    private lateinit var firstName: TextInputEditText
    private lateinit var lastName: TextInputEditText
    private lateinit var btnEditProfile: Button
    private lateinit var btnUploadImage: Button

    private val userViewModel: UserViewModel by activityViewModels()
    private var selectedImageUri: Uri? = null

    private val photoPickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
        profileImage.setImageURI(uri)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_user_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        profileImage = view.findViewById(R.id.profile_image)
        username = view.findViewById(R.id.username)
        email = view.findViewById(R.id.email)
        firstName = view.findViewById(R.id.first_name)
        lastName = view.findViewById(R.id.last_name)
        btnEditProfile = view.findViewById(R.id.btn_edit_profile)
        //btnUploadImage = view.findViewById(R.id.btn_upload_image)

        profileImage.setOnClickListener {
            photoPickerLauncher.launch("image/*")
        }

        btnUploadImage.setOnClickListener {
            selectedImageUri?.let { updateProfileImage(it) }
        }

        btnEditProfile.setOnClickListener {
            updateProfileData()
        }

        observeViewModel()
    }

    private fun observeViewModel() {
        userViewModel.user.observe(viewLifecycleOwner) { user ->
            user?.let {
                username.text = Editable.Factory.getInstance().newEditable(it.username)
                email.text = Editable.Factory.getInstance().newEditable(it.email)
                firstName.text = Editable.Factory.getInstance().newEditable(it.name)
                lastName.text = Editable.Factory.getInstance().newEditable(it.lastname)
                Glide.with(this).load(it.profilePicture).into(profileImage)
            }
        }

        userViewModel.operationStatus.observe(viewLifecycleOwner) { status ->
            status?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateProfileData() {
        if (!NetworkUtils.isNetworkAvailable(requireContext())) {
            Toast.makeText(requireContext(), "No hay conexión a Internet", Toast.LENGTH_SHORT).show()
            return
        }

        if (!isInputValid()) {
            Toast.makeText(requireContext(), "Por favor completa todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        val updatedUser = User(
            userId = PreferencesHelper.getUserId(requireContext()) ?: "",
            username = username.text.toString(),
            name = firstName.text.toString(),
            lastname = lastName.text.toString(),
            email = email.text.toString(),
            password = "",
            profilePicture = userViewModel.user.value?.profilePicture
        )

        userViewModel.updateUserData(updatedUser)
    }

    private fun isInputValid(): Boolean {
        return username.text.toString().isNotBlank() &&
                email.text.toString().isNotBlank() &&
                firstName.text.toString().isNotBlank() &&
                lastName.text.toString().isNotBlank()
    }

    private fun updateProfileImage(imageUri: Uri) {
        val userId = PreferencesHelper.getUserId(requireContext()) ?: ""
        if (userId.isBlank()) return

        val file = File.createTempFile("temp_image", ".jpg", requireContext().cacheDir)
        requireContext().contentResolver.openInputStream(imageUri)?.use { inputStream ->
            FileOutputStream(file).use { outputStream -> inputStream.copyTo(outputStream) }
        }

        val avatarPart = MultipartBody.Part.createFormData(
            "avatar",
            file.name,
            file.asRequestBody("image/jpeg".toMediaTypeOrNull())
        )

        userViewModel.updateProfileImage(userId, avatarPart)
    }
}
