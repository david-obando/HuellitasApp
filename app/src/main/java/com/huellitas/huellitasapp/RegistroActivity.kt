package com.huellitas.huellitasapp

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class RegistroActivity : AppCompatActivity() {

    // Cliente de Supabase
    private val supabase = createSupabaseClient(
        supabaseUrl = SupabaseConfig.URL,
        supabaseKey = SupabaseConfig.ANON_KEY
    ) {
        install(Auth)
        install(Postgrest)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_registro)

        // Ajuste de márgenes del sistema
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // --- DECLARACIÓN DE VARIABLES ---
        val etNombre = findViewById<EditText>(R.id.etNombreRegistro)
        val etTelefono = findViewById<EditText>(R.id.etTelefonoRegistro)
        val spnDistrito = findViewById<Spinner>(R.id.spnDistritoRegistro)
        val etDireccion = findViewById<EditText>(R.id.etDireccionRegistro)
        val etEmail = findViewById<EditText>(R.id.etEmailRegistro)
        val etPassword = findViewById<EditText>(R.id.etPasswordRegistro)
        val btnFinalizar = findViewById<Button>(R.id.btnFinalizarRegistro)

        // Configuración de la lista de distritos
        val distritosLima = arrayOf(
            "Ancón", "Ate", "Barranco", "Breña", "Carabayllo", "Cercado de Lima",
            "Chaclacayo", "Chorrillos", "Cieneguilla", "Comas", "El Agustino",
            "Independencia", "Jesús María", "La Molina", "La Victoria", "Lince",
            "Los Olivos", "Lurigancho", "Lurín", "Magdalena del Mar", "Miraflores",
            "Pachacámac", "Pucusana", "Pueblo Libre", "Puente Piedra", "Punta Hermosa",
            "Punta Negra", "Rímac", "San Bartolo", "San Borja", "San Isidro",
            "San Juan de Lurigancho", "San Juan de Miraflores", "San Luis",
            "San Martín de Porres", "San Miguel", "Santa Anita", "Santa María del Mar",
            "Santa Rosa", "Santiago de Surco", "Surquillo", "Villa El Salvador",
            "Villa María del Triunfo"
        )

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, distritosLima)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spnDistrito.adapter = adapter

        // --- LÓGICA DEL BOTÓN ---
        btnFinalizar.setOnClickListener {
            val nombre = etNombre.text.toString().trim()
            val telefono = etTelefono.text.toString().trim()
            val distrito = spnDistrito.selectedItem.toString()
            val direccion = etDireccion.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            // VALIDACIONES
            if (nombre.isEmpty()) {
                etNombre.error = "Ingresa tu nombre"
                etNombre.requestFocus()
                return@setOnClickListener
            }
            if (telefono.isEmpty()) {
                etTelefono.error = "Ingresa tu celular"
                etTelefono.requestFocus()
                return@setOnClickListener
            }

            if (telefono.length != 9) {
                etTelefono.error = "El celular debe tener 9 dígitos"
                etTelefono.requestFocus()
                return@setOnClickListener
            }

            //Validar que empiece con 9
            if (!telefono.startsWith("9")) {
                etTelefono.error = "El celular debe empezar con 9"
                etTelefono.requestFocus()
                return@setOnClickListener
            }

            if (direccion.isEmpty()) {
                etDireccion.error = "La dirección es necesaria para los paseos"
                etDireccion.requestFocus()
                return@setOnClickListener
            }

            if (email.isEmpty()) {
                etEmail.error = "El correo es obligatorio"
                etEmail.requestFocus()
                return@setOnClickListener
            }

            if (password.length < 6) {
                etPassword.error = "La contraseña debe tener al menos 6 caracteres"
                etPassword.requestFocus()
                return@setOnClickListener
            }

            // REGISTRO EN SUPABASE
            lifecycleScope.launch {
                try {
                    supabase.auth.signUpWith(Email) {
                        this.email = email
                        this.password = password
                        data = buildJsonObject {
                            put("nombre", nombre)
                            put("telefono", telefono)
                            put("distrito", distrito)
                            put("direccion", direccion)
                        }
                    }

                    Toast.makeText(this@RegistroActivity, "Registro exitoso. Revisa tu correo", Toast.LENGTH_LONG).show()
                    finish()

                } catch (e: Exception) {
                    Toast.makeText(this@RegistroActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}