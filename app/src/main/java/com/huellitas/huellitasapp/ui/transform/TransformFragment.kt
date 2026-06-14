package com.huellitas.huellitasapp.ui.transform

import android.Manifest
import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.huellitas.huellitasapp.R
import com.huellitas.huellitasapp.SupabaseConfig.supabase
import com.huellitas.huellitasapp.databinding.FragmentTransformBinding
import com.huellitas.huellitasapp.modelos.Mascota
import com.huellitas.huellitasapp.modelos.Paseos // Tu nuevo modelo acoplado a la BD
import com.huellitas.huellitasapp.modelos.Usuario
import com.huellitas.huellitasapp.util.Constants
import com.huellitas.huellitasapp.util.TouchableWrapper
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TransformFragment : Fragment(), OnMapReadyCallback, GoogleMap.OnMarkerDragListener {

    private var _binding: FragmentTransformBinding? = null
    private val binding get() = _binding!!

    // Google Maps y Ubicación
    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var markerRecojo: Marker? = null
    private var circleRadio: Circle? = null

    // Atributos de control en tiempo real
    private var latitudRecojo: Double = -12.019650
    private var longitudRecojo: Double = -76.985287
    private var radioSeguridadMetros: Int = 300
    private var precioFinalPaseo: Double = 0.0

    // Gestión del Tiempo e Instancias Calendario
    private val calendarioControl = Calendar.getInstance()
    private val formatoFechaVisual = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val formatoHoraVisual = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val formatoBaseDatos = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    // Listas de datos para Mascotas
    private val listaMascotasNombres = mutableListOf<String>()
    private val listaMascotasIds = mutableListOf<String>()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineLocationGranted || coarseLocationGranted) {
            obtenerUbicacionActual()
        } else {
            Toast.makeText(requireContext(), "Permiso de ubicación denegado. Usando base.", Toast.LENGTH_LONG).show()
            colocarPinYRadioEnMapa()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransformBinding.inflate(inflater, container, false)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        val mTouchView = TouchableWrapper(requireContext())
        mTouchView.addView(binding.root)

        mTouchView.onTouchAction = { action ->
            when (action) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                    binding.root.requestDisallowInterceptTouchEvent(true)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    binding.root.requestDisallowInterceptTouchEvent(false)
                }
            }
        }
        return mTouchView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        configurarSpinnerTiempo()
        setupSpinnerDistritos()
        setupFechaHoraPickers() // Inicializa selectores con fecha de hoy
        cargarMascotasDesdeSupabase()
        cargarDatosPredeterminadosUsuario()

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)

        binding.btnSolicitarPaseo.setOnClickListener {
            procesarSolicitudPaseo()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.uiSettings.isScrollGesturesEnabled = true
        mMap.setOnMarkerDragListener(this)

        verificarPermisosYMovilizar()

        binding.seekBarRadio.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                radioSeguridadMetros = if (progress < 100) 100 else progress
                binding.lblRadioMetros.text = if (radioSeguridadMetros >= 1000) {
                    String.format("Radio: %.1f km ", radioSeguridadMetros / 1000.0)
                } else {
                    "Radio: ${radioSeguridadMetros}m "
                }
                circleRadio?.radius = radioSeguridadMetros.toDouble()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    // --- MANEJO DE SELECCIÓN DE TIEMPO (PICKERS) ---
    private fun setupFechaHoraPickers() {
        // Cargar fecha y hora de este instante por defecto en los inputs visuales
        binding.etFechaPaseo.setText(formatoFechaVisual.format(calendarioControl.time))
        binding.etHoraPaseo.setText(formatoHoraVisual.format(calendarioControl.time))

        // Al hacer clic al EditText de fecha
        binding.etFechaPaseo.setOnClickListener {
            val datePickerDialog = DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    calendarioControl.set(Calendar.YEAR, year)
                    calendarioControl.set(Calendar.MONTH, month)
                    calendarioControl.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    binding.etFechaPaseo.setText(formatoFechaVisual.format(calendarioControl.time))
                },
                calendarioControl.get(Calendar.YEAR),
                calendarioControl.get(Calendar.MONTH),
                calendarioControl.get(Calendar.DAY_OF_MONTH)
            )
            // Bloquea los días anteriores al de hoy restando 1 segundo al tiempo actual
            datePickerDialog.datePicker.minDate = System.currentTimeMillis() - 1000
            datePickerDialog.show()
        }

        // Al hacer clic al EditText de hora
        binding.etHoraPaseo.setOnClickListener {
            val timePickerDialog = TimePickerDialog(
                requireContext(),
                { _, hourOfDay, minute ->
                    calendarioControl.set(Calendar.HOUR_OF_DAY, hourOfDay)
                    calendarioControl.set(Calendar.MINUTE, minute)
                    binding.etHoraPaseo.setText(formatoHoraVisual.format(calendarioControl.time))
                },
                calendarioControl.get(Calendar.HOUR_OF_DAY),
                calendarioControl.get(Calendar.MINUTE),
                true // Formato de 24 horas activo
            )
            timePickerDialog.show()
        }
    }

    private fun setupSpinnerDistritos() {
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            Constants.distritosLima
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerDistritos.adapter = adapter
    }

    private fun cargarDatosPredeterminadosUsuario() {
        val userAuth = supabase.auth.currentUserOrNull() ?: return

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val datos = withContext(Dispatchers.IO) {
                    supabase.from("usuarios")
                        .select { filter { eq("id", userAuth.id) } }
                        .decodeSingle<Usuario>()
                }

                binding.etDireccionDetalle.setText(datos.direccion ?: "")

                val distritoGuardado = datos.distrito
                if (!distritoGuardado.isNullOrEmpty()) {
                    val index = Constants.distritosLima.indexOfFirst {
                        it.trim().equals(distritoGuardado.trim(), ignoreCase = true)
                    }

                    if (index >= 0) {
                        binding.spinnerDistritos.post {
                            binding.spinnerDistritos.setSelection(index)
                        }
                    }
                }

            } catch (e: Exception) {
                Log.e("HUELLITAS_ERROR", "Error al jalar datos predeterminados: ${e.message}")
            }
        }
    }

    // --- MANEJO DE UBICACIÓN GPS ---
    private fun verificarPermisosYMovilizar() {
        val tieneFine = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val tieneCoarse = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (tieneFine || tieneCoarse) {
            obtenerUbicacionActual()
        } else {
            requestPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun obtenerUbicacionActual() {
        mMap.isMyLocationEnabled = true
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                latitudRecojo = location.latitude
                longitudRecojo = location.longitude
            }
            colocarPinYRadioEnMapa()
        }.addOnFailureListener {
            colocarPinYRadioEnMapa()
        }
    }

    private fun colocarPinYRadioEnMapa() {
        val posicionFinal = LatLng(latitudRecojo, longitudRecojo)
        if (markerRecojo == null) {
            markerRecojo = mMap.addMarker(
                MarkerOptions()
                    .position(posicionFinal)
                    .title("Punto de Recojo")
                    .snippet("Mantén presionado 1 segundo para reubicar")
                    .draggable(true)
            )
        } else {
            markerRecojo?.position = posicionFinal
        }
        dibujarRadioSeguridad(posicionFinal)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(posicionFinal, 15f))
    }

    override fun onMarkerDragStart(marker: Marker) {}
    override fun onMarkerDrag(marker: Marker) {
        circleRadio?.center = marker.position
    }
    override fun onMarkerDragEnd(marker: Marker) {
        latitudRecojo = marker.position.latitude
        longitudRecojo = marker.position.longitude
        circleRadio?.center = marker.position
        Toast.makeText(requireContext(), "Ubicación de recojo fijada", Toast.LENGTH_SHORT).show()
    }

    private fun dibujarRadioSeguridad(centro: LatLng) {
        circleRadio?.remove()
        val circleOptions = CircleOptions()
            .center(centro)
            .radius(radioSeguridadMetros.toDouble())
            .strokeColor(Color.parseColor("#FF2196F3"))
            .fillColor(Color.parseColor("#222196F3"))
            .strokeWidth(4f)
        circleRadio = mMap.addCircle(circleOptions)
    }

    private fun configurarSpinnerTiempo() {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, Constants.opcionesTiempoTexto)
        binding.spinnerTiempo.adapter = adapter
        binding.spinnerTiempo.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val minutes = Constants.obtenerMinutosPorPosicion(position)
                precioFinalPaseo = if (position == 6) 35.00 else (minutes / 30.0) * 10.00
                binding.lblPrecioCalculado.text = String.format("El precio estimado por el paseo es: S/ %.2f", precioFinalPaseo)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun cargarMascotasDesdeSupabase() {
        listaMascotasNombres.clear()
        listaMascotasIds.clear()
        listaMascotasNombres.add("Selecciona tu mascota...")
        listaMascotasIds.add("")

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val userAuth = supabase.auth.currentUserOrNull() ?: return@launch
                val mascotasDeBD = withContext(Dispatchers.IO) {
                    supabase.from("mascotas")
                        .select { filter { eq("owner_id", userAuth.id) } }
                        .decodeList<Mascota>()
                }
                mascotasDeBD.forEach { mascota ->
                    listaMascotasNombres.add("${mascota.nombre} ${if (!mascota.raza.isNullOrEmpty()) "(${mascota.raza})" else ""}")
                    listaMascotasIds.add(mascota.id ?: "")
                }
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, listaMascotasNombres)
                binding.spinnerMascotas.adapter = adapter
            } catch (e: Exception) {
                Log.e("HUELLITAS_ERROR", "Error al traer mascotas: ${e.message}")
            }
        }
    }

    // --- ACCIÓN REAL DEL BOTÓN CONFIRMAR PASEO ---
    private fun procesarSolicitudPaseo() {
        val posicionMascota = binding.spinnerMascotas.selectedItemPosition
        if (posicionMascota <= 0) {
            Toast.makeText(requireContext(), "Por favor, selecciona una mascota", Toast.LENGTH_SHORT).show()
            return
        }

        val distritoSeleccionado = binding.spinnerDistritos.selectedItem.toString()
        val direccionDetalle = binding.etDireccionDetalle.text.toString().trim()

        if (direccionDetalle.isEmpty()) {
            Toast.makeText(requireContext(), "Por favor, ingresa la dirección detallada", Toast.LENGTH_SHORT).show()
            return
        }

        // Parsear el estado de la instancia Calendar a string ISO compatible con PostgreSQL timestamps
        val stringFechaFinalBD = formatoBaseDatos.format(calendarioControl.time)

        val idMascotaString = listaMascotasIds[posicionMascota]
        val posicionTiempo = binding.spinnerTiempo.selectedItemPosition
        val minutes = Constants.obtenerMinutosPorPosicion(posicionTiempo)
        val tipoPago = if (binding.rgTipoPago.checkedRadioButtonId == R.id.rbYape) Constants.PAGO_YAPE else if (binding.rgTipoPago.checkedRadioButtonId == R.id.rbPlin) Constants.PAGO_PLIN else Constants.PAGO_EFECTIVO
        val notas = binding.etNotas.text.toString().trim()

        // 1. Instanciar la data class en plural tal cual está mapeada en tu backend
        val nuevoPaseo = Paseos(
            mascota_id = idMascotaString,
            distrito_paseo = distritoSeleccionado,
            direccion_paseo = direccionDetalle,
            precio = precioFinalPaseo,
            tiempo_paseo = minutes,
            tipo_pago = tipoPago,
            latitud_recojo = latitudRecojo,
            longitud_recojo = longitudRecojo,
            radio_seguridad_metros = radioSeguridadMetros,
            notas = if (notas.isEmpty()) null else notas,
            fecha_paseo = stringFechaFinalBD, // Insertamos la marca temporal programada
            estado = "pendiente"
        )

        // 2. Inhabilitar el disparador para mitigar clicks rápidos por lag de red
        binding.btnSolicitarPaseo.isEnabled = false

        // 3. Ejecutar corrutina asíncrona en el hilo IO para persistir en Supabase
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    supabase.from("paseos").insert(nuevoPaseo)
                }

                // 1. Mostrar el Toast en el hilo principal de la UI
                Toast.makeText(requireContext(), "¡Paseo solicitado exitosamente!", Toast.LENGTH_LONG).show()

                // 2. Navegación explícita y limpia hacia el Historial (SlideshowFragment)
                val navController = androidx.navigation.Navigation.findNavController(requireView())

                // Reemplaza 'R.id.nav_slideshow' por el ID exacto que tenga tu SlideshowFragment en mobile_navigation.xml
                navController.navigate(R.id.nav_slideshow)

            } catch (e: Exception) {
                Log.e("HUELLITAS_ERROR", "Error de inserción en tabla paseos: ${e.message}")
                Toast.makeText(requireContext(), "Error de conexión al procesar orden", Toast.LENGTH_SHORT).show()
                binding.btnSolicitarPaseo.isEnabled = true
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}