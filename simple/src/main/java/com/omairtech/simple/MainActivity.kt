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
import com.omairtech.gpslocation.model.AddressData
import com.omairtech.gpslocation.model.PermissionUIData
import com.omairtech.gpslocation.ui.SelectLocationDialog
import com.omairtech.gpslocation.util.LocationType
import com.omairtech.gpslocation.viewmodels.GPSLocationViewModel
import com.omairtech.gpslocation.viewmodels.GPSLocationViewModelFactory

private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {
    private var selectedAddress: AddressData? = null

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
    private val intervalInSecond: Long = 6
    private val fastIntervalInSecond: Long = 3

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
        findViewById<Button>(R.id.btn_select).setOnClickListener {
            SelectLocationDialog.newInstance(this,viewModel,selectedAddress){
                setToText(it, "Selected Address: ${it.name} - ${it.address}")
            }
        }

        // Receiving location updates if it's starting or not
        viewModel.receivingLocationUpdates.observe(this) {
            updateStartOrStopButtonState(it)
        }

        // Receiving status of whether the app permission is granted or not.
        viewModel.isPermissionGranted.observe(this) {
            // keep asking if imp or do whatever
            Log.e("Permission","isPermissionGranted: $it")
            // viewModel.startLocationUpdates() // keep asking
        }

        // Receiving location data from [FusedLocationProviderClient]
        viewModel.receivingLocation.observe(this) { location ->
            setToText(location,"Current Location:  ${location.latitude} - ${location.longitude}")

            // Retrieve address data from [Geocoder] with the retrieved location
            viewModel.retrieveAddressDataFromGeocoder {
                setToText(location, "Current Address: ${it.name} - ${it.address}")
            }

            // Retrieve address data from [Geocoder] using custom location
            viewModel.retrieveAddressDataFromGeocoder(location.latitude, location.longitude) {
                setToText(location, "Current Address: ${it.name} - ${it.address}")
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // Stops location updates if background permissions aren't approved.
        if (viewModel.receivingLocationUpdates.value == true && !viewModel.hasBackgroundPermissions) {
            Log.e("test","te")
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

    private var locationString: String = ""
    private fun setToText(location: AddressData, locationString: String) {
        this.selectedAddress = location
        this.locationString += "\n\n$locationString"

        Log.d(TAG, locationString)
        findViewById<TextView>(R.id.textView).text = this.locationString
    }
}