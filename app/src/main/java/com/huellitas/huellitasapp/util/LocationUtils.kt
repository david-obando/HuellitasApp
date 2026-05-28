package com.huellitas.huellitasapp.util

import android.location.Location
import com.huellitas.huellitasapp.modelos.Paseador

object LocationUtils {

    /**
     * Calcula la distancia en metros entre dos puntos geográficos.
     */
    fun calcularDistancia(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0] // Retorna la distancia en metros
    }

    /**
     * Toma la lista de paseadores de Supabase y la ordena:
     * El primero será el que esté más cerca de la ubicación del cliente.
     */
    fun obtenerPaseadoresOrdenados(
        miLat: Double,
        miLon: Double,
        lista: List<Paseador>
    ): List<Paseador> {
        return lista.sortedBy { paseador ->
            calcularDistancia(
                miLat, miLon,
                paseador.latitud_actual ?: 0.0,
                paseador.longitud_actual ?: 0.0
            )
        }
    }
}