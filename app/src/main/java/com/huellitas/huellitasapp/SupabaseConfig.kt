package com.huellitas.huellitasapp
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

object SupabaseConfig {
    const val URL = "https://pkhbmqtdwkxfhojlqqph.supabase.co" //url supabase
    const val ANON_KEY = "sb_publishable_ExqCk6H4qGbouTLlaIlc_A_r5JIOQLI" // key supabase

    // Creamos el cliente aquí para que sea global
    val supabase = createSupabaseClient(URL, ANON_KEY) {
        // Prueba con estos nombres, fíjate si el autocompletado te ayuda
        install(Auth)
        install(Postgrest)
        install(Storage)
    }
}