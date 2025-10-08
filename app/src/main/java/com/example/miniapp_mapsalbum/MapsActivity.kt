package com.example.miniapp_mapsalbum

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.miniapp_mapsalbum.data.Memory
import com.example.miniapp_mapsalbum.data.MemoryDatabase
import com.example.miniapp_mapsalbum.databinding.ActivityMapsBinding
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolygonOptions
import com.google.android.gms.maps.model.PolylineOptions
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

// DIUBAH: Implementasikan listener dari KEDUA dialog
class MapsActivity : AppCompatActivity(), OnMapReadyCallback,
    EditMarkerInfoDialogFragment.EditMarkerInfoListener,
    MarkerDetailsDialogFragment.MarkerDetailsListener {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var pickImageLauncher: ActivityResultLauncher<String>

    private val fusedLocationProviderClient by lazy { LocationServices.getFusedLocationProviderClient(this) }
    private val db by lazy { MemoryDatabase.getDatabase(this).memoryDao() }

    // BARU: Map untuk menyimpan referensi Marker berdasarkan ID Memory
    private val savedMemories = mutableListOf<Memory>() // Untuk menyimpan data dari DB
    companion object {
        private const val MIN_DISTANCE_METERS = 30.0
    }
    private var myLocationMarker: Marker? = null
    private val markerMap = mutableMapOf<Long, Marker>()
    private var memoryToUpdateImage: Memory? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        binding.btnMapType.setOnClickListener { showMapTypeMenu(it as Button) }
        setupLaunchers()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isZoomControlsEnabled = true
        checkLocationPermission()
        setupMapListeners()

        // BARU: Muat semua kenangan dari database saat peta siap
        loadAndDisplayAllMemories()
    }

    private fun setupLaunchers() {
        requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (isGranted) getLastLocation()
                else showPermissionRationale { requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) }
            }

        pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                val currentMemory = memoryToUpdateImage ?: return@let
                val filePath = copyImageToInternalStorage(it, "mem_${System.currentTimeMillis()}.jpg")
                if (filePath != null) {
                    currentMemory.imagePath = filePath
                    val dialog = supportFragmentManager.findFragmentByTag("EditMarkerInfoDialog") as? EditMarkerInfoDialogFragment
                    dialog?.updateImageView(filePath)
                } else {
                    Toast.makeText(this, "Gagal menyimpan gambar.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupMapListeners() {
        mMap.setOnMapLongClickListener { latLng ->
            // This part stays the same: create a new memory on long press
            val newMemory = Memory(
                latitude = latLng.latitude,
                longitude = latLng.longitude
            )
            EditMarkerInfoDialogFragment.newInstance(newMemory).show(supportFragmentManager, "EditMarkerInfoDialog")
        }

        // DIUBAH: Tambahkan logika untuk menangani klik pada marker "Lokasi Saya"
        mMap.setOnMarkerClickListener { marker ->
            val memoryId = marker.tag as? Long ?: return@setOnMarkerClickListener false

            if (memoryId == -1L) {
                // Ini adalah marker "Lokasi Saya"
                // Buat objek Memory baru di posisi marker ini
                val newMemory = Memory(
                    latitude = marker.position.latitude,
                    longitude = marker.position.longitude,
                    title = "Kenangan di Lokasi Saya"
                )
                // Buka dialog edit untuk membuat kenangan baru
                EditMarkerInfoDialogFragment.newInstance(newMemory).show(supportFragmentManager, "EditMarkerInfoDialog")
            } else {
                // Ini adalah marker kenangan yang sudah disimpan
                lifecycleScope.launch {
                    val memory = db.getMemoryById(memoryId)
                    memory?.let {
                        MarkerDetailsDialogFragment.newInstance(it).show(supportFragmentManager, "MarkerDetailsDialog")
                    }
                }
            }
            true // Event sudah ditangani
        }
    }

    private fun loadAndDisplayAllMemories() {
        lifecycleScope.launch {
            val memoriesFromDb = db.getAllMemoriesList()

            // BARU: Simpan data ke list kita
            savedMemories.clear()
            savedMemories.addAll(memoriesFromDb)

            mMap.clear()
            markerMap.clear()
            for (memory in savedMemories) { // Gunakan savedMemories di sini
                addMarkerToMap(memory)
            }

            // Panggil getLastLocation() lagi setelah data dimuat untuk mengecek jarak
            getLastLocation()
            val umnToBethsaida = PolylineOptions()
                .add(LatLng(-6.256718, 106.618209))
                .add(LatLng(-6.255982, 106.618434))
                .add(LatLng(-6.256061, 106.621020))
                .add(LatLng(-6.254611, 106.622085))
                .add(LatLng(-6.254752, 106.622383))
                .color(Color.RED)
                .width(10.0f)
            val u2bPolyline = mMap.addPolyline(umnToBethsaida)

            // Tambahkan Polyline dari UMN ke SDC
            val umnToSdc = PolylineOptions()
                .add(LatLng(-6.256718, 106.618209))
                .add(LatLng(-6.256166, 106.618363))
                .add(LatLng(-6.256251, 106.617400))
                .add(LatLng(-6.255877, 106.616238))
                .add(LatLng(-6.256302, 106.616085))
                .color(Color.GREEN)
                .width(10.0f)
            val u2sdcPolyline = mMap.addPolyline(umnToSdc)


            // Tambahkan Polygon di UMN
            val umnCampus = PolygonOptions()
                .add(LatLng(-6.256302, 106.617534))
                .add(LatLng(-6.256099, 106.619744))
                .add(LatLng(-6.256558, 106.619851))
                .add(LatLng(-6.259374, 106.618639))
                .add(LatLng(-6.258659, 106.616740))
                .add(LatLng(-6.256302, 106.617534))
                .strokeColor(Color.BLUE)
                .strokeWidth(10.0f)
                .fillColor(Color.argb(20, 0, 255, 255))
            val umnArea = mMap.addPolygon(umnCampus)

            // Tambahkan circle di UMN
            val umn = LatLng(-6.2574591, 106.6183484)
            val circleUmn = CircleOptions()
                .center(umn)
                .radius(500.0)
                .strokeColor(Color.YELLOW)
                .fillColor(Color.argb(30, 255, 255, 0))
            val UMNAreaIn500m = mMap.addCircle(circleUmn)
        }
    }

    private fun addMarkerToMap(memory: Memory) {
        val marker = mMap.addMarker(
            MarkerOptions()
                .position(LatLng(memory.latitude, memory.longitude))
                .title(memory.title)
                .icon(
                    if (memory.imagePath != null) BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET)
                    else BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
                )
        )
        marker?.tag = memory.id
        if (marker != null) {
            markerMap[memory.id] = marker
        }
    }

    // --- Implementasi Listener ---

    override fun onMemorySaved(memory: Memory) {
        lifecycleScope.launch {
            if (memory.id == 0L) {
                myLocationMarker?.remove()
                val newId = db.insertMemory(memory)
                val newMemory = memory.copy(id = newId)

                savedMemories.add(newMemory) // BARU: Tambahkan ke list

                addMarkerToMap(newMemory)
                Toast.makeText(this@MapsActivity, "Kenangan '${newMemory.title}' disimpan!", Toast.LENGTH_SHORT).show()
            } else {
                db.updateMemory(memory)

                // BARU: Update juga di list
                val index = savedMemories.indexOfFirst { it.id == memory.id }
                if (index != -1) savedMemories[index] = memory

                markerMap[memory.id]?.remove()
                addMarkerToMap(memory)
                Toast.makeText(this@MapsActivity, "Kenangan '${memory.title}' diperbarui!", Toast.LENGTH_SHORT).show() // <-- FIXED
            }
        }
    }

    override fun onMemoryDeleted(memory: Memory) {
        lifecycleScope.launch {
            // Kode yang sudah ada untuk menghapus data
            db.deleteMemory(memory)
            savedMemories.removeAll { it.id == memory.id }
            markerMap[memory.id]?.remove()
            markerMap.remove(memory.id)
            Toast.makeText(this@MapsActivity, "Kenangan '${memory.title}' dihapus!", Toast.LENGTH_SHORT).show()

            // BARU: Panggil kembali pengecekan lokasi untuk menampilkan marker "Lokasi Saya" jika perlu
            getLastLocation()
        }
    }

    override fun onPickImageClicked(memoryToEdit: Memory?) {
        this.memoryToUpdateImage = memoryToEdit
        pickImageLauncher.launch("image/*")
    }

    override fun onEditMemoryClicked(memory: Memory) {
        EditMarkerInfoDialogFragment.newInstance(memory).show(supportFragmentManager, "EditMarkerInfoDialog")
    }


    // --- Fungsi Helper & Izin ---

    private fun copyImageToInternalStorage(uri: Uri, fileName: String): String? {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            val file = File(filesDir, fileName)
            val outputStream = FileOutputStream(file)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()
            file.absolutePath
        } catch (e: Exception) {
            Log.e("MapsActivity", "Error copying image", e)
            null
        }
    }

    // (Fungsi-fungsi lain seperti getLastLocation, showMapTypeMenu, dll tetap sama seperti kode Anda sebelumnya)
    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            getLastLocation()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun getLastLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.isMyLocationEnabled = true
            fusedLocationProviderClient.lastLocation.addOnSuccessListener { currentLocation: Location? ->
                currentLocation?.let {
                    val userLatLng = LatLng(it.latitude, it.longitude)
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 18f)) // Zoom lebih dekat

                    // --- LOGIKA PENGECEKAN JARAK ---
                    var isCloseToExistingMemory = false
                    for (memory in savedMemories) {
                        val memoryLocation = Location("").apply {
                            latitude = memory.latitude
                            longitude = memory.longitude
                        }
                        // Hitung jarak dalam meter
                        val distance = currentLocation.distanceTo(memoryLocation)

                        if (distance < MIN_DISTANCE_METERS) {
                            isCloseToExistingMemory = true
                            break // Ditemukan kenangan terdekat, hentikan pencarian
                        }
                    }

                    // Hapus marker "Lokasi Saya" yang mungkin sudah ada
                    myLocationMarker?.remove()

                    // Hanya tampilkan marker "Lokasi Saya" jika tidak ada kenangan lain di dekatnya
                    if (!isCloseToExistingMemory) {
                        myLocationMarker = mMap.addMarker(
                            MarkerOptions()
                                .position(userLatLng)
                                .title("Lokasi Saya (Tambah Kenangan)")
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                        )
                        myLocationMarker?.tag = -1L
                    }
                    // --- AKHIR LOGIKA PENGECEKAN JARAK ---
                }
            }
        }
    }


    private fun showMapTypeMenu(anchor: Button) {
        val popup = PopupMenu(this, anchor)
        popup.menuInflater.inflate(R.menu.map_types_menu, popup.menu) // Anggap Anda punya R.menu.map_types_menu
        popup.setOnMenuItemClickListener { item ->
            mMap.mapType = when (item.itemId) {
                R.id.normal_map -> GoogleMap.MAP_TYPE_NORMAL
                R.id.hybrid_map -> GoogleMap.MAP_TYPE_HYBRID
                R.id.satellite_map -> GoogleMap.MAP_TYPE_SATELLITE
                R.id.terrain_map -> GoogleMap.MAP_TYPE_TERRAIN
                else -> mMap.mapType
            }
            true
        }
        popup.show()
    }

    private fun showPermissionRationale(positiveAction: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle("Izin Lokasi")
            .setMessage("Aplikasi ini membutuhkan izin lokasi untuk menampilkan posisi Anda di peta.")
            .setPositiveButton("OK") { _, _ -> positiveAction() }
            .setNegativeButton("Batal") { dialog, _ -> dialog.dismiss() }
            .create().show()
    }
}