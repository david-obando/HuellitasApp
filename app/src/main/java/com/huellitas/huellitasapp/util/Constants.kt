package com.huellitas.huellitasapp.util

object Constants {
    val distritosLima = arrayOf(
        "Ancón", "Ate", "Barranco", "Breña", "Carabayllo", "Cercado de Lima",
        "Chaclacayo", "Chorrillos", "Cieneguilla", "Comas", "El Agustino",
        "Independencia", "Jesús María", "La Molina", "La Victoria", "Lince",
        "Los Olivos", "Lurigancho", "Lurín", "Magdalena del Mar", "Miraflores",
        "Pachacámac", "Pucusana", "Pueblo Libre", "Puente Piedra", "Punta Hermosa",
        "Punta Negra", "Rímac", "San Bartolo", "San Borja", "San Isidro",
        "San Juan de Lurigancho", "San Juan de Miraflores", "San Luis",
        "San Martín de Porres", "San Miguel", "Santa Anita", "Santa María del Mar",
        "Santa Rosa", "Santiago de Surco", "Surquillo", "Villa El Salvador",
        "Villa María del Triunfo"
    )

    val comportamientoMascota = arrayOf(
        "Verde (Amigable)",
        "Amarillo (Nervioso)",
        "Rojo (No sociable)",
        "Naranja (No sociable con perros)",
        "Azul (Perro guía)",
        "Blanco (Discapacidad)",
        "Morado (No alimentar)"
    )

    val generoMascota = arrayOf(
        "Macho",
        "Hembra"
    )

    val tamanioMascota = arrayOf(
        "Pequeño",
        "Mediano",
        "Grande"
    )

    // --- NUEVOS CAMPOS PARA EL MÓDULO DE RESERVAS ---

    // Strings exactos que se guardarán en el campo 'tipo_pago' de Supabase
    const val PAGO_EFECTIVO = "efectivo"
    const val PAGO_YAPE = "yape"
    const val PAGO_PLIN = "plin"

    // Textos visibles para el Spinner de Duración
    val opcionesTiempoTexto = arrayOf(
        "30 minutos",
        "1 hora",
        "1 hora 30 minutos",
        "2 horas",
        "2 horas 30 minutos",
        "3 horas (Máximo)"
    )

    /**
     * Devuelve los minutos reales basados en la posición seleccionada del Spinner.
     * Si eligen "Tiempo Libre" (posición 6), devolvemos 0 minutos como bandera.
     */
    fun obtenerMinutosPorPosicion(posicion: Int): Int {
        return when (posicion) {
            0 -> 30
            1 -> 60
            2 -> 90
            3 -> 120
            4 -> 150
            5 -> 180
            6 -> 0 // 0 significa tiempo libre en nuestra lógica de negocio
            else -> 60
        }
    }

}