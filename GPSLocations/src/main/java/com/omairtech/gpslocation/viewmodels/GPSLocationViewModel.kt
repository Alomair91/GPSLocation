package com.omairtech.gpslocation.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.work.*
import com.omairtech.gpslocation.data.LocationRepository
import com.omairtech.gpslocation.model.AddressData
import com.omairtech.gpslocation.util.*
import com.omairtech.gpslocation.workers.LocationWorker

/**
 * Allows [Activity, Fragment] to observer [AddressData], follow the state of location updates,
 * and start/stop receiving location updates.
 */
class GPSLocationViewModel(
    applications: Application,
    private val locationRepository: LocationRepository,
) : AndroidViewModel(applications) {
    private val workManager = WorkManager.getInstance(applications)

    // This transformation makes sure that whenever the current work Id changes the WorkInfo
    // the UI is listening to changes
    val receivingAddressFromGeocoder: LiveData<List<WorkInfo>> = workManager.getWorkInfosByTagLiveData(TAG_FULL_ADDRESS)


    /**
     * Status of whether the app is actively subscribed to location changes.
     */
    val receivingLocationUpdates: LiveData<Boolean> = locationRepository.receivingLocationUpdates

    /**
     *  Returns current location from LocationCallback.
     */
    val receivingLocation: LiveData<AddressData> = locationRepository.locationUpdates



    fun setLocationType(locationType: LocationType) {
        locationRepository.setLocationType(locationType)
    }
    var isBackgroundOn:Boolean = locationRepository.isBackgroundOn
    var isForegroundOn:Boolean = locationRepository.isForegroundOn

    fun setIsGPSRequested(isGPSRequested: Boolean) {
        locationRepository.setIsGPSRequested(isGPSRequested)
    }

    fun createLocationRequest() = locationRepository.createLocationRequest()

    fun startLocationUpdates() = locationRepository.startLocationUpdates()

    fun stopLocationUpdates() = locationRepository.stopLocationUpdates()

    /**
     * Create the WorkRequest to retrieve the address data from Geocoder
     */
    fun retrieveAddressDataFromGeocoder() {
        workManager.enqueueUniqueWork(
            LOCATION_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<LocationWorker>()
                .addTag(TAG_FULL_ADDRESS)
                .setInputData(createInputDataForUri())
                .build())
    }

    fun cancelRetrieveAddressDataFromGeocoder() {
        workManager.cancelUniqueWork(LOCATION_WORK_NAME)
    }

    /**
     * Creates the input data bundle which includes the Latitude and Longitude to operate on
     * @return Data which contains the Location's Latitude and Longitude as a Double
     */
    private fun createInputDataForUri(): Data {
        val builder = Data.Builder()
        receivingLocation.value?.let {
            builder.putDouble(KEY_LAT, it.latitude)
            builder.putDouble(KEY_LONG, it.longitude)
        }
        return builder.build()
    }
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