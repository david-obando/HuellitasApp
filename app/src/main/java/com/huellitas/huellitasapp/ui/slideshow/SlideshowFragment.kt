package com.huellitas.huellitasapp.ui.slideshow

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.huellitas.huellitasapp.R
import com.huellitas.huellitasapp.SupabaseConfig.supabase
import com.huellitas.huellitasapp.adapter.PaseosAdapter
import com.huellitas.huellitasapp.databinding.FragmentSlideshowBinding
import com.huellitas.huellitasapp.modelos.Mascota
import com.huellitas.huellitasapp.modelos.Paseos
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SlideshowFragment : Fragment() {

    private var _binding: FragmentSlideshowBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSlideshowBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Configurar el layout manager para el RecyclerView
        binding.rvHistorialPaseos.layoutManager = LinearLayoutManager(requireContext())

        // Lanzar la petición a Supabase
        obtenerHistorialDesdeSupabase()
    }

    private fun obtenerHistorialDesdeSupabase() {
        val usuarioActual = supabase.auth.currentUserOrNull() ?: return

        binding.progressBarHistorial.visibility = View.VISIBLE
        binding.lblSinPaseos.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // 1. Descargar las mascotas del dueño conectado para armar el mapa ID -> Nombre
                val mascotasUsuario = withContext(Dispatchers.IO) {
                    supabase.from("mascotas")
                        .select { filter { eq("owner_id", usuarioActual.id) } }
                        .decodeList<Mascota>()
                }

                val mapaMascotas = mascotasUsuario.associate { (it.id ?: "") to (it.nombre ?: "Mascota") }
                val listadoIdsMascotas = mascotasUsuario.map { it.id ?: "" }.filter { it.isNotEmpty() }

                if (listadoIdsMascotas.isEmpty()) {
                    binding.progressBarHistorial.visibility = View.GONE
                    binding.lblSinPaseos.visibility = View.VISIBLE
                    return@launch
                }

                // 2. Traer los paseos filtrando de forma segura con el constructor 'or' de Supabase
                val paseosBD = withContext(Dispatchers.IO) {
                    supabase.from("paseos")
                        .select {
                            filter {
                                if (listadoIdsMascotas.size == 1) {
                                    eq("mascota_id", listadoIdsMascotas.first())
                                } else {
                                    or {
                                        listadoIdsMascotas.forEach { idMascota ->
                                            eq("mascota_id", idMascota)
                                        }
                                    }
                                }
                            }
                        }
                        .decodeList<Paseos>()
                        // Ordenamos para ver los paseos recién creados arriba en la lista
                        .sortedByDescending { it.fecha_paseo }
                }

                // 3. Ocultar el cargador y setear el adaptador con la data, el mapa y el CLICK
                binding.progressBarHistorial.visibility = View.GONE
                if (paseosBD.isEmpty()) {
                    binding.lblSinPaseos.visibility = View.VISIBLE
                } else {
                    // CAMBIO CLAVE: Implementamos la lambda para capturar el click de la tarjeta
                    binding.rvHistorialPaseos.adapter = PaseosAdapter(paseosBD, mapaMascotas) { paseoSeleccionado ->

                        // Obtenemos el nombre de la mascota del mapa de mascotas
                        val nombreMascota = mapaMascotas[paseoSeleccionado.mascota_id] ?: "Mascota"

                        // Envolvemos el objeto del paseo y el nombre en un Bundle para pasarlo a la otra vista
                        val bundle = Bundle().apply {
                            putSerializable("PASEO_OBJETO", paseoSeleccionado)
                            putString("NOMBRE_MASCOTA_PASADO", nombreMascota)
                        }

                        // Navegamos al fragmento de detalle usando la ruta de navegación del nav_graph
                        findNavController().navigate(R.id.action_nav_slideshow_to_detallePaseoFragment, bundle)
                    }
                }

            } catch (e: Exception) {
                binding.progressBarHistorial.visibility = View.GONE
                binding.lblSinPaseos.visibility = View.VISIBLE
                Log.e("HUELLITAS_ERROR", "Error en el fragmento al cargar historial: ${e.message}")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}