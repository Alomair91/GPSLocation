package com.omairtech.simple

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationResult
import com.omairtech.gpslocation.model.AddressData
import com.omairtech.gpslocation.model.toLocation
import com.omairtech.gpslocation.util.ACTION_PROCESS_UPDATES
import com.omairtech.gpslocation.util.getAddressData
import com.omairtech.gpslocation.util.isAppInForeground
import com.omairtech.gpslocation.util.makeStatusNotification

private const val TAG = "LocationReceiver"

class LocationBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
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
                    getAddressData(context,addressData.latitude,addressData.longitude){
                        addressData = it
                        Log.d(TAG, "Background Location in App ${it.address}")

                        // Make notification to inform user when you retrieve his location in the background
                        makeStatusNotification(context.getString(R.string.background_location_starting),it.address,context)

                        // Send address data [addressData] to the server

                    }

                    // Send location data [addressData] to the server
                }
            }
        }
    }
}




