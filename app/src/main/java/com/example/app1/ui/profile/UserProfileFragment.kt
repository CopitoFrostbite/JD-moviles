package com.example.app1.ui.profile


import android.net.Uri
import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
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
    private lateinit var email: TextInputEditText
    private lateinit var firstName: TextInputEditText
    private lateinit var lastName: TextInputEditText
    private lateinit var currentPassword: TextInputEditText
    private lateinit var newPassword: TextInputEditText
    private lateinit var confirmPassword: TextInputEditText
    private lateinit var btnEditProfile: Button
    private lateinit var btnUploadImage: Button
    private lateinit var btnChangePassword: Button

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
        currentPassword = view.findViewById(R.id.current_password)
        newPassword = view.findViewById(R.id.new_password)
        confirmPassword = view.findViewById(R.id.confirm_password)
        btnEditProfile = view.findViewById(R.id.btn_edit_profile)
        btnUploadImage = view.findViewById(R.id.btn_upload_image)
        btnChangePassword = view.findViewById(R.id.btn_change_password)





        profileImage.setOnClickListener {
            photoPickerLauncher.launch("image/*")

        }

        btnUploadImage.setOnClickListener {
            selectedImageUri?.let { updateProfileImage(it) }
        }

        btnEditProfile.setOnClickListener {
            updateProfileData()
        }

        btnChangePassword.setOnClickListener {
            changePassword()
        }

        observeViewModel()
    }

    private fun observeViewModel() {
        userViewModel.user.observe(viewLifecycleOwner) { user ->
            user?.let {
                username.setText(it.username)
                email.setText(it.email)
                firstName.setText(it.name)
                lastName.setText(it.lastname)
                loadProfileImage(it)
            }
        }

        userViewModel.operationStatus.observe(viewLifecycleOwner) { status ->
            status?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateProfileData() {
        if (!isInputValid()) return

        val updatedUser = User(
            userId = PreferencesHelper.getUserId(requireContext()) ?: "",
            username = username.text.toString(),
            name = firstName.text.toString(),
            lastname = lastName.text.toString(),
            email = email.text.toString(),
            password = "",
            profilePicture = userViewModel.user.value?.profilePicture
        )


        Toast.makeText(requireContext(), "Actualizando datos del perfil...", Toast.LENGTH_SHORT).show()

        userViewModel.updateUserData(updatedUser).observe(viewLifecycleOwner) { response ->
            if (response.isSuccessful) {
                Toast.makeText(requireContext(), "Perfil actualizado con éxito", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Error al actualizar el perfil", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun isInputValid(): Boolean {
        if (username.text.toString().isBlank() ||
            email.text.toString().isBlank() ||
            firstName.text.toString().isBlank() ||
            lastName.text.toString().isBlank()
        ) {
            Toast.makeText(requireContext(), "Todos los campos son obligatorios", Toast.LENGTH_SHORT).show()
            return false
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email.text.toString()).matches()) {
            Toast.makeText(requireContext(), "El email no es válido", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
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


        Toast.makeText(requireContext(), "Subiendo imagen de perfil...", Toast.LENGTH_SHORT).show()

        userViewModel.updateProfileImage(userId, avatarPart).observe(viewLifecycleOwner) { response ->
            if (response.isSuccessful) {
                Toast.makeText(requireContext(), "Imagen de perfil actualizada", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Error al subir la imagen", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadProfileImage(user: User) {
        val localPath = user.localProfilePicture // Prioridad local
        val remoteUrl = user.profilePicture // Respaldo remoto

        if (!localPath.isNullOrBlank() && File(localPath).exists()) {
            // Carga desde la ruta local
            Glide.with(this).load(File(localPath)).into(profileImage)
        } else if (!remoteUrl.isNullOrBlank()) {
            // Carga desde la URL remota y guarda localmente
            Glide.with(this)
                .load(remoteUrl)
                .into(profileImage)
                .clearOnDetach()
        } else {
            // Imagen por defecto
            profileImage.setImageResource(R.drawable.ic_profile_placeholder)
        }
    }

    private fun changePassword() {
        val userId = PreferencesHelper.getUserId(requireContext()) ?: ""
        if (userId.isBlank()) {
            Toast.makeText(requireContext(), "No se pudo obtener el ID del usuario", Toast.LENGTH_SHORT).show()
            return
        }

        val currentPwd = currentPassword.text.toString()
        val newPwd = newPassword.text.toString()
        val confirmPwd = confirmPassword.text.toString()

        if (currentPwd.isBlank() || newPwd.isBlank() || confirmPwd.isBlank()) {
            Toast.makeText(requireContext(), "Todos los campos son obligatorios", Toast.LENGTH_SHORT).show()
            return
        }

        if (newPwd != confirmPwd) {
            Toast.makeText(requireContext(), "Las contraseñas nuevas no coinciden", Toast.LENGTH_SHORT).show()
            return
        }

        val passwordPattern = Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[A-Za-z\\d]{8,}\$")
        if (!passwordPattern.matches(newPwd)) {
            Toast.makeText(
                requireContext(),
                "La nueva contraseña debe tener al menos 8 caracteres, una mayúscula, una minúscula y un número",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        userViewModel.updatePassword(userId, currentPwd, newPwd).observe(viewLifecycleOwner) { response ->
            if (response.isSuccessful) {
                Toast.makeText(requireContext(), "Contraseña actualizada con éxito", Toast.LENGTH_SHORT).show()
                currentPassword.text?.clear()
                newPassword.text?.clear()
                confirmPassword.text?.clear()
            } else {
                val errorBody = response.errorBody()?.string()
                Toast.makeText(requireContext(), "Error: $errorBody", Toast.LENGTH_SHORT).show()
            }
        }
    }
}