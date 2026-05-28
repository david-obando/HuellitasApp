package com.huellitas.huellitasapp.modelos

import kotlinx.serialization.Serializable

@Serializable
data class Mascota(
    val id: String? = null,
    val owner_id: String,
    val nombre: String,
    val raza: String? = null,
    val fecha_nacimiento: String? = null, // Formato "YYYY-MM-DD"
    val foto_url: String? = null,
    val created_at: String? = null,      // Timestamptz de Supabase
    val genero: String? = null,
    val tamano: String? = null,
    val comportamiento: String? = null,   // Aquí va el color (ej: "Verde")
    val descripcion_comportamiento: String? = null // El detalle largo
)