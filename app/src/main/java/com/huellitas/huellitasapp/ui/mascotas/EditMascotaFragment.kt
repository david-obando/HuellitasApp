package com.huellitas.huellitasapp.ui.mascotas

import android.app.DatePickerDialog
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import coil.load
import com.huellitas.huellitasapp.SupabaseConfig.supabase
import com.huellitas.huellitasapp.databinding.FragmentEditMascotaBinding
import com.huellitas.huellitasapp.util.Constants // Uso de tus constantes (Género, Tamaño, etc.)
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class EditMascotaFragment : Fragment() {

    private var _binding: FragmentEditMascotaBinding? = null
    private val binding get() = _binding!!

    private var idMascota: String? = null
    private var fotoUrlActual: String? = null
    private var imageUri: Uri? = null

    // Registro para seleccionar imagen de la galería
    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            imageUri = uri
            binding.ivMascotaPreview.load(uri)
            binding.ivMascotaPreview.setPadding(0, 0, 0, 0)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEditMascotaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSpinners()
        setupDatePicker()

        // 1. CARGA DE DATOS: Recuperamos la información enviada desde la lista (Bundle)
        arguments?.let { bundle ->
            fotoUrlActual = bundle.getString("foto_url")
            idMascota = bundle.getString("id")
            binding.etNombreMascota.setText(bundle.getString("nombre"))
            binding.etRazaMascota.setText(bundle.getString("raza"))
            binding.etFechaNacimiento.setText(bundle.getString("fecha_nacimiento"))
            binding.etDescripcionComportamiento.setText(bundle.getString("descripcion_comportamiento"))

            // Sincronizamos los Spinners con los valores actuales de la mascota
            setSpinnerValue(binding.spnGenero, bundle.getString("genero") ?: "")
            setSpinnerValue(binding.spnTamanio, bundle.getString("tamanio") ?: "")
            setSpinnerValue(binding.spnComportamiento, bundle.getString("comportamiento") ?: "")

            if (!fotoUrlActual.isNullOrEmpty()) {
                binding.ivMascotaPreview.load(fotoUrlActual)
                binding.ivMascotaPreview.setPadding(0, 0, 0, 0)
            }
        }

        binding.btnSeleccionarFoto.setOnClickListener { pickImage.launch("image/*") }
        binding.btnActualizarMascota.setOnClickListener { actualizarMascota() }
    }

    private fun setupSpinners() {
        // Llenamos los Spinners usando tus arreglos de Constants.kt
        val adapterGenero = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, Constants.generoMascota)
        val adapterTamanio = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, Constants.tamanioMascota)
        val adapterComportamiento = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, Constants.comportamientoMascota)

        binding.spnGenero.adapter = adapterGenero
        binding.spnTamanio.adapter = adapterTamanio
        binding.spnComportamiento.adapter = adapterComportamiento
    }

    private fun setupDatePicker() {
        binding.etFechaNacimiento.setOnClickListener {
            val c = Calendar.getInstance()
            val year = c.get(Calendar.YEAR)
            val month = c.get(Calendar.MONTH)
            val day = c.get(Calendar.DAY_OF_MONTH)

            val dpd = DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->
                // Formato ISO (YYYY-MM-DD) para que Supabase no dé error de rango
                val mesFormateado = String.format("%02d", selectedMonth + 1)
                val diaFormateado = String.format("%02d", selectedDay)
                val fechaParaBD = "$selectedYear-$mesFormateado-$diaFormateado"

                binding.etFechaNacimiento.setText(fechaParaBD)
            }, year, month, day)

            dpd.show()
        }
    }

    private fun setSpinnerValue(spinner: Spinner, value: String) {
        val adapter = spinner.adapter as ArrayAdapter<String>
        val position = adapter.getPosition(value)
        if (position >= 0) spinner.setSelection(position)
    }

    private fun actualizarMascota() {
        val nombre = binding.etNombreMascota.text.toString().trim()

        if (nombre.isEmpty()) {
            binding.etNombreMascota.error = "El nombre es obligatorio"
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE
                binding.btnActualizarMascota.isEnabled = false

                var urlFinal = fotoUrlActual

                // 2. GESTIÓN DE IMAGEN (Optimización para no saturar el Storage)
                imageUri?.let { uri ->
                    val bytes = withContext(Dispatchers.IO) {
                        requireContext().contentResolver.openInputStream(uri)?.readBytes()
                    }
                    if (bytes != null) {
                        // Usamos un nombre fijo basado en el ID para SOBREESCRIBIR el archivo
                        val fileName = "pet_${idMascota}.webp"
                        val bucket = supabase.storage.from("fotos_mascotas")

                        // 'upsert = true' permite reemplazar el archivo anterior
                        bucket.upload(fileName, bytes, upsert = true)

                        // AGREGAMOS EL PARAMETRO DE TIEMPO (?v=...)
                        // Esto engaña a la caché de Android para que muestre la foto nueva al instante
                        urlFinal = bucket.publicUrl(fileName) + "?v=${System.currentTimeMillis()}"
                    }
                }

                // 3. ACTUALIZACIÓN EN TABLA: Mapeo exacto con tus columnas de Supabase
                supabase.from("mascotas").update(
                    mapOf(
                        "nombre" to nombre,
                        "raza" to binding.etRazaMascota.text.toString(),
                        "fecha_nacimiento" to binding.etFechaNacimiento.text.toString(),
                        "genero" to binding.spnGenero.selectedItem.toString(),
                        "tamano" to binding.spnTamanio.selectedItem.toString(),
                        "comportamiento" to binding.spnComportamiento.selectedItem.toString(),
                        "descripcion_comportamiento" to binding.etDescripcionComportamiento.text.toString(),
                        "foto_url" to urlFinal // Guardamos la URL con el truco de la caché
                    )
                ) {
                    filter { eq("id", idMascota!!) }
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Datos de la mascota actualizados", Toast.LENGTH_SHORT).show()
                    // Regresamos a la lista
                    findNavController().popBackStack()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.btnActualizarMascota.isEnabled = true
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}