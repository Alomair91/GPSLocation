package com.omairtech.gpslocation.data

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentSender
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
import com.omairtech.gpslocation.model.toLocation
import com.omairtech.gpslocation.receiver.LocationsBroadcastReceiver
import com.omairtech.gpslocation.ui.PermissionFragment
import com.omairtech.gpslocation.util.ACTION_PROCESS_UPDATES
import com.omairtech.gpslocation.util.LocationType
import com.omairtech.gpslocation.util.cancelStatusNotification
import com.omairtech.gpslocation.util.hasPermission
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

    private var isPermissionGranted: Boolean = false

    init {
        Thread.sleep(500)
        if (!checkHasPermission()) checkPermission()
        _receivingLocationUpdates.value = SessionPreference.getInstance(context).isForegroundOn ||
                SessionPreference.getInstance(context).isBackgroundOn
    }

    fun setLocationType(locationType: LocationType) {
        SessionPreference.getInstance(context).saveForeground(locationType == LocationType.FINE_LOCATION)
        SessionPreference.getInstance(context).saveBackground(locationType == LocationType.BACKGROUND_LOCATION)

        if(this.locationType != locationType) stopLocationUpdates()
        this.locationType = locationType
        if (!checkHasPermission()) checkPermission()
    }

    var isBackgroundOn:Boolean = SessionPreference.getInstance(context).isBackgroundOn
    var isForegroundOn:Boolean = SessionPreference.getInstance(context).isForegroundOn

    private fun checkPermission() {
        PermissionFragment.newInstance(context, locationType, permissionUIData) { isGranted ->
            isPermissionGranted = isGranted
        }
    }

    private fun checkHasPermission(): Boolean {
        return if (locationType == LocationType.BACKGROUND_LOCATION)
            context.hasPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        else
            context.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    }

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

    private var isGPSRequested: Boolean = false
    fun setIsGPSRequested(isGPSRequested: Boolean) {
        this.isGPSRequested = isGPSRequested
    }

    fun createLocationRequest() {
        if (!checkHasPermission()) {
            checkPermission()
            return
        }

        if (activityResultLauncher != null) {
            val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
            val client = LocationServices.getSettingsClient(context)
            val task = client.checkLocationSettings(builder.build())
            task.addOnSuccessListener(fun(_: LocationSettingsResponse) {
                // All location sp are satisfied. The client can initialize  location requests here.
                startLocationUpdates()
                setIsGPSRequested(false)
            })
            task.addOnFailureListener(fun(e: Exception?) {
                if (e is ResolvableApiException) {
                    // Location sp are not satisfied, but this can be fixed by showing the user a dialog.
                    try {
                        if (!isGPSRequested) {
                            setIsGPSRequested(true)
                            val intentSenderRequest =
                                IntentSenderRequest.Builder(e.resolution).build()
                            activityResultLauncher.launch(intentSenderRequest)
                        }
                    } catch (ex: IntentSender.SendIntentException) {
                        ex.printStackTrace()
                    }
                }
            })
        } else startLocationUpdates()
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
    fun startLocationUpdates() {
        Log.d(TAG, "startLocationUpdates()")

        if (!checkHasPermission()) {
            checkPermission()
            return
        }

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

        if (locationType == LocationType.FINE_LOCATION) {
            // stop location updates when Activity is no longer active
            fusedLocationClient.removeLocationUpdates(locationCallback)
            SessionPreference.getInstance(context).saveForeground(false)
        } else {
            fusedLocationClient.removeLocationUpdates(locationUpdatePendingIntent)
            SessionPreference.getInstance(context).saveBackground(false)
        }
        cancelStatusNotification(context)
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