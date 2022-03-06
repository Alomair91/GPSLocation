/*
 * Copyright (C) 2020 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.omairtech.gpslocation.data

import android.content.BroadcastReceiver
import android.content.Context
import android.location.Geocoder
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.annotation.MainThread
import androidx.lifecycle.LiveData
import com.omairtech.gpslocation.model.AddressData
import com.omairtech.gpslocation.model.PermissionUIData
import com.omairtech.gpslocation.util.LocationType

private const val TAG = "LocationRepository"

/**
 * Access point for location APIs (start/stop location updates and checking location update status).
 */
class LocationRepository private constructor(private val locationManager: LocationManager) {

    // Location related fields/methods:
    /**
     * Status of whether the app is actively subscribed to location changes.
     */
    val receivingLocationUpdates: LiveData<Boolean> = locationManager.receivingLocationUpdates

    /**
     * Status of whether the app is actively subscribed to location changes.
     */
    val locationUpdates: LiveData<AddressData> = locationManager.receivingLocation


    var isBackgroundOn: Boolean = locationManager.isBackgroundOn
    var isForegroundOn: Boolean = locationManager.isForegroundOn

    var hasForegroundPermissions: Boolean = locationManager.hasForegroundPermissions
    var hasBackgroundPermissions: Boolean = locationManager.hasBackgroundPermissions

    fun setLocationType(locationType: LocationType) {
        locationManager.setLocationType(locationType)
    }
    fun isRequestGPSDialogOn(isGPSRequested: Boolean) {
        locationManager.isRequestGPSDialogOn(isGPSRequested)
    }


    /**
     * Subscribes to location updates.
     */
    @MainThread
    fun startLocationUpdates() = locationManager.startLocationUpdates()

    /**
     * Un-subscribes from location updates.
     */
    @MainThread
    fun stopLocationUpdates() = locationManager.stopLocationUpdates()

    /**
     * Get address data from [Geocoder]
     */
    @MainThread
    fun retrieveAddressDataFromGeocoder(
        latitude: Double? = null,
        longitude: Double? = null,
        callback: (AddressData) -> Unit,
    ) {
        locationManager.retrieveAddressDataFromGeocoder(latitude, longitude, callback)
    }

    companion object {
        @Volatile
        private var INSTANCE: LocationRepository? = null

        fun getInstance(
            context: Context,
            locationType: LocationType = LocationType.FINE_LOCATION,
            intervalInSecond: Long = 60,
            fastIntervalInSecond: Long = 30,
            activityResultLauncher: ActivityResultLauncher<IntentSenderRequest>? = null,
            permissionUIData: PermissionUIData? = PermissionUIData(),
            broadcastReceiver: Class<BroadcastReceiver>? = null,
        ): LocationRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LocationRepository(
                    LocationManager.getInstance(context,
                        locationType,
                        intervalInSecond,
                        fastIntervalInSecond,
                        activityResultLauncher, permissionUIData, broadcastReceiver))
                    .also { INSTANCE = it }
            }
        }
    }
}
