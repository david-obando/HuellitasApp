package com.huellitas.huellitasapp

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import coil.load
import coil.transform.CircleCropTransformation
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.huellitas.huellitasapp.databinding.FragmentDetallePaseoBinding
import com.huellitas.huellitasapp.modelos.Paseos
import java.net.URLEncoder
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

            // Extraemos de forma segura el primer elemento de las listas anidadas
            val paseador = paseo.paseadores?.firstOrNull()
            val usuarioPaseador = paseador?.usuarios?.firstOrNull()

            // Asignamos la información recuperada de las relaciones a los TextViews correspondientes
            binding.txtPaseadorNombre.text = usuarioPaseador?.nombre ?: "Paseador Asignado"
            binding.txtPaseadorCelular.text = "📞 Celular: ${usuarioPaseador?.celular ?: "-"}"
            binding.rbPaseadorCalificacion.rating = (paseador?.calificacion_promedio ?: 0.0).toFloat()

            // Cargar Foto del paseador de manera asíncrona con Coil
            val fotoUrl = paseador?.foto_url
            if (!fotoUrl.isNullOrEmpty()) {
                binding.imgPaseadorFoto.load(fotoUrl) {
                    crossfade(true)
                    placeholder(android.R.drawable.ic_menu_gallery)
                    error(android.R.drawable.ic_menu_report_image)
                    transformations(CircleCropTransformation()) // Aplica el recorte circular estético
                }
            } else {
                binding.imgPaseadorFoto.setImageResource(android.R.drawable.ic_menu_gallery)
            }

            // Lógica del botón para abrir el chat directo en WhatsApp
            binding.btnContactarWhatsApp.setOnClickListener {
                val celular = usuarioPaseador?.celular
                if (!celular.isNullOrEmpty()) {
                    abrirWhatsApp(celular, nombreMascota)
                } else {
                    Toast.makeText(context, "El paseador no cuenta con un número de celular válido", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun abrirWhatsApp(celular: String, nombreMascota: String) {
        // Limpiamos la cadena quitando espacios vacíos o guiones intermedios
        var numeroLimpio = celular.replace(" ", "").replace("-", "")

        // Verificamos y anteponemos de forma automática el prefijo telefónico de Perú (51)
        if (!numeroLimpio.startsWith("51") && !numeroLimpio.startsWith("+51")) {
            numeroLimpio = "51$numeroLimpio"
        }
        if (numeroLimpio.startsWith("+")) {
            numeroLimpio = numeroLimpio.replace("+", "")
        }

        // Mensaje predeterminado codificado para evitar problemas con espacios en la URL
        val mensaje = "Hola, te contacto desde HuellitasApp respecto al paseo de $nombreMascota. ¡Muchas gracias!"

        try {
            val url = "https://wa.me/$numeroLimpio?text=${URLEncoder.encode(mensaje, "UTF-8")}"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "No se pudo abrir WhatsApp o la aplicación no está instalada", Toast.LENGTH_SHORT).show()
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