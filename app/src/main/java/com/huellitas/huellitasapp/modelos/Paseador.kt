package com.huellitas.huellitasapp.modelos
import kotlinx.serialization.Serializable

@Serializable
data class Paseador(
    val id: String? = null,
    val usuario_id: String? = null,
    val disponible: Boolean = false,
    val precio_base: Double = 0.0,
    val calificacion_promedio: Double = 0.0,
    val cantidad_paseos: Int = 0,
    val esta_activo: Boolean = true,
    // Estos son los campos de GPS
    val latitud_actual: Double? = null,
    val longitud_actual: Double? = null,

    // Esto es para poder traer el nombre y distrito desde la tabla 'usuarios'
    val usuarios: List<Usuario>? = null,

    val foto_url: String?
)