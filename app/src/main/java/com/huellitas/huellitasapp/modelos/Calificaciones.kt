package com.huellitas.huellitasapp.modelos

import kotlinx.serialization.Serializable

@Serializable
data class Calificaciones(
    // Al igual que en Paseos, el id es opcional (null por defecto)
    // porque Supabase generará el UUID automáticamente al insertar.
    val id: String? = null,

    // Llave foránea que enlaza la calificación a un paseo específico
    val paseo_id: String,

    // Cantidad de estrellas asignadas (Int)
    val estrellas: Int,

    // Comentario u opinión opcional del cliente sobre el servicio
    val comentario: String? = null
) : java.io.Serializable