package com.example.app1.ui

import android.content.Intent
import android.content.SharedPreferences
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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.bumptech.glide.Glide
import com.example.app1.R
import com.example.app1.data.model.User
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
    private lateinit var password: TextInputEditText
    private lateinit var email: TextInputEditText
    private lateinit var firstName: TextInputEditText
    private lateinit var lastName: TextInputEditText
    private lateinit var journalCount: TextInputEditText
    private lateinit var btnEditProfile: Button
    private val userViewModel: UserViewModel by activityViewModels()
    private var selectedImageUri: Uri? = null

    private val photoPickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            profileImage.setImageURI(uri) // Actualizar la imagen mostrada en la UI
            Toast.makeText(requireContext(), "Imagen seleccionada", Toast.LENGTH_SHORT).show()

            // Llamar a updateProfileImage para enviar la imagen inmediatamente
            Log.d("UserProfileFragment", "Imagen seleccionada, llamando a updateProfileImage")
            updateProfileImage(uri) // Llamada directa a la función para enviar la imagen
        } else {
            Toast.makeText(requireContext(), "Selección de imagen cancelada", Toast.LENGTH_SHORT).show()
        }
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
        password = view.findViewById(R.id.password)
        email = view.findViewById(R.id.email)
        firstName = view.findViewById(R.id.first_name)
        lastName = view.findViewById(R.id.last_name)
        journalCount = view.findViewById(R.id.journal_count)
        btnEditProfile = view.findViewById(R.id.btn_edit_profile)

        profileImage.setOnClickListener {
            photoPickerLauncher.launch("image/*")
        }

        // Load user data into UI
        userViewModel.getCurrentUser().observe(viewLifecycleOwner) { user ->
            user?.let {
                username.text = Editable.Factory.getInstance().newEditable(it.username)
                email.text = Editable.Factory.getInstance().newEditable(it.email)
                firstName.text = Editable.Factory.getInstance().newEditable(it.name)
                lastName.text = Editable.Factory.getInstance().newEditable(it.lastname)
                Glide.with(this).load(it.profilePicture).into(profileImage)
            }
        }

        // Set listener for updating profile data
        btnEditProfile.setOnClickListener {
            updateProfileData()
        }
    }


    // Método para actualizar solo los datos del perfil
    private fun updateProfileData() {
        val userId = PreferencesHelper.getUserId(requireContext()) ?: return
        if (userId.isBlank()) {
            Toast.makeText(requireContext(), "Error: ID de usuario no válido", Toast.LENGTH_SHORT).show()
            return
        }

        val updatedUser = User(
            userId = userId,
            username = username.text.toString(),
            name = firstName.text.toString(),
            lastname = lastName.text.toString(),
            email = email.text.toString(),
            password = password.text.toString(),
            profilePicture = userViewModel.user.value?.profilePicture
        )

        userViewModel.updateUserData(updatedUser).observe(viewLifecycleOwner) { response ->
            if (response.isSuccessful) {
                response.body()?.let { userResponse ->
                    username.text = Editable.Factory.getInstance().newEditable(userResponse.username)
                    email.text = Editable.Factory.getInstance().newEditable(userResponse.email)
                    firstName.text = Editable.Factory.getInstance().newEditable(userResponse.name)
                    lastName.text = Editable.Factory.getInstance().newEditable(userResponse.lastname)
                    Glide.with(this).load(userResponse.profilePicture).into(profileImage)
                }
                Toast.makeText(requireContext(), "Perfil actualizado correctamente", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Error al actualizar el perfil", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Método para actualizar solo la imagen del perfil
    private fun updateProfileImage(imageUri: Uri) {
        val userId = PreferencesHelper.getUserId(requireContext()) ?: ""
        if (userId.isBlank()) {
            Toast.makeText(requireContext(), "Error: ID de usuario no válido", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            // Crear un archivo temporal a partir de la Uri
            val inputStream = requireContext().contentResolver.openInputStream(imageUri)
            val file = File.createTempFile("temp_image", ".jpg", requireContext().cacheDir)
            val outputStream = FileOutputStream(file)
            inputStream.use { input ->
                outputStream.use { output ->
                    input?.copyTo(output)
                }
            }

            // Crear el RequestBody y MultipartBody.Part para la imagen
            val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
            val avatarPart = MultipartBody.Part.createFormData("avatar", file.name, requestFile)
            Log.d("UserProfileFragment", "Actualizando imagen con avatarPart: ${avatarPart.body.contentType()}")
            // Llamar al ViewModel para actualizar la imagen
            userViewModel.updateProfileImage(userId, avatarPart).observe(viewLifecycleOwner) { response ->
                if (response.isSuccessful) {
                    response.body()?.let { userResponse ->
                        Glide.with(this).load(userResponse.profilePicture).into(profileImage)
                    }
                    Toast.makeText(requireContext(), "Imagen actualizada correctamente", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Error al actualizar imagen", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Log.e("UserProfileFragment", "Error al procesar la imagen", e)
            Toast.makeText(requireContext(), "Error al procesar la imagen", Toast.LENGTH_SHORT).show()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_PICK && resultCode == AppCompatActivity.RESULT_OK) {
            val imageUri: Uri? = data?.data
            if (imageUri != null) {
                // Actualizar la imagen en la vista
                profileImage.setImageURI(imageUri)
                // Llamar a updateProfileImage para subir la imagen seleccionada
                updateProfileImage(imageUri)
            }
        }
    }

    companion object {
        private const val REQUEST_IMAGE_PICK = 1
    }
}
