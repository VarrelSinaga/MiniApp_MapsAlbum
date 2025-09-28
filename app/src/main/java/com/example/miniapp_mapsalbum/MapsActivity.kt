package com.example.miniapp_mapsalbum

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.PopupMenu
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
import com.google.android.gms.maps.model.MarkerOptions

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    private val fusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(this)
    }

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
    }

    // Tampilkan menu pilihan jenis peta
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

                            // Geser kamera ke lokasi user
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15f))

                            // Tambah marker lokasi user
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
