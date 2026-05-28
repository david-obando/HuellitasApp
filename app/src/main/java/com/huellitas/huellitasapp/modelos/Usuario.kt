package com.huellitas.huellitasapp.modelos

import kotlinx.serialization.Serializable

@Serializable
data class Usuario(
    val id: String,
    val nombre: String,
    val email: String,
    val celular: String? = null,
    val distrito: String? = null,
    val direccion: String? = null,
    val rol: String? = null
)