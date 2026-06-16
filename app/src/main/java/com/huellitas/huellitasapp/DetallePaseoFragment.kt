package com.huellitas.huellitasapp

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.huellitas.huellitasapp.databinding.FragmentDetallePaseoBinding
import com.huellitas.huellitasapp.modelos.Paseos
import java.text.SimpleDateFormat
import java.util.Locale

class DetallePaseoFragment : Fragment(R.layout.fragment_detalle_paseo) {

    private var _binding: FragmentDetallePaseoBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentDetallePaseoBinding.bind(view)

        // Inicializar el mapa
        binding.mapViewDetalle.onCreate(savedInstanceState)

        val paseo = arguments?.getSerializable("PASEO_OBJETO") as? Paseos
        // Recogemos el nombre que pasamos desde SlideshowFragment
        val nombreMascota = arguments?.getString("NOMBRE_MASCOTA_PASADO") ?: "Mascota"

        if (paseo != null) {
            inyectarDatosEnInterfaz(paseo, nombreMascota)
            gestionarSeccionCalificacion(paseo)
            configurarMapa(paseo)
        }
    }

    private fun inyectarDatosEnInterfaz(paseo: Paseos, nombreMascota: String) {
        // 1. Formateo de fecha
        try {
            val fechaLimpia = paseo.fecha_paseo.replace("Z", "+00:00").substringBefore(".")
            val patron = if (fechaLimpia.contains("+")) "yyyy-MM-dd'T'HH:mm:ssXXX" else "yyyy-MM-dd'T'HH:mm:ss"
            val date = SimpleDateFormat(patron, Locale.getDefault()).parse(fechaLimpia)
            binding.txtDetalleFecha.text = "Programado: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(date!!)}"
        } catch (e: Exception) {
            binding.txtDetalleFecha.text = "Programado: ${paseo.fecha_paseo}"
        }

        // AHORA USAMOS EL NOMBRE REAL
        binding.txtDetalleMascota.text = "Paseo de: $nombreMascota"

        binding.txtDetalleEstado.text = paseo.estado.uppercase()

        binding.txtDetalleDistrito.text = "Distrito: ${paseo.distrito_paseo}"
        binding.txtDetalleDireccion.text = "Dirección: ${paseo.direccion_paseo}"
        binding.txtDetalleDuracion.text = "Duración: ${paseo.tiempo_paseo} min"
        binding.txtDetalleRadio.text = "Radio de seguridad: ${paseo.radio_seguridad_metros} metros"
        binding.txtDetallePago.text = "Método de pago: ${paseo.tipo_pago}"
        binding.txtDetalleNotas.text = "Notas: ${paseo.notas ?: "Sin especificaciones"}"
        binding.txtDetallePrecio.text = "S/. ${String.format("%.2f", paseo.precio)}"

        if (paseo.estado.lowercase() == "pendiente") {
            binding.cardPaseador.visibility = View.GONE
            binding.txtDetalleEstado.setBackgroundColor(Color.parseColor("#E65100"))
        } else {
            binding.cardPaseador.visibility = View.VISIBLE
        }
    }

    private fun configurarMapa(paseo: Paseos) {
        binding.mapViewDetalle.getMapAsync { map ->
            val pos = LatLng(paseo.latitud_recojo ?: -12.0464, paseo.longitud_recojo ?: -77.0428)
            map.addMarker(MarkerOptions().position(pos).title("Punto de Recojo"))
            map.addCircle(CircleOptions()
                .center(pos)
                .radius(paseo.radio_seguridad_metros.toDouble())
                .strokeColor(Color.BLUE)
                .fillColor(Color.parseColor("#220000FF")))
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, 15f))
        }
    }

    private fun gestionarSeccionCalificacion(paseo: Paseos) {
        if (paseo.estado.lowercase() == "finalizado") {
            binding.cardCalificacionPaseo.visibility = View.VISIBLE
        } else {
            binding.cardCalificacionPaseo.visibility = View.GONE
        }
    }

    // CICLO DE VIDA CORREGIDO
    override fun onResume() { super.onResume(); binding.mapViewDetalle.onResume() }
    override fun onPause() { super.onPause(); binding.mapViewDetalle.onPause() }
    override fun onStart() { super.onStart(); binding.mapViewDetalle.onStart() }
    override fun onStop() { super.onStop(); binding.mapViewDetalle.onStop() }
    override fun onLowMemory() { super.onLowMemory(); binding.mapViewDetalle.onLowMemory() }

    override fun onDestroyView() {
        // Primero destruimos el mapa, LUEGO anulamos el binding
        binding.mapViewDetalle.onDestroy()
        _binding = null
        super.onDestroyView()
    }
}