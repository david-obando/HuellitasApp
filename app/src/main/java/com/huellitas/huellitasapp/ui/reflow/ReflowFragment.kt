package com.huellitas.huellitasapp.ui.reflow

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.huellitas.huellitasapp.R
import com.huellitas.huellitasapp.SupabaseConfig.supabase
import com.huellitas.huellitasapp.databinding.FragmentReflowBinding
import com.huellitas.huellitasapp.modelos.Mascota
import com.huellitas.huellitasapp.adapter.MascotaAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ReflowFragment : Fragment() {

    private var _binding: FragmentReflowBinding? = null
    private val binding get() = _binding!!
    private lateinit var mascotaAdapter: MascotaAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentReflowBinding.inflate(inflater, container, false)

        setupRecyclerView()

        binding.fabAddMascota.setOnClickListener {
            findNavController().navigate(R.id.action_nav_reflow_to_addMascotaFragment)
        }

        cargarMascotas()

        return binding.root
    }

    private fun setupRecyclerView() {
        // Ahora el adaptador recibe dos parámetros de función (lambdas)
        mascotaAdapter = MascotaAdapter(
            mascotas = emptyList(),
            onItemClick = { mascotaSeleccionada ->
                // --- LÓGICA DE EDICIÓN (CLIC CORTO) ---
                val bundle = Bundle().apply {
                    putString("foto_url", mascotaSeleccionada.foto_url)
                    putString("id", mascotaSeleccionada.id)
                    putString("nombre", mascotaSeleccionada.nombre)
                    putString("raza", mascotaSeleccionada.raza)
                    putString("fecha_nacimiento", mascotaSeleccionada.fecha_nacimiento)
                    putString("genero", mascotaSeleccionada.genero)
                    putString("tamanio", mascotaSeleccionada.tamano)
                    putString("comportamiento", mascotaSeleccionada.comportamiento)
                    putString("descripcion_comportamiento", mascotaSeleccionada.descripcion_comportamiento)
                }
                findNavController().navigate(R.id.action_nav_reflow_to_editMascotaFragment, bundle)
            },
            onItemLongClick = { mascotaSeleccionada ->
                // --- LÓGICA DE ELIMINACIÓN (CLIC LARGO) ---
                mostrarDialogoConfirmacion(mascotaSeleccionada)
            }
        )

        binding.rvMascotas.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMascotas.adapter = mascotaAdapter
    }

    private fun mostrarDialogoConfirmacion(mascota: Mascota) {
        // Usamos MaterialAlertDialogBuilder para un diseño moderno (Material Design)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("¿Eliminar a ${mascota.nombre}?")
            .setMessage("Se borrarán todos sus datos y su foto del servidor. Esta acción es irreversible.")
            .setCancelable(false) // Obligamos al usuario a elegir una opción
            .setNeutralButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton("Eliminar") { _, _ ->
                eliminarMascota(mascota)
            }
            .show()
    }

    private fun eliminarMascota(mascota: Mascota) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // 1. ELIMINAR IMAGEN DEL STORAGE
                // Como usamos el nombre fijo pet_ID.webp, es fácil encontrarla
                val fileName = "pet_${mascota.id}.webp"
                val bucket = supabase.storage.from("fotos_mascotas")

                // Intentamos borrar la foto (si existe)
                try {
                    bucket.delete(listOf(fileName))
                } catch (e: Exception) {
                    Log.e("EliminarStorage", "No se pudo borrar la foto o no existía: ${e.message}")
                }

                // 2. ELIMINAR REGISTRO DE LA TABLA
                supabase.from("mascotas").delete {
                    filter {
                        eq("id", mascota.id!!) // El !! asegura que el ID existe para poder filtrar
                    }
                }

                // 3. ACTUALIZAR UI DESDE EL HILO PRINCIPAL
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "${mascota.nombre} ha sido eliminado", Toast.LENGTH_SHORT).show()
                    cargarMascotas() // Refrescamos la lista automáticamente
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error al eliminar: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun cargarMascotas() {
        val userId = supabase.auth.currentUserOrNull()?.id ?: return

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val lista = supabase.from("mascotas")
                    .select { filter { eq("owner_id", userId) } }
                    .decodeList<Mascota>()

                if (lista.isEmpty()) {
                    Toast.makeText(context, "Aún no tienes mascotas registradas", Toast.LENGTH_SHORT).show()
                }
                mascotaAdapter.updateList(lista)
            } catch (e: Exception) {
                Log.e("CargarMascotas", "Error: ${e.localizedMessage}")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}