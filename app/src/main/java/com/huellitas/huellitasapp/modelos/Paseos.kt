package com.huellitas.huellitasapp.modelos

import kotlinx.serialization.Serializable

@Serializable
data class Paseos(
    // El id se declara como opcional y nulo por defecto porque Supabase (PostgreSQL)
    // lo autogenerará usando su secuencia o la función gen_random_uuid() al insertar.
    val id: String? = null,

    val mascota_id: String,
    val distrito_paseo: String,
    val direccion_paseo: String,
    val precio: Double,
    val tiempo_paseo: Int,
    val tipo_pago: String,
    val latitud_recojo: Double,
    val longitud_recojo: Double,
    val radio_seguridad_metros: Int,
    val notas: String? = null, // Nulo por defecto si el cliente no deja especificaciones
    val estado: String = "pendiente", // Estado inicial por defecto de la orden
    val fecha_paseo: String, // Fecha/Hora programada en formato "YYYY-MM-DD HH:mm:ss"
    val created_at: String? = null // Se deja nulo porque Supabase se encarga de llenarlo solo
)