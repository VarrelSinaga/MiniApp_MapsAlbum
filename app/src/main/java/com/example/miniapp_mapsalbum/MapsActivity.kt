package com.example.miniapp_mapsalbum

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.miniapp_mapsalbum.databinding.ActivityMapsBinding
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var pickImageLauncher: ActivityResultLauncher<String>

    private val fusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(this)
    }

    private var currentMarker: Marker? = null
    private val markerImages = mutableMapOf<Marker, Uri?>() // menyimpan foto tiap marker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Ambil map fragment
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Tombol pilih jenis peta
        binding.btnMapType.setOnClickListener { showMapTypeMenu(it as Button) }

        // Registrasi permission launcher
        requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (isGranted) getLastLocation()
                else showPermissionRationale { requestPermissionLauncher.launch(ACCESS_FINE_LOCATION) }
            }

        // Registrasi launcher untuk pilih gambar
        pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                currentMarker?.let { marker ->
                    markerImages[marker] = it
                }
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // === Atur UI controls & gesture map ===
        val uiSettings = mMap.uiSettings
        uiSettings.isZoomControlsEnabled = true
        uiSettings.isCompassEnabled = true
        uiSettings.isMapToolbarEnabled = true
        uiSettings.isZoomGesturesEnabled = true
        uiSettings.isScrollGesturesEnabled = true
        uiSettings.isTiltGesturesEnabled = true
        uiSettings.isRotateGesturesEnabled = true

        // Cek izin lokasi
        when {
            hasLocationPermission() -> getLastLocation()
            shouldShowRequestPermissionRationale(ACCESS_FINE_LOCATION) -> {
                showPermissionRationale { requestPermissionLauncher.launch(ACCESS_FINE_LOCATION) }
            }
            else -> requestPermissionLauncher.launch(ACCESS_FINE_LOCATION)
        }

        // Listener long press map untuk menambah marker
        mMap.setOnMapLongClickListener { latLng ->
            mMap.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title("Marker Baru")
                    .snippet("Lat: ${latLng.latitude}, Lng: ${latLng.longitude}")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
            )
        }

        // Listener klik marker untuk edit info (pop up nama & foto)
        mMap.setOnMarkerClickListener { marker ->
            showMarkerEditDialog(marker)
            true
        }
    }

    private fun showMarkerEditDialog(marker: Marker) {
        currentMarker = marker
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Edit Marker Info")

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        val padding = 50
        layout.setPadding(padding, padding, padding, padding)

        // EditText untuk nama marker
        val inputTitle = EditText(this)
        inputTitle.hint = "Nama marker"
        inputTitle.setText(marker.title ?: "")
        inputTitle.setPadding(0, 20, 0, 20)

        // TextView koordinat
        val infoLatLng = TextView(this)
        infoLatLng.text = "Lat: ${marker.position.latitude}, Lng: ${marker.position.longitude}"
        infoLatLng.setPadding(0, 20, 0, 20)

        // ImageView preview foto marker
        val imageView = ImageView(this)
        imageView.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            400
        )
        imageView.setPadding(0, 20, 0, 20)
        markerImages[marker]?.let { imageView.setImageURI(it) }

        // Tombol pilih foto
        val btnPickImage = Button(this)
        btnPickImage.text = "Pilih Foto"
        btnPickImage.setOnClickListener {
            currentMarker = marker
            pickImageLauncher.launch("image/*")
        }

        // Tambahkan view ke layout
        layout.addView(inputTitle)
        layout.addView(infoLatLng)
        layout.addView(imageView)
        layout.addView(btnPickImage)

        builder.setView(layout)

        builder.setPositiveButton("Simpan") { dialog, _ ->
            val title = inputTitle.text.toString()
            if (title.isNotEmpty()) marker.title = title
            marker.showInfoWindow()
            dialog.dismiss()
        }

        builder.setNegativeButton("Batal") { dialog, _ -> dialog.dismiss() }

        builder.create().show()
    }

    private fun showMapTypeMenu(anchor: Button) {
        val popup = PopupMenu(this, anchor)
        popup.menu.add("Normal")
        popup.menu.add("Satellite")
        popup.menu.add("Hybrid")
        popup.menu.add("Terrain")

        popup.setOnMenuItemClickListener { item ->
            when (item.title) {
                "Normal" -> mMap.mapType = GoogleMap.MAP_TYPE_NORMAL
                "Satellite" -> mMap.mapType = GoogleMap.MAP_TYPE_SATELLITE
                "Hybrid" -> mMap.mapType = GoogleMap.MAP_TYPE_HYBRID
                "Terrain" -> mMap.mapType = GoogleMap.MAP_TYPE_TERRAIN
            }
            true
        }
        popup.show()
    }

    private fun hasLocationPermission() =
        ContextCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun showPermissionRationale(positiveAction: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle("Location permission")
            .setMessage("This app needs your location to work properly")
            .setPositiveButton(android.R.string.ok) { _, _ -> positiveAction() }
            .setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.dismiss() }
            .create().show()
    }

    private fun getLastLocation() {
        if (hasLocationPermission()) {
            try {
                fusedLocationProviderClient.lastLocation
                    .addOnSuccessListener { location: Location? ->
                        location?.let {
                            val userLocation = LatLng(it.latitude, it.longitude)
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15f))
                            mMap.addMarker(
                                MarkerOptions()
                                    .position(userLocation)
                                    .title("Lokasi Saya")
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                            )
                        }
                    }
            } catch (e: SecurityException) {
                Log.d("MapsActivity", "getLastLocation() SecurityException: ${e.message}")
            }
        } else {
            requestPermissionLauncher.launch(ACCESS_FINE_LOCATION)
        }
    }
}
