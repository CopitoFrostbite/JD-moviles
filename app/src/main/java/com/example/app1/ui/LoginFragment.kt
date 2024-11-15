package com.example.app1.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.example.app1.R
import com.example.app1.viewmodel.UserViewModel
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class LoginFragment : Fragment() {

    private val userViewModel: UserViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val emailEditText: EditText = view.findViewById(R.id.emailEditText)
        val passwordEditText: EditText = view.findViewById(R.id.passwordEditText)
        val btnLogin: Button = view.findViewById(R.id.btnLogin)
        val tvRegister: TextView = view.findViewById(R.id.linkText)

        btnLogin.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (validateInputs(email, password)) {
                performLogin(email, password)
            } else {
                Toast.makeText(requireContext(), "Por favor ingrese un email y contraseña válidos.", Toast.LENGTH_SHORT).show()
            }
        }

        tvRegister.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, RegisterFragment())
                .addToBackStack(null)
                .commit()
        }

        observeViewModel()
    }

    private fun validateInputs(email: String, password: String): Boolean {
        return email.isNotEmpty() && password.isNotEmpty()
    }

    private fun observeViewModel() {
        userViewModel.user.observe(viewLifecycleOwner, Observer { user ->
            user?.let {

                Toast.makeText(requireContext(), "Bienvenido, ${it.username}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun performLogin(email: String, password: String) {
        userViewModel.loginUser(email, password).observe(viewLifecycleOwner, Observer { response ->
            if (response?.isSuccessful == true) {
                response.body()?.let { user ->
                    userViewModel.setUser(user)

                    Toast.makeText(requireContext(), "Login exitoso", Toast.LENGTH_SHORT).show()

                    startActivity(Intent(requireContext(), Home::class.java))
                    activity?.finish()
                } ?: run {

                    val errorMessage = "Error de autenticación: El cuerpo de la respuesta es null"
                    Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
                }
            } else {

                val errorMessage = response?.errorBody()?.string() ?: "Error de autenticación: ${response?.message()}"
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
            }
        })
    }

}