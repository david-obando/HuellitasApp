package com.huellitas.huellitasapp

import android.content.Intent
import android.os.Bundle
import android.util.Log // Importante para depurar
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
// IMPORTANTE: Usamos el cliente único de tu objeto de configuración
import com.huellitas.huellitasapp.SupabaseConfig.supabase
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    // 1. YA NO inicializamos el cliente aquí.
    // Ahora usamos el 'supabase' que importamos de SupabaseConfig.

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        // Manejo de bordes de pantalla
        val mainView = findViewById<android.view.View>(android.R.id.content)
        // Nota: Asegúrate de que el ID de tu layout principal en activity_login.xml sea "main"
        val rootLayout = findViewById<android.view.View>(R.id.main)
        if (rootLayout != null) {
            ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }
        }

        // 2. Referenciamos tus campos del XML
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvOlvide = findViewById<TextView>(R.id.tvOlvidePassword)
        val tvRegistro = findViewById<TextView>(R.id.tvIrARegistro)

        // 3. Lógica para botón Iniciar Sesión
        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                lifecycleScope.launch {
                    try {
                        // Login con Supabase Auth usando el cliente global
                        supabase.auth.signInWith(Email) {
                            this.email = email
                            this.password = password
                        }

                        Log.d("LOGIN_SUCCESS", "Sesión iniciada correctamente")

                        // Si el login es exitoso, vamos al menú
                        irAlMenu()

                    } catch (e: Exception) {
                        Log.e("LOGIN_ERROR", "Error: ${e.message}")
                        Toast.makeText(
                            this@LoginActivity,
                            "Credenciales inválidas o falta confirmar correo",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } else {
                Toast.makeText(this, "Por favor, ingresa tus datos", Toast.LENGTH_SHORT).show()
            }
        }

        // 4. Lógica para Recuperar Contraseña
        tvOlvide.setOnClickListener {
            val email = etEmail.text.toString().trim()
            if (email.isNotEmpty()) {
                lifecycleScope.launch {
                    try {
                        // Enviar el correo de cambiar contraseña
                        supabase.auth.resetPasswordForEmail(email)
                        Toast.makeText(
                            this@LoginActivity,
                            "Enlace de recuperación enviado a tu email",
                            Toast.LENGTH_LONG
                        ).show()
                    } catch (e: Exception) {
                        Toast.makeText(this@LoginActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "Escribe tu correo arriba para recuperarlo", Toast.LENGTH_SHORT).show()
                etEmail.error = "Escribe tu correo aquí para enviarte el enlace"
                etEmail.requestFocus()
            }
        }

        // 5. Navegar hacia la pantalla de Registro
        tvRegistro.setOnClickListener {
            val intent = Intent(this, RegistroActivity::class.java)
            startActivity(intent)
        }
    }

    private fun irAlMenu() {
        val intent = Intent(this, MainActivity::class.java)
        // Flag de seguridad para limpiar el historial de actividades
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish() // Evita que el usuario regrese al login con el botón de atrás
    }
}