package com.omairtech.gpslocation

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.IntentSender.SendIntentException
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.omairtech.gpslocation.util.LocationUtils.getAddressDetails
import com.omairtech.gpslocation.util.LocationListener

/**
 * Created by OmairTech on 01/04/2021.
 */
@SuppressLint("Registered")
class GPSLocation(
    private val activity: Activity,
    private val interval: Int,
    private val fastInterval: Int,
    private val requestPermissionLauncher: ActivityResultLauncher<Array<String>>? = null,
    private val activityResultLauncher: ActivityResultLauncher<IntentSenderRequest>? = null,
    private val locationListener: LocationListener,
) : android.location.LocationListener {


    companion object {
        private const val TAG = "GPS"
    }

    init {
        currentLocation
    }

    /**
     * Helper method
     * check if google play services is support or not
     *
     * @return boolean
     */
    private val isGooglePlayServicesAvailable: Boolean
        get() {
            val PLAY_SERVICES_RESOLUTION_REQUEST = 9000
            val apiAvailability = GoogleApiAvailability.getInstance()
            val resultCode = apiAvailability.isGooglePlayServicesAvailable(activity)
            if (resultCode != ConnectionResult.SUCCESS) {
                if (apiAvailability.isUserResolvableError(resultCode)) {
                    apiAvailability.getErrorDialog(
                        activity, resultCode,
                        PLAY_SERVICES_RESOLUTION_REQUEST
                    )?.show()
                } else {
                    Log.d(TAG, "This device is not supported.")
                }
                return false
            }
            Log.d(TAG, "This device is supported.")
            return true
        }

    /**
     * Helper method
     * Set updated location to the interface and get address from location
     *
     * @param location the updated location
     */
    private fun setResult(location: Location?) {
        if (location != null) {
            locationListener.onFindCurrentLocation(location.latitude, location.longitude)
            getAddressDetails(activity, locationListener, location.latitude, location.longitude)
        }
    }

    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private lateinit var mLocationRequest: LocationRequest

    // Got last known location. In some rare situations this can be null.
    private val currentLocation: Unit
        get() {
            if (isGooglePlayServicesAvailable) {

                mFusedLocationClient = LocationServices.getFusedLocationProviderClient(activity)
                // Got last known location. In some rare situations this can be null.
                mFusedLocationClient.lastLocation.addOnSuccessListener(activity, fun(it: Location) {
                    setResult(it)
                })

                mLocationRequest = LocationRequest.create()
                mLocationRequest.interval = (interval * 1000 /* UPDATE INTERVAL */).toLong()
                mLocationRequest.fastestInterval =
                    (fastInterval * 1000 /* FASTEST UPDATE INTERVAL*/).toLong()
                mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

                checkLocationPermission(true)
            }
        }

    fun onStart() {
        checkLocationPermission(true)
    }

    fun onStop() {
        // stop location updates when Activity is no longer active
        mFusedLocationClient.removeLocationUpdates(mLocationCallback)
    }

    fun refreshLocation() {
        onStop()
        currentLocation
    }


    fun checkLocationPermission(createLocationRequest: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat
                    .checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat
                    .checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher?.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
                return
            }
        }
        if (createLocationRequest) createLocationRequest()
    }

    // put blow method on your activity or fragment
//    private val requestPermissionLauncher =
//        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions  ->
//            // If request is cancelled, the result arrays are empty.
//            permissions.entries.forEach {
//                if(it.value){
//                    gpsLocation?.checkLocationPermission(true)
//                    return@forEach
//                }
//            }
//        }

    private var isGPSRequested: Boolean = false
    fun setIsGPSRequested(isGPSRequested: Boolean) {
        this.isGPSRequested = isGPSRequested
    }

     fun createLocationRequest() {
        val builder = LocationSettingsRequest.Builder().addLocationRequest(mLocationRequest)
        val client = LocationServices.getSettingsClient(activity)
        val task = client.checkLocationSettings(builder.build())
        task.addOnSuccessListener(activity, fun(_: LocationSettingsResponse) {
            // All location sp are satisfied. The client can initialize  location requests here.
            requestLocationUpdates()
        })
        task.addOnFailureListener(activity, fun(e: Exception?) {
            if (e is ResolvableApiException) {
                // Location sp are not satisfied, but this can be fixed by showing the user a dialog.
                try {
                    if (!isGPSRequested) {
                        setIsGPSRequested(true)
                        if (activityResultLauncher != null) {
                            val intentSenderRequest =
                                IntentSenderRequest.Builder(e.resolution).build()
                            activityResultLauncher.launch(intentSenderRequest)
                        }
                    }
                } catch (ex: SendIntentException) {
                    ex.printStackTrace()
                }
            }
        })
    }


    // put blow method on your activity or fragment
//    private val resolutionForResult = registerForActivityResult(
//        ActivityResultContracts.StartIntentSenderForResult()
//    ) { result ->
//        gpsLocation?.setIsGPSRequested(false)
//        when (result.resultCode) {
//            Activity.RESULT_OK ->
//                gpsLocation?.refreshLocation()
//            else ->
//                gpsLocation?.createLocationRequest();//keep asking if imp or do whatever
//        }
//    }


    private var mLocationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val locationList = locationResult.locations
            if (locationList.size > 0) {
                //The last location in the list is the newest
                val location = locationList[locationList.size - 1]
                setResult(location)
            }
        }
    }

    fun requestLocationUpdates() {
        checkLocationPermission(false)
        mFusedLocationClient.requestLocationUpdates(
            mLocationRequest,
            mLocationCallback,
            Looper.myLooper()!!
        )
    }

    override fun onLocationChanged(location: Location) {
        setResult(location)
    }
}