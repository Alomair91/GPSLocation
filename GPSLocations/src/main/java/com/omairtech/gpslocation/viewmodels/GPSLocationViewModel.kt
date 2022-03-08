package com.omairtech.gpslocation.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.omairtech.gpslocation.data.LocationRepository
import com.omairtech.gpslocation.model.AddressData
import com.omairtech.gpslocation.util.*

/**
 * Allows [Activity, Fragment] to observer [AddressData], follow the state of location updates,
 * and start/stop receiving location updates.
 */
class GPSLocationViewModel(
    applications: Application,
    private val locationRepository: LocationRepository,
) : AndroidViewModel(applications) {

    /**
     * Status of whether the app is actively subscribed to location changes.
     */
    val receivingLocationUpdates: LiveData<Boolean> = locationRepository.receivingLocationUpdates

    /**
     * Status of whether the app permission is granted or not.
     */
    val isPermissionGranted: LiveData<Boolean> = locationRepository.isPermissionGranted

    /**
     *  Returns current location from LocationCallback.
     */
    val receivingLocation: LiveData<AddressData> = locationRepository.locationUpdates

    var isForegroundOn: Boolean = locationRepository.isForegroundOn
    var isBackgroundOn: Boolean = locationRepository.isBackgroundOn

    var hasForegroundPermissions: Boolean = locationRepository.hasForegroundPermissions
    var hasBackgroundPermissions: Boolean = locationRepository.hasBackgroundPermissions

    fun setLocationType(locationType: LocationType) =
        locationRepository.setLocationType(locationType)

    fun isRequestGPSDialogOn(isGPSRequested: Boolean) =
        locationRepository.isRequestGPSDialogOn(isGPSRequested)

    fun startLocationUpdates() = locationRepository.startLocationUpdates()

    fun stopLocationUpdates() = locationRepository.stopLocationUpdates()

    fun retrieveAddressDataFromGeocoder(
        latitude: Double? = null,
        longitude: Double? = null,
        callback: (AddressData) -> Unit,
    ) = locationRepository.retrieveAddressDataFromGeocoder(latitude, longitude, callback)
}

class GPSLocationViewModelFactory(
    private val application: Application, private val locationRepository: LocationRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GPSLocationViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GPSLocationViewModel(application,
                locationRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}