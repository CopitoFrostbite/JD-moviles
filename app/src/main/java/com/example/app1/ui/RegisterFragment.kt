package com.example.app1.ui

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
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
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

@AndroidEntryPoint
class RegisterFragment : Fragment() {

    private val userViewModel: UserViewModel by viewModels()
    private var selectedImageUri: Uri? = null

    private val photoPickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            Toast.makeText(requireContext(), "Imagen seleccionada: $uri", Toast.LENGTH_SHORT).show()
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

            if (validateInputs(email, password, repeatPassword, username, name, lastname)) {
                userViewModel.createUser(
                    username = username,
                    name = name,
                    lastname = lastname,
                    email = email,
                    password = password,
                    avatarUri = selectedImageUri
                ).observe(viewLifecycleOwner) { response: Response<User> ->
                    if (response.isSuccessful) {
                        val user = response.body()
                        Toast.makeText(requireContext(), "Usuario creado con éxito", Toast.LENGTH_SHORT).show()

                        parentFragmentManager.commit {
                            replace(R.id.fragment_container, LoginFragment())
                            addToBackStack(null)
                        }
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Log.e("RegisterFragment", "Error de registro: ${response.message()} - $errorBody")
                        Log.e("RegisterFragment", "Código de estado: ${response.code()}")
                        Toast.makeText(requireContext(), "Error de registro: ${response.message()} - $errorBody", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        tvLogin.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun validateInputs(email: String, password: String, repeatPassword: String, username: String, name: String, lastname: String): Boolean {
        if (email.isBlank() || password.isBlank() || repeatPassword.isBlank() || username.isBlank() || name.isBlank() || lastname.isBlank()) {
            Toast.makeText(requireContext(), "Todos los campos son obligatorios", Toast.LENGTH_SHORT).show()
            return false
        }
        if (password != repeatPassword) {
            Toast.makeText(requireContext(), "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }
}