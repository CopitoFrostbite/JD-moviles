package com.example.app1.ui

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.example.app1.R
import com.example.app1.data.model.User
import com.example.app1.viewmodel.UserViewModel
import dagger.hilt.android.AndroidEntryPoint
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream


@AndroidEntryPoint
class RegisterFragment : Fragment() {

    private val userViewModel: UserViewModel by viewModels()

    private var selectedImageUri: Uri? = null

    // Para seleccionar la imagen desde el dispositivo
    private val photoPickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            Toast.makeText(requireContext(), "Imagen seleccionada", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "Selección de imagen cancelada", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_register, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val emailEditText: EditText = view.findViewById(R.id.emailEditText)
        val passwordEditText: EditText = view.findViewById(R.id.passwordEditText)
        val repeatPasswordEditText: EditText = view.findViewById(R.id.repeatPasswordEditText)
        val nombreEditText: EditText = view.findViewById(R.id.nombreEditText)
        val apellidosEditText: EditText = view.findViewById(R.id.apellidosEditText)
        val usernameEditText: EditText = view.findViewById(R.id.usernameEditText)
        val btnRegister: Button = view.findViewById(R.id.btnRegister)
        val btnSelectImage: Button = view.findViewById(R.id.btnSelectImage)
        val tvLogin: TextView = view.findViewById(R.id.tvLogin)

        btnSelectImage.setOnClickListener {
            photoPickerLauncher.launch("image/*")
        }

        btnRegister.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()
            val repeatPassword = repeatPasswordEditText.text.toString()
            val username = usernameEditText.text.toString()
            val name = nombreEditText.text.toString()
            val lastname = apellidosEditText.text.toString()

            // Validamos los campos antes de continuar
            if (validateInputs(email, password, repeatPassword, username, name, lastname)) {
                if (selectedImageUri != null) {
                    try {
                        // Crear un archivo temporal a partir de la Uri
                        val inputStream = requireContext().contentResolver.openInputStream(selectedImageUri!!)
                        val file = File.createTempFile("temp_image", ".jpg", requireContext().cacheDir)
                        val outputStream = FileOutputStream(file)
                        inputStream.use { input ->
                            outputStream.use { output ->
                                input?.copyTo(output)
                            }
                        }

                        // Crear el RequestBody y MultipartBody.Part para la imagen
                        val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                        val avatarBody = MultipartBody.Part.createFormData("avatar", file.name, requestFile)

                        // Crear los RequestBody para los otros campos
                        val usernamePart = username.toRequestBody("text/plain".toMediaTypeOrNull())
                        val namePart = name.toRequestBody("text/plain".toMediaTypeOrNull())
                        val lastnamePart = lastname.toRequestBody("text/plain".toMediaTypeOrNull())
                        val emailPart = email.toRequestBody("text/plain".toMediaTypeOrNull())
                        val passwordPart = password.toRequestBody("text/plain".toMediaTypeOrNull())

                        // Llamamos a la API para crear el usuario
                        userViewModel.createUser(
                            usernamePart,
                            namePart,
                            lastnamePart,
                            emailPart,
                            passwordPart,
                            avatarBody
                        ).observe(viewLifecycleOwner) { response ->
                            if (response.isSuccessful) {
                                Toast.makeText(requireContext(), "Usuario creado con éxito", Toast.LENGTH_SHORT).show()
                                parentFragmentManager.commit {
                                    replace(R.id.fragment_container, LoginFragment())
                                    addToBackStack(null)
                                }
                            } else {
                                val errorBody = response.errorBody()?.string()
                                Log.e("RegisterFragment", "Error de registro: ${response.message()} - ${response.code()} - $errorBody")
                                Toast.makeText(requireContext(), "Error de registro: ${response.message()} - $errorBody", Toast.LENGTH_SHORT).show()
                                Log.e("RegisterFragment", "Response raw: ${response.raw()}")
                                Log.d("RegisterFragment", "Username: $username")
                                Log.d("RegisterFragment", "Name: $name")
                                Log.d("RegisterFragment", "Lastname: $lastname")
                                Log.d("RegisterFragment", "Email: $email")
                                Log.d("RegisterFragment", "Password: $password")
                                Log.d("RegisterFragment", "Avatar: ${avatarBody.body.contentType()}")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("RegisterFragment", "Error al procesar la imagen", e)
                        Toast.makeText(requireContext(), "Error al procesar la imagen", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "Selecciona una imagen para el perfil", Toast.LENGTH_SHORT).show()
                }
            }
        }

        tvLogin.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun validateInputs(
        email: String,
        password: String,
        repeatPassword: String,
        username: String,
        name: String,
        lastname: String
    ): Boolean {
        if (email.isBlank() || password.isBlank() || repeatPassword.isBlank() || username.isBlank() || name.isBlank() || lastname.isBlank()) {
            Toast.makeText(requireContext(), "Todos los campos son obligatorios", Toast.LENGTH_SHORT).show()
            return false
        }

        // Validar formato del email
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(requireContext(), "El email no es válido", Toast.LENGTH_SHORT).show()
            return false
        }

        // Validar fuerza de la contraseña
        val passwordPattern = Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[A-Za-z\\d]{8,}\$")
        if (!passwordPattern.matches(password)) {
            Toast.makeText(
                requireContext(),
                "La contraseña debe tener al menos 8 caracteres, una mayúscula, una minúscula y un número",
                Toast.LENGTH_SHORT
            ).show()
            return false
        }

        // Validar que las contraseñas coincidan
        if (password != repeatPassword) {
            Toast.makeText(requireContext(), "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
            return false
        }

        // Validar tamaño del nombre y apellidos
        if (name.length < 2 || name.length > 50) {
            Toast.makeText(requireContext(), "El nombre debe tener entre 2 y 50 caracteres", Toast.LENGTH_SHORT).show()
            return false
        }
        if (lastname.length < 2 || lastname.length > 50) {
            Toast.makeText(requireContext(), "Los apellidos deben tener entre 2 y 50 caracteres", Toast.LENGTH_SHORT).show()
            return false
        }

        // Validar formato del username
        val usernamePattern = Regex("^[a-zA-Z0-9_]{3,20}\$")
        if (!usernamePattern.matches(username)) {
            Toast.makeText(
                requireContext(),
                "El nombre de usuario debe tener entre 3 y 20 caracteres y solo puede contener letras, números y guiones bajos",
                Toast.LENGTH_SHORT
            ).show()
            return false
        }

        return true
    }
}

