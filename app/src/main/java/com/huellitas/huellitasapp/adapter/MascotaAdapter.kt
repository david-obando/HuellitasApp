package com.huellitas.huellitasapp.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.huellitas.huellitasapp.databinding.ItemMascotaBinding
import com.huellitas.huellitasapp.modelos.Mascota

class MascotaAdapter(
    private var mascotas: List<Mascota>,
    private val onItemClick: (Mascota) -> Unit,
    private val onItemLongClick: (Mascota) -> Unit // NUEVO: Callback para eliminación
) : RecyclerView.Adapter<MascotaAdapter.MascotaViewHolder>() {

    class MascotaViewHolder(val binding: ItemMascotaBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MascotaViewHolder {
        val binding = ItemMascotaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MascotaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MascotaViewHolder, position: Int) {
        val mascota = mascotas[position]

        with(holder.binding) {
            tvNombreMascotaItem.text = mascota.nombre
            tvRazaMascotaItem.text = "${mascota.raza} (${mascota.genero})"

            // Carga de imagen con Coil
            if (!mascota.foto_url.isNullOrEmpty()) {
                ivMascotaItem.load(mascota.foto_url) {
                    crossfade(true)
                    placeholder(android.R.drawable.ic_menu_gallery)
                    error(android.R.drawable.ic_menu_report_image)
                    transformations(CircleCropTransformation())
                }
            } else {
                ivMascotaItem.setImageResource(android.R.drawable.ic_menu_gallery)
            }

            // Lógica del semáforo
            val colorHex = when {
                mascota.comportamiento?.contains("Verde") == true -> "#4CAF50"
                mascota.comportamiento?.contains("Amarillo") == true -> "#FFEB3B"
                mascota.comportamiento?.contains("Rojo") == true -> "#F44336"
                mascota.comportamiento?.contains("Naranja") == true -> "#FF9800"
                mascota.comportamiento?.contains("Azul") == true -> "#2196F3"
                mascota.comportamiento?.contains("Blanco") == true -> "#FFFFFF"
                mascota.comportamiento?.contains("Morado") == true -> "#9C27B0"
                else -> "#BDBDBD"
            }
            viewSemaforo.setBackgroundColor(Color.parseColor(colorHex))

            // EVENTO 1: Clic normal (Abrir edición)
            root.setOnClickListener {
                onItemClick(mascota)
            }

            // EVENTO 2: Clic largo (Eliminar)
            root.setOnLongClickListener {
                onItemLongClick(mascota)
                true // Retornamos true para indicar que el evento fue procesado
            }
        }
    }

    override fun getItemCount() = mascotas.size

    fun updateList(nuevaLista: List<Mascota>) {
        mascotas = nuevaLista
        notifyDataSetChanged()
    }
}