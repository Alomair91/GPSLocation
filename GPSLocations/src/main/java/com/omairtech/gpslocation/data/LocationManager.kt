package com.omairtech.gpslocation.data

import android.Manifest
import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.location.Geocoder
import android.os.Looper
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.annotation.MainThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.omairtech.gpslocation.model.AddressData
import com.omairtech.gpslocation.model.PermissionUIData
import com.omairtech.gpslocation.model.toAddress
import com.omairtech.gpslocation.model.toLocation
import com.omairtech.gpslocation.receiver.LocationsBroadcastReceiver
import com.omairtech.gpslocation.ui.PermissionFragment
import com.omairtech.gpslocation.util.*
import java.util.concurrent.TimeUnit

private const val TAG = "LocationManager"

/**
 * Manages all location related tasks for the app.
 */
internal class LocationManager(
    private val context: Context,
    private var locationType: LocationType = LocationType.FINE_LOCATION,
    private val intervalInSecond: Long = 60,
    private val fastIntervalInSecond: Long = 30,
    private val activityResultLauncher: ActivityResultLauncher<IntentSenderRequest>? = null,
    private val permissionUIData: PermissionUIData? = PermissionUIData(),
    private val broadcastReceiver: Class<BroadcastReceiver>? = null
) {

    /**
     * Status of location updates, i.e., whether the app is actively subscribed to location changes.
     */
    private val _receivingLocationUpdates: MutableLiveData<Boolean> =
        MutableLiveData<Boolean>(false)
    val receivingLocationUpdates: LiveData<Boolean> get() = _receivingLocationUpdates

    /**
     * Location updates, i.e., whether the app is actively subscribed to location changes.
     */
    private val _receivingLocation: MutableLiveData<AddressData> = MutableLiveData<AddressData>()
    val receivingLocation: LiveData<AddressData> get() = _receivingLocation

    // The Fused Location Provider provides access to location APIs.
    private var fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    var isForegroundOn:Boolean = SessionPreference.getInstance(context).isForegroundOn
    var isBackgroundOn:Boolean = SessionPreference.getInstance(context).isBackgroundOn

    var hasForegroundPermissions:Boolean = checkHasPermission(LocationType.FINE_LOCATION)
    var hasBackgroundPermissions:Boolean = checkHasPermission(LocationType.BACKGROUND_LOCATION)


    // Stores parameters for requests to the FusedLocationProviderApi.
    private var locationRequest: LocationRequest = LocationRequest.create().apply {
        // Sets the desired interval for active location updates. This interval is inexact. You
        // may not receive updates at all if no location sources are available, or you may
        // receive them slower than requested. You may also receive updates faster than
        // requested if other applications are requesting location at a faster interval.
        //
        // IMPORTANT NOTE: Apps running on "O" devices (regardless of targetSdkVersion) may
        // receive updates less frequently than this interval when the app is no longer in the
        // foreground.
        interval = TimeUnit.SECONDS.toMillis(intervalInSecond)

        // Sets the fastest rate for active location updates. This interval is exact, and your
        // application will never receive updates faster than this value.
        fastestInterval = TimeUnit.SECONDS.toMillis(fastIntervalInSecond)

        // Sets the maximum time when batched location updates are delivered. Updates may be
        // delivered sooner than this interval.
        maxWaitTime = TimeUnit.MINUTES.toMillis(2)

        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }

    // Creates default LocationCallback for location changes.
    private var locationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            //The last location in the list is the newest
            if (locationResult.locations.size > 0) locationResult.locations.last().let {
                Log.d(TAG, "CurrentLocation:  ${it.latitude} - ${it.longitude}")
                _receivingLocation.value = toLocation(it, true)
            }
        }
    }

    /**
     * Creates default PendingIntent for location changes.
     *
     * Note: We use a BroadcastReceiver because on API level 26 and above (Oreo+), Android places
     * limits on Services.
     */
    private val locationUpdatePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, broadcastReceiver ?: LocationsBroadcastReceiver::class.java)
        intent.action = ACTION_PROCESS_UPDATES
        PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }


    init {
        Thread.sleep(500)
        _receivingLocationUpdates.value = isForegroundOn || isBackgroundOn
        context.isGooglePlayServicesAvailable
    }

    fun setLocationType(locationType: LocationType) {
        if(locationType == LocationType.FINE_LOCATION)
            SessionPreference.getInstance(context).saveForeground(true)
        else SessionPreference.getInstance(context).saveBackground(true)

        if(this.locationType != locationType) stopLocationUpdates()
        this.locationType = locationType
    }


    private fun checkHasPermission(locationType:LocationType): Boolean {
        return if (locationType == LocationType.BACKGROUND_LOCATION)
            context.hasPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        else
            context.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    // Ask user to get the required permissions
    private fun checkPermission() {
        PermissionFragment.newInstance(context, locationType, permissionUIData) { isGranted ->
            if(isGranted) startLocationUpdates()
        }
    }

    // To prevent more than one GPS start request
    private var isRequestGPSDialogOn: Boolean = false
    fun isRequestGPSDialogOn(isGPSRequested: Boolean) {
        this.isRequestGPSDialogOn = isGPSRequested
    }

    /**
     * startLocationUpdates function will do:
     * 1. Check if the user has the permissions or ask for the required permissions
     * 2. Stop receiving the update if you started before
     * 3. If the phone's GPS is off, ask to start it.
     * 4. Start receiving location updates.
     */
    fun startLocationUpdates() {
        Log.d(TAG, "startLocationUpdates()")

        if (!checkHasPermission(locationType)) {
            checkPermission()
            return
        }

        if(isForegroundOn || isBackgroundOn)
            stopLocationUpdates()

        if (activityResultLauncher != null) {
            val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
            val client = LocationServices.getSettingsClient(context)
            val task = client.checkLocationSettings(builder.build())
            task.addOnSuccessListener(fun(_: LocationSettingsResponse) {
                // All location sp are satisfied. The client can initialize  location requests here.
                createLocationRequest()
                isRequestGPSDialogOn(false)
            })
            task.addOnFailureListener(fun(e: Exception?) {
                if (e is ResolvableApiException) {
                    // Location sp are not satisfied, but this can be fixed by showing the user a dialog.
                    try {
                        if (!isRequestGPSDialogOn) {
                            isRequestGPSDialogOn(true)
                            val intentSenderRequest =
                                IntentSenderRequest.Builder(e.resolution).build()
                            activityResultLauncher.launch(intentSenderRequest)
                        }
                    } catch (ex: IntentSender.SendIntentException) {
                        ex.printStackTrace()
                    }
                }
            })
        } else createLocationRequest()
    }


    /**
     * Uses the FusedLocationProvider to start location updates if the correct fine locations are
     * approved.
     *
     * @throws SecurityException if ACCESS_FINE_LOCATION permission is removed before the
     * FusedLocationClient's requestLocationUpdates() has been completed.
     */
    @Throws(SecurityException::class)
    @MainThread
    private fun createLocationRequest() {
        try {
            _receivingLocationUpdates.value = true

            if (locationType == LocationType.FINE_LOCATION) {
                fusedLocationClient.requestLocationUpdates(locationRequest,
                    locationCallback, Looper.myLooper()!!)

            } else {
                // If the PendingIntent is the same as the last request (which it always is), this
                // request will replace any requestLocationUpdates() called before.
                fusedLocationClient.requestLocationUpdates(locationRequest,
                    locationUpdatePendingIntent)
            }
        } catch (permissionRevoked: SecurityException) {
            _receivingLocationUpdates.value = false

            // Exception only occurs if the user revokes the FINE location permission before
            // requestLocationUpdates() is finished executing (very rare).
            Log.d(TAG, "Location permission revoked; details: $permissionRevoked")
            throw permissionRevoked
        }
    }

    @MainThread
    fun stopLocationUpdates() {
        Log.d(TAG, "stopLocationUpdates() $locationType")
        _receivingLocationUpdates.value = false
        cancelStatusNotification(context)

        if (locationType == LocationType.FINE_LOCATION) {
            // stop location updates when Activity is no longer active
            fusedLocationClient.removeLocationUpdates(locationCallback)
            SessionPreference.getInstance(context).saveForeground(false)
        } else {
            fusedLocationClient.removeLocationUpdates(locationUpdatePendingIntent)
            SessionPreference.getInstance(context).saveBackground(false)
        }
    }

    /**
     * Get address data from [Geocoder]
     */
    fun retrieveAddressDataFromGeocoder(latitude: Double? = null, longitude: Double? = null, callback: (AddressData) -> Unit) {
         if(latitude == null || longitude == null){
            receivingLocation.value?.let {
               retrieveAddressDataFromGeocoder(context,it.latitude, it.longitude,callback)
            }
        } else retrieveAddressDataFromGeocoder(context,latitude, longitude,callback)
    }

    companion object {
        @Volatile // to avoid potential bugs. (meaning that writes to this field are immediately made visible to other threads.)
        private var INSTANCE: LocationManager? = null
        fun getInstance(
            context: Context,
            locationType: LocationType = LocationType.FINE_LOCATION,
            intervalInSecond: Long = 60,
            fastIntervalInSecond: Long = 30,
            activityResultLauncher: ActivityResultLauncher<IntentSenderRequest>? = null,
            permissionUIData: PermissionUIData? = PermissionUIData(),
            broadcastReceiver: Class<BroadcastReceiver>? = null
        ): LocationManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LocationManager(context,
                    locationType,
                    intervalInSecond,
                    fastIntervalInSecond,
                    activityResultLauncher, permissionUIData,broadcastReceiver).also { INSTANCE = it }
            }
        }
    }
}