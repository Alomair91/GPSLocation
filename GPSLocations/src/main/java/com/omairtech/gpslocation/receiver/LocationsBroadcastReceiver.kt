package com.omairtech.gpslocation.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationResult
import com.omairtech.gpslocation.R
import com.omairtech.gpslocation.model.AddressData
import com.omairtech.gpslocation.model.toLocation
import com.omairtech.gpslocation.util.*


private const val TAG = "LUBroadcastReceiver"

/**
 * Receiver for handling location updates.
 *
 * For apps targeting API level O and above
 * {@link android.app.PendingIntent#getBroadcast(Context, int, Intent, int)} should be used when
 * requesting location updates in the background. Due to limits on background services,
 * {@link android.app.PendingIntent#getService(Context, int, Intent, int)} should NOT be used.
 *
 *  Note: Apps running on "O" devices (regardless of targetSdkVersion) may receive updates
 *  less frequently than the interval specified in the
 *  {@link com.google.android.gms.location.LocationRequest} when the app is no longer in the
 *  foreground.
 */
internal open class LocationsBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive() context:$context, intent:$intent")

        if (intent.action == ACTION_PROCESS_UPDATES) {

            // Checks for location availability changes.
            LocationAvailability.extractLocationAvailability(intent).let { locationAvailability ->
                @Suppress("SENSELESS_COMPARISON")
                if (null != locationAvailability &&  !locationAvailability.isLocationAvailable) {
                    Log.d(TAG, "Location services in App are no longer available!")
                }
            }

            LocationResult.extractResult(intent).let { locationResult ->
                @Suppress("SENSELESS_COMPARISON")
                if (null != locationResult && locationResult.locations.isNotEmpty()) {
                    var addressData:AddressData = toLocation(locationResult.locations.last(), isAppInForeground(context))
                    Log.d(TAG, "Background Location in App ${addressData.latitude} - ${addressData.longitude}")

                    // Retrieve address data from [Geocoder]
                    retrieveAddressDataFromGeocoder(context, addressData.latitude, addressData.longitude) {
                        addressData = it
                        Log.d(TAG, "Background Location in App ${it.address}")

                        // Make notification to inform user when you retrieve his location in the background
                        makeStatusNotification(context.getString(R.string.background_location_starting),
                            it.address,
                            context)

                        // Send address data [addressData] to the server

                    }
                    // Send location data [addressData] to the server

                }
            }
        }
    }
}
