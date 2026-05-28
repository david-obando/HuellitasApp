package com.huellitas.huellitasapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.huellitas.huellitasapp.databinding.ActivityMainBinding
import com.huellitas.huellitasapp.modelos.Usuario
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import com.huellitas.huellitasapp.SupabaseConfig.supabase
import kotlinx.coroutines.launch
import androidx.navigation.ui.NavigationUI

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Verificar sesión primero
        val session = supabase.auth.currentSessionOrNull()
        if (session == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.appBarMain.toolbar)

        // 2. Cargar los datos del usuario
        cargarDatosDeUsuario()


        val navHostFragment =
            (supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment?)!!
        val navController = navHostFragment.navController

        // CONFIGURACIÓN DEL NAVIGATION DRAWER
        // CONFIGURACIÓN DEL NAVIGATION DRAWER
        binding.navView?.let { navView ->
            appBarConfiguration = AppBarConfiguration(
                setOf(
                    R.id.nav_transform, R.id.nav_reflow, R.id.nav_slideshow, R.id.nav_settings
                ),
                binding.drawerLayout
            )
            setupActionBarWithNavController(navController, appBarConfiguration)

            // Reemplazamos por un listener manual
            navView.setNavigationItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.nav_settings -> {
                        cerrarSesion()
                    }
                    else -> {
                        val handled = NavigationUI.onNavDestinationSelected(item, navController)
                        // USAMOS LA INTERROGACIÓN AQUÍ:
                        if (handled) binding.drawerLayout?.closeDrawers()
                    }
                }
                true
            }

            // CLIC EN EL HEADER
            val headerView = navView.getHeaderView(0)
            headerView.setOnClickListener {
                navController.navigate(R.id.nav_settings)
                // Y AQUÍ TAMBIÉN:
                binding.drawerLayout?.closeDrawers()
            }
        }
        // Bottom Navigation (si lo sigues usando)
        binding.appBarMain.contentMain.bottomNavView?.let {
            it.setupWithNavController(navController)
        }
        /*binding.appBarMain.contentMain.bottomNavView?.let {
            appBarConfiguration = AppBarConfiguration(
                setOf(
                    R.id.nav_transform, R.id.nav_reflow, R.id.nav_slideshow
                )
            )
            setupActionBarWithNavController(navController, appBarConfiguration)
            it.setupWithNavController(navController)
        }*/


    }

    private fun cerrarSesion() {
        lifecycleScope.launch {
            try {
                supabase.auth.signOut()
                irALogin()
            } catch (e: Exception) {
                Log.e("HUELLITAS_ERROR", "Error al cerrar sesión: ${e.message}")
            }
        }
    }

    private fun irALogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun cargarDatosDeUsuario() {
        val userAuth = supabase.auth.currentUserOrNull()

        if (userAuth != null) {
            lifecycleScope.launch {
                try {
                    // 1. Consultamos a Supabase usando el ID de la sesión
                    val datosUsuario = supabase.from("usuarios")
                        .select {
                            filter { eq("id", userAuth.id) }
                        }.decodeSingle<Usuario>()


                    // 2. OPCIONAL: Intentamos también ponerlo en el menú lateral si existe
                    binding.navView?.let { navView ->
                        if (navView.headerCount > 0) {
                            val headerView = navView.getHeaderView(0)
                            val tvNombre = headerView.findViewById<TextView>(R.id.tvNombreHeader)
                            val tvEmail = headerView.findViewById<TextView>(R.id.tvEmailHeader)

                            tvNombre?.text = datosUsuario.nombre
                            tvEmail?.text = datosUsuario.email
                            Log.d("HUELLITAS_OK", "Datos mostrados en el menú lateral")
                        }
                    }

                } catch (e: Exception) {
                    // Si algo falla, lo veremos en el Logcat y en el label
                    Log.e("HUELLITAS_ERROR", "Fallo al obtener datos: ${e.message}")
                }
            }
        } else {
            Log.e("HUELLITAS_ERROR", "No se encontró una sesión activa")
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val result = super.onCreateOptionsMenu(menu)
        // Using findViewById because NavigationView exists in different layout files
        // between w600dp and w1240dp
        val navView: NavigationView? = findViewById(R.id.nav_view)
        if (navView == null) {
            // The navigation drawer already has the items including the items in the overflow menu
            // We only inflate the overflow menu if the navigation drawer isn't visible
            menuInflater.inflate(R.menu.overflow, menu)
        }
        return result
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_settings -> {
                val navController = findNavController(R.id.nav_host_fragment_content_main)
                navController.navigate(R.id.nav_settings)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}
