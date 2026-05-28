package com.huellitas.huellitasapp.ui.mascotas

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.datepicker.MaterialDatePicker
import com.huellitas.huellitasapp.SupabaseConfig.supabase
import com.huellitas.huellitasapp.databinding.FragmentAddMascotaBinding
import com.huellitas.huellitasapp.modelos.Mascota
import com.huellitas.huellitasapp.util.Constants
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*

class AddMascotaFragment : Fragment() {

    private var _binding: FragmentAddMascotaBinding? = null
    private val binding get() = _binding!!

    // Variable para guardar la URI de la imagen seleccionada localmente
    private var imageUri: Uri? = null

    // Configuración del selector de imágenes de la galería
    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            imageUri = it
            binding.ivMascotaPreview.setImageURI(it) // Muestra la previa en el círculo
            binding.ivMascotaPreview.setPadding(0, 0, 0, 0) // Quita el padding del icono de cámara
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAddMascotaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Cargar datos en los Spinners desde Constants
        setupSpinners()

        // 2. Listener para abrir la galería
        binding.btnSeleccionarFoto.setOnClickListener {
            pickImage.launch("image/*")
        }

        // 3. Listener para el calendario (DatePicker)
        binding.etFechaNacimiento.setOnClickListener {
            mostrarDatePicker()
        }

        // 4. Listener del botón principal para guardar todo
        binding.btnGuardarMascota.setOnClickListener {
            validarYGuardar()
        }
    }

    private fun setupSpinners() {
        val adapterGen = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, Constants.generoMascota)
        binding.spnGenero.adapter = adapterGen

        val adapterTam = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, Constants.tamanioMascota)
        binding.spnTamanio.adapter = adapterTam

        val adapterComp = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, Constants.comportamientoMascota)
        binding.spnComportamiento.adapter = adapterComp
    }

    private fun mostrarDatePicker() {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Fecha de Nacimiento")
            .build()

        picker.addOnPositiveButtonClickListener { selection ->
            val dateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(selection))
            binding.etFechaNacimiento.setText(dateString)
        }
        picker.show(parentFragmentManager, "DATE_PICKER")
    }

    private fun validarYGuardar() {
        val nombre = binding.etNombreMascota.text.toString()
        if (nombre.isEmpty()) {
            binding.tilNombre.error = "Ingresa el nombre"
            return
        }

        // Mostramos el ProgressBar y bloqueamos el botón para evitar clics dobles
        binding.progressBar.visibility = View.VISIBLE
        binding.btnGuardarMascota.isEnabled = false

        lifecycleScope.launch {
            try {
                var urlFinal: String? = null

                // Si el usuario seleccionó una imagen, la procesamos
                imageUri?.let { uri ->
                    val bytes = comprimirImagen(uri)
                    urlFinal = subirImagenASupabase(nombre, bytes)
                }

                // Guardamos en la base de datos (PostgreSQL)
                guardarEnBaseDeDatos(nombre, urlFinal)

            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                binding.progressBar.visibility = View.GONE
                binding.btnGuardarMascota.isEnabled = true
            }
        }
    }

    // Acción de optimización: Reduce dimensiones y calidad para no saturar la DB
    private suspend fun comprimirImagen(uri: Uri): ByteArray = withContext(Dispatchers.IO) {
        val inputStream = requireContext().contentResolver.openInputStream(uri)
        val originalBitmap = BitmapFactory.decodeStream(inputStream)

        // Redimensionamos a un máximo de 800px de ancho para ahorrar espacio
        val anchoFinal = 800
        val altoFinal = (originalBitmap.height * (anchoFinal.toDouble() / originalBitmap.width)).toInt()
        val bitmapReducido = Bitmap.createScaledBitmap(originalBitmap, anchoFinal, altoFinal, true)

        val outputStream = ByteArrayOutputStream()
        // Comprimimos al 80% de calidad en formato WebP (más ligero que JPEG)
        bitmapReducido.compress(Bitmap.CompressFormat.WEBP, 80, outputStream)
        outputStream.toByteArray()
    }

    // Acción de Storage: Sube el archivo al Bucket y devuelve la URL pública
    private suspend fun subirImagenASupabase(nombre: String, bytes: ByteArray): String {
        val fileName = "pet_${nombre.lowercase()}_${System.currentTimeMillis()}.webp"
        val bucket = supabase.storage.from("fotos_mascotas") // <--- Asegúrate de que este bucket exista en Supabase

        bucket.upload(fileName, bytes)
        return bucket.publicUrl(fileName)
    }

    // Acción de Base de Datos: Inserta el registro final
    private suspend fun guardarEnBaseDeDatos(nombre: String, fotoUrl: String?) {
        val userId = supabase.auth.currentUserOrNull()?.id ?: return

        val nuevaMascota = Mascota(
            owner_id = userId,
            nombre = nombre,
            raza = binding.etRazaMascota.text.toString(),
            fecha_nacimiento = binding.etFechaNacimiento.text.toString(),
            genero = binding.spnGenero.selectedItem.toString(),
            tamano = binding.spnTamanio.selectedItem.toString(),
            comportamiento = binding.spnComportamiento.selectedItem.toString(),
            descripcion_comportamiento = binding.etDescripcionComportamiento.text.toString(),
            foto_url = fotoUrl // Guardamos el link de la imagen
        )

        supabase.from("mascotas").insert(nuevaMascota)

        withContext(Dispatchers.Main) {
            Toast.makeText(context, "¡$nombre registrado!", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack() // Regresa a la lista
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}