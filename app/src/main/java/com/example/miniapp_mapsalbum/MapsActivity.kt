package com.example.miniapp_mapsalbum

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.EditText
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
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import java.util.*

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

        // init Places API (pakai API key dari strings.xml)
        if (!Places.isInitialized()) {
            Places.initialize(
                applicationContext,
                getString(R.string.google_maps_key),
                Locale.getDefault()
            )
        }

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (isGranted) {
                    getLastLocation()
                } else {
                    showPermissionRationale {
                        requestPermissionLauncher.launch(ACCESS_FINE_LOCATION)
                    }
                }
            }

        setupSearch()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isZoomControlsEnabled = true   // tombol zoom aktif
        mMap.uiSettings.isMapToolbarEnabled = true

        // cek permission lokasi
        when {
            hasLocationPermission() -> getLastLocation()
            shouldShowRequestPermissionRationale(ACCESS_FINE_LOCATION) -> {
                showPermissionRationale {
                    requestPermissionLauncher.launch(ACCESS_FINE_LOCATION)
                }
            }
            else -> requestPermissionLauncher.launch(ACCESS_FINE_LOCATION)
        }

        // klik map → input nama marker
        mMap.setOnMapClickListener { latLng ->
            val editText = EditText(this)
            editText.hint = "Nama spot"

            AlertDialog.Builder(this)
                .setTitle("Tambah Marker")
                .setMessage("Masukkan nama untuk marker ini:")
                .setView(editText)
                .setPositiveButton("OK") { _, _ ->
                    val namaSpot = editText.text.toString().ifEmpty { "Spot Baru" }
                    addSpotMarker(latLng, namaSpot)
                }
                .setNegativeButton("Batal", null)
                .show()
        }

        // marker bisa digeser
        mMap.setOnMarkerDragListener(object : GoogleMap.OnMarkerDragListener {
            override fun onMarkerDragStart(marker: Marker) {}
            override fun onMarkerDrag(marker: Marker) {}
            override fun onMarkerDragEnd(marker: Marker) {
                Log.d("MapsActivity", "Marker dipindah ke: ${marker.position}")
            }
        })
    }

    private fun setupSearch() {
        val autocompleteFragment =
            supportFragmentManager.findFragmentById(R.id.autocomplete_fragment) as AutocompleteSupportFragment

        autocompleteFragment.setPlaceFields(
            listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG)
        )

        autocompleteFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                place.latLng?.let { latLng ->
                    Log.d("MapsActivity", "Place: ${place.name}, ${latLng.latitude}, ${latLng.longitude}")
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                    addSpotMarker(latLng, place.name ?: "Lokasi")
                }
            }

            override fun onError(status: com.google.android.gms.common.api.Status) {
                Log.e("MapsActivity", "Autocomplete error: $status")
            }
        })
    }

    private fun hasLocationPermission() =
        ContextCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun showPermissionRationale(positiveAction: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle("Location permission")
            .setMessage("App ini butuh lokasi agar bisa jalan.")
            .setPositiveButton(android.R.string.ok) { _, _ -> positiveAction() }
            .setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.dismiss() }
            .create()
            .show()
    }

    private fun getLastLocation() {
        if (hasLocationPermission()) {
            try {
                fusedLocationProviderClient.lastLocation
                    .addOnSuccessListener { location: Location? ->
                        location?.let {
                            val userLocation = LatLng(it.latitude, it.longitude)
                            updateMapLocation(userLocation)
                            addUserMarker(userLocation) // default marker
                        }
                    }
            } catch (e: SecurityException) {
                Log.e("MapsActivity", "getLastLocation error: ${e.message}")
            }
        } else {
            requestPermissionLauncher.launch(ACCESS_FINE_LOCATION)
        }
    }

    private fun updateMapLocation(location: LatLng) {
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))
    }

    // convert vector drawable ke BitmapDescriptor dengan ukuran default marker (48dp)
    private fun vectorToBitmap(drawableId: Int): BitmapDescriptor {
        val vectorDrawable = ContextCompat.getDrawable(this, drawableId)!!

        val defaultSizeDp = 48
        val density = resources.displayMetrics.density
        val width = (defaultSizeDp * density).toInt()
        val height = (defaultSizeDp * density).toInt()

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        vectorDrawable.setBounds(0, 0, canvas.width, canvas.height)
        vectorDrawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    // marker lokasi user → default icon Google Maps
    private fun addUserMarker(location: LatLng) {
        mMap.addMarker(
            MarkerOptions()
                .position(location)
                .title("Lokasi Kamu")
                .draggable(false)
        )
    }

    // marker spot baru → icon restaurant dengan ukuran sama default
    private fun addSpotMarker(location: LatLng, title: String) {
        val icon = vectorToBitmap(R.drawable.ic_restaurant)
        mMap.addMarker(
            MarkerOptions()
                .position(location)
                .title(title)
                .icon(icon)
                .draggable(true)
        )
    }
}
