package com.example.app1.ui

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
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()
            performLogin(email, password)
        }

        tvRegister.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, RegisterFragment())
                .addToBackStack(null)
                .commit()
        }

        observeViewModel()
    }

    private fun observeViewModel() {
        userViewModel.user.observe(viewLifecycleOwner, Observer { user ->
            user?.let {
                // Actualiza la UI con los datos del usuario
                Toast.makeText(requireContext(), "Bienvenido, ${it.username}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun performLogin(email: String, password: String) {
        userViewModel.loginUser(email, password).observe(viewLifecycleOwner, Observer { response ->
            if (response.isSuccessful) {
                response.body()?.let { user ->
                    userViewModel.setUser(user)
                    // Login exitoso
                    Toast.makeText(requireContext(), "Login exitoso", Toast.LENGTH_SHORT).show()
                    // Navegar a otra pantalla, si es necesario
                }
            } else {
                // Manejar el caso de error en el login
                Toast.makeText(requireContext(), "Error de autenticación: ${response.message()}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}