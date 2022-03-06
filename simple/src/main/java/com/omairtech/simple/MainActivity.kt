package com.omairtech.simple

import android.app.Activity
import android.content.BroadcastReceiver
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.omairtech.gpslocation.data.LocationRepository
import com.omairtech.gpslocation.model.PermissionUIData
import com.omairtech.gpslocation.util.LocationType
import com.omairtech.gpslocation.viewmodels.GPSLocationViewModel
import com.omairtech.gpslocation.viewmodels.GPSLocationViewModelFactory

private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {

    private val activityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        viewModel.isRequestGPSDialogOn(false)
        when (result.resultCode) {
            Activity.RESULT_OK ->
                viewModel.startLocationUpdates()
            else -> {
                //keep asking if imp or do whatever
                viewModel.startLocationUpdates()
            }
        }
    }

    private val locationType = LocationType.FINE_LOCATION
    private val intervalInSecond: Long = 60
    private val fastIntervalInSecond: Long = 30

    // You can customize the resources of [permission dialog] if you wish
    private val foregroundUiData = PermissionUIData.Foreground(
        hideUi = false,
        btn_approve = R.string.approve_location_access,
        btn_cancel = android.R.string.cancel,
    )

    // You can customize the resources of [permission dialog] if you wish
    private val backgroundUiData = PermissionUIData.Background(
        hideUi = false,
        btn_approve = R.string.approve_background_location_access,
        btn_cancel = android.R.string.cancel,
    )

    private val viewModel: GPSLocationViewModel by viewModels {
        @Suppress("UNCHECKED_CAST")
        GPSLocationViewModelFactory(application, LocationRepository.getInstance(
            this, locationType, intervalInSecond, fastIntervalInSecond, activityResultLauncher,
            PermissionUIData(foreground = foregroundUiData, background = backgroundUiData),
            // Don't forget to pass broadcast receiver class here
            LocationBroadcastReceiver::class.java as Class<BroadcastReceiver>
        ))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<SwitchCompat>(R.id.switch_background_location).isChecked = viewModel.isBackgroundOn
        findViewById<SwitchCompat>(R.id.switch_background_location).setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                viewModel.setLocationType(LocationType.BACKGROUND_LOCATION)
            } else {
                viewModel.setLocationType(LocationType.FINE_LOCATION)
            }
        }

        updateStartOrStopButtonState(viewModel.isForegroundOn || viewModel.isBackgroundOn)

        // Receiving location updates if it's starting or not
        viewModel.receivingLocationUpdates.observe(this) {
            updateStartOrStopButtonState(it)
        }

        // Receiving location data from [FusedLocationProviderClient]
        viewModel.receivingLocation.observe(this) { location ->
            setToText("CurrentLocation:  ${location.latitude} - ${location.longitude}")

            // Retrieve address data from [Geocoder] with the retrieved location
            viewModel.retrieveAddressDataFromGeocoder {
                setToText("Current Address: ${it.name} - ${it.address}")
            }

            // Retrieve address data from [Geocoder] using custom location
            viewModel.retrieveAddressDataFromGeocoder(location.latitude, location.longitude) {
                setToText("Current Address: ${it.name} - ${it.address}")
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // Stops location updates if background permissions aren't approved.
        if (viewModel.receivingLocationUpdates.value == true && !viewModel.hasBackgroundPermissions) {
            viewModel.stopLocationUpdates()
        }
    }

    private fun updateStartOrStopButtonState(receivingLocation: Boolean) {
        if (receivingLocation) {
            findViewById<Button>(R.id.btn_refresh).apply {
                text = getString(R.string.stop_receiving_location)
                setOnClickListener { viewModel.stopLocationUpdates() }
            }
        } else {
            findViewById<Button>(R.id.btn_refresh).apply {
                text = getString(R.string.start_receiving_location)
                setOnClickListener { viewModel.startLocationUpdates() }
            }
        }
    }

    private var location: String = ""
    private fun setToText(location: String) {
        Log.d(TAG, location)
        this.location += "\n\n$location"
        findViewById<TextView>(R.id.textView).text = this.location
    }
}