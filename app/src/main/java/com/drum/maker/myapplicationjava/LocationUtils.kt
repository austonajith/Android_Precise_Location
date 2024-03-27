package com.drum.maker.myapplicationjava

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority.PRIORITY_BALANCED_POWER_ACCURACY
import com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY
import com.google.android.gms.tasks.CancellationTokenSource

@SuppressLint("MissingPermission")
class LocationUtils(private val activity: AppCompatActivity, val listener: LocationListener) {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationManager: LocationManager
    private var isPreciseLocation = true

    private val locationRequest = LocationRequest.Builder(0)
        .setPriority(PRIORITY_HIGH_ACCURACY)
        .build()

    private val locationPermissionRequest = activity.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrFalse(android.Manifest.permission.ACCESS_FINE_LOCATION) -> {
                Log.d(TAG, "FINE_LOCATION GRANTED")
                isPreciseLocation = true
                getLocation()
            }

            permissions.getOrFalse(android.Manifest.permission.ACCESS_COARSE_LOCATION) -> {
                Log.d(TAG, "COARSE_LOCATION GRANTED")
                isPreciseLocation = false
                getLocation()
            }

            else -> {
                Toast.makeText(
                    activity,
                    "Location permission denied. Please enable in settings",
                    Toast.LENGTH_LONG
                ).show()
                Log.d(TAG, "NONE GRANTED")
                listener.onLocationFetchFailed("Location permission denied")
                // No location access granted.
            }
        }
    }

    private val locationManagerListener = object : android.location.LocationListener {
        override fun onLocationChanged(result: Location) {
            listener.onLocationFetchSuccess(result, isPreciseLocation, "locMgr")
            locationManager.removeUpdates(this)
        }
    }

    private fun getLocation() {
        enableGPS {
            startFetching()
        }
    }

    init {
        setupLocationConfig()
    }

    private fun setupLocationConfig() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity)
        locationManager = activity.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    private fun hasPermissions(vararg permissions: String): Boolean = permissions.all {
        ActivityCompat.checkSelfPermission(activity, it) == PackageManager.PERMISSION_GRANTED
    }


    private fun askPermission() {
        locationPermissionRequest.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }


    private fun startFetching() {
        val priority =
            if (isPreciseLocation) PRIORITY_HIGH_ACCURACY else PRIORITY_BALANCED_POWER_ACCURACY
        val result = fusedLocationClient.getCurrentLocation(
            priority,
            CancellationTokenSource().token
        )
        result.addOnSuccessListener {
            it?.let {
                listener.onLocationFetchSuccess(it, isPreciseLocation, "fusedLoc")
            } ?: kotlin.run {
                Log.e(
                    TAG,
                    "FusedLocationProviderClient API location fetch Failed. Trying to fetch using LocationManager API "
                )
                fetchUsingLocationManagerApi()
            }
        }.addOnFailureListener {
            listener.onLocationFetchFailed("Location Fetch Failed " + it.message)
        }
    }

    private fun fetchUsingLocationManagerApi() {
        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            0L, // Minimum time between updates (ignored for single request)
            0f, // Minimum distance between updates (ignored for single request)
            locationManagerListener
        )
    }

    fun scanLocation() {
        if (!hasPermissions(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        ) {
            askPermission()
            return
        }
        getLocation()
    }

    private fun enableGPS(onEnable: () -> Unit) {
        val settingRequest =
            LocationSettingsRequest.Builder().addLocationRequest(locationRequest).build()
        val client = LocationServices.getSettingsClient(activity)
        val task = client.checkLocationSettings(settingRequest)
        task.addOnSuccessListener {
            onEnable()
        }.addOnFailureListener {
            if (it is ResolvableApiException) {
                it.startResolutionForResult(activity, ENABLE_GPS_SETTING_CLIENT_REQ_CODE)
            } else {
                Log.d(TAG, "Unable to turn on GPS ex: ${it.localizedMessage}")
                listener.onLocationFetchFailed("Unable to turn on GPS")
            }

        }
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            ENABLE_GPS_SETTING_CLIENT_REQ_CODE -> {
                if (resultCode == RESULT_OK) {
                    getLocation()
                } else {
                    listener.onLocationFetchFailed("Please click ok to fetch current location")
                }
            }
        }
    }

    private fun Map<String, Boolean>.getOrFalse(permission: String): Boolean {
        return this[permission] == true
    }

    interface LocationListener {
        fun onLocationFetchSuccess(
            result: Location,
            isPreciseLocation: Boolean,
            api: String? = null
        )

        fun onLocationFetchFailed(error: String)
    }

    companion object {
        private const val TAG = "LocationUtils"
        private const val ENABLE_GPS_SETTING_CLIENT_REQ_CODE = 1060
    }
}