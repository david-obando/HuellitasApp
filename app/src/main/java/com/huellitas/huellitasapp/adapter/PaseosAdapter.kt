package com.huellitas.huellitasapp.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.huellitas.huellitasapp.R
import com.huellitas.huellitasapp.modelos.Paseos
import java.text.SimpleDateFormat
import java.util.Locale

class PaseosAdapter(
    private val listaPaseos: List<Paseos>,
    private val mapaMascotas: Map<String, String> // Recibe los nombres de las mascotas vinculados a sus IDs
) : RecyclerView.Adapter<PaseosAdapter.PaseoViewHolder>() {

    class PaseoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtMascota: TextView = view.findViewById(R.id.txtItemMascota)
        val txtFechaHora: TextView = view.findViewById(R.id.txtItemFechaHora)
        val txtEstado: TextView = view.findViewById(R.id.txtItemEstado)
        val ratingBar: RatingBar = view.findViewById(R.id.ratingBarPaseo)
        val txtCalificacionInfo: TextView = view.findViewById(R.id.txtCalificacionInfo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PaseoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_paseo, parent, false)
        return PaseoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PaseoViewHolder, position: Int) {
        val paseo = listaPaseos[position]

        // Buscamos el nombre real de la mascota usando su ID desde el mapa que pasamos
        val nombreMascota = mapaMascotas[paseo.mascota_id] ?: "Desconocida"
        holder.txtMascota.text = "Paseo de $nombreMascota"

        // Formatear la fecha
        // SOLUCIÓN DEFINITIVA AL FORMATO DE FECHA Y HORA
        try {
            // 1. Limpiamos cualquier microsegundo o letra Z sobrante que mande Supabase
            val fechaLimpia = paseo.fecha_paseo
                .replace("Z", "+00:00")
                .substringBefore(".")

            // 2. Detectamos si trae la zona horaria (+00:00) para usar el patrón ISO adecuado ('T' y 'XXX')
            val patronEntrada = if (fechaLimpia.contains("+") || fechaLimpia.contains("-")) {
                "yyyy-MM-dd'T'HH:mm:ssXXX"
            } else {
                "yyyy-MM-dd'T'HH:mm:ss"
            }

            val parser = SimpleDateFormat(patronEntrada, Locale.getDefault())
            val date = parser.parse(fechaLimpia)

            if (date != null) {
                // 3. Formateadores de salida por separado para estructurar tu frase
                val formatoFecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val formatoHora = SimpleDateFormat("HH:mm", Locale.getDefault())

                // Construye el texto exacto: "fecha 25/06/2026 hora 20:32"
                holder.txtFechaHora.text = "Fecha ${formatoFecha.format(date)} Hora ${formatoHora.format(date)}"
            } else {
                holder.txtFechaHora.text = paseo.fecha_paseo
            }
        } catch (e: Exception) {
            // Contingencia en caso de emergencia: remueve la 'T' y corta los segundos
            try {
                val alternativa = paseo.fecha_paseo.replace("T", " ")
                holder.txtFechaHora.text = "Fecha ${alternativa.substring(0, 10)} Hora ${alternativa.substring(11, 16)}"
            } catch (ex: Exception) {
                holder.txtFechaHora.text = paseo.fecha_paseo
            }
        }

        // Configuración del Estado
        val estadoNormalizado = paseo.estado.lowercase()
        holder.txtEstado.text = paseo.estado.uppercase()

        when (estadoNormalizado) {
            "pendiente" -> {
                holder.txtEstado.setBackgroundColor(Color.parseColor("#FFF3E0"))
                holder.txtEstado.setTextColor(Color.parseColor("#E65100"))

                // CONSIDERACIÓN: Estrellas grisadas (0) por defecto cuando está pendiente
                holder.ratingBar.rating = 0f
                holder.txtCalificacionInfo.text = "(Calificación pendiente)"
                holder.txtCalificacionInfo.visibility = View.VISIBLE
            }
            "en_progreso" -> {
                holder.txtEstado.setBackgroundColor(Color.parseColor("#E3F2FD"))
                holder.txtEstado.setTextColor(Color.parseColor("#0D47A1"))

                holder.ratingBar.rating = 0f
                holder.txtCalificacionInfo.text = "(En progreso...)"
                holder.txtCalificacionInfo.visibility = View.VISIBLE
            }
            "finalizado" -> {
                holder.txtEstado.setBackgroundColor(Color.parseColor("#E8F5E9"))
                holder.txtEstado.setTextColor(Color.parseColor("#1B5E20"))

                // Por ahora, al estar finalizado, lo dejamos sin calificar hasta conectar la tabla intermedia
                holder.ratingBar.rating = 0f
                holder.txtCalificacionInfo.text = "(Sin calificar aún)"
                holder.txtCalificacionInfo.visibility = View.VISIBLE
            }
            else -> {
                holder.txtEstado.setBackgroundColor(Color.parseColor("#EEEEEE"))
                holder.txtEstado.setTextColor(Color.parseColor("#616161"))
            }
        }
    }

    override fun getItemCount(): Int = listaPaseos.size
}