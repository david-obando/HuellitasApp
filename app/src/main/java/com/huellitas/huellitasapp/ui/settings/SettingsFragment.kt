package com.huellitas.huellitasapp.ui.settings

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.lifecycle.lifecycleScope
import com.huellitas.huellitasapp.SupabaseConfig.supabase
import com.huellitas.huellitasapp.databinding.FragmentSettingsBinding
import com.huellitas.huellitasapp.modelos.Usuario
import com.huellitas.huellitasapp.util.Constants // Importamos nuestra constante global
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)

        // 1. Configuración inicial de la UI
        setupSpinner()

        // 2. Carga de datos asíncrona desde Supabase
        cargarDatosDeUsuario()

        // 3. Acción del botón para persistir cambios
        binding.btnActualizarPerfil.setOnClickListener {
            actualizarPerfil()
        }

        return binding.root
    }

    private fun setupSpinner() {
        // Usamos la lista centralizada en Constants para evitar duplicidad de datos
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            Constants.distritosLima
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spnDistritoPerfil.adapter = adapter
    }

    private fun cargarDatosDeUsuario() {
        val userAuth = supabase.auth.currentUserOrNull() ?: return

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Consulta a Supabase filtrando por el ID de sesión activa
                val datos = supabase.from("usuarios")
                    .select { filter { eq("id", userAuth.id) } }
                    .decodeSingle<Usuario>()

                // Seteamos los textos en los campos del perfil
                binding.etNombrePerfil.setText(datos.nombre)
                binding.etCelularPerfil.setText(datos.celular ?: "")
                binding.etDireccionPerfil.setText(datos.direccion ?: "")

                // Buscamos el distrito en nuestra lista global para marcarlo en el Spinner
                val distritoGuardado = datos.distrito
                if (!distritoGuardado.isNullOrEmpty()) {
                    val index = Constants.distritosLima.indexOfFirst {
                        it.trim().equals(distritoGuardado.trim(), ignoreCase = true)
                    }

                    if (index >= 0) {
                        // .post asegura que la selección ocurra después de que el Spinner esté listo
                        binding.spnDistritoPerfil.post {
                            binding.spnDistritoPerfil.setSelection(index)
                        }
                    }
                }

            } catch (e: Exception) {
                Log.e("HUELLITAS_ERROR", "Error al jalar datos: ${e.message}")
            }
        }
    }

    private fun actualizarPerfil() {
        val userAuth = supabase.auth.currentUserOrNull() ?: return

        // Obtenemos el valor actual seleccionado en el Spinner
        val distritoSeleccionado = binding.spnDistritoPerfil.selectedItem.toString()

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Realizamos el UPDATE usando nombres de columnas directos (Strings)
                // Esto es más robusto ante cambios en las Data Classes
                supabase.from("usuarios").update(
                    {
                        set("nombre", binding.etNombrePerfil.text.toString())
                        set("celular", binding.etCelularPerfil.text.toString())
                        set("direccion", binding.etDireccionPerfil.text.toString())
                        set("distrito", distritoSeleccionado)
                    }
                ) {
                    filter { eq("id", userAuth.id) }
                }

                Toast.makeText(context, "¡Perfil actualizado con éxito!", Toast.LENGTH_SHORT).show()
                // 1. Volver al fragmento de inicio (Home)
                val navController = androidx.navigation.Navigation.findNavController(requireView())
                navController.popBackStack() // Esto regresa a la pantalla anterior
            } catch (e: Exception) {
                Log.e("HUELLITAS_ERROR", "Fallo el update: ${e.message}")
                Toast.makeText(context, "Error al guardar cambios", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}