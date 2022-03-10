package com.omairtech.simple

import android.Manifest
import android.app.Activity
import android.content.*
import android.location.Location
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.omairtech.gpslocation.data.LocationRepository
import com.omairtech.gpslocation.model.AddressData
import com.omairtech.gpslocation.model.PermissionUIData
import com.omairtech.gpslocation.model.toLocation
import com.omairtech.gpslocation.ui.PermissionFragment
import com.omairtech.gpslocation.util.LocationType
import com.omairtech.gpslocation.util.hasPermission
import com.omairtech.gpslocation.viewmodels.GPSLocationViewModel
import com.omairtech.gpslocation.viewmodels.GPSLocationViewModelFactory

private const val TAG = "ForegroundActivity"

class ForegroundActivity : AppCompatActivity() {
    private var selectedAddress: AddressData? = null
    private var mService: AppForegroundService? = null
    private var mBound = false

    private val mMessageReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // Get extra data included in the Intent
            val location: Location? = intent.getParcelableExtra("location")
            location?.let {
                setToText(toLocation(it, false),
                    "Current Location:  ${it.latitude} - ${it.longitude}")
            }
        }
    }

    private val mServiceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, iBinder: IBinder) {
            val binder: AppForegroundService.LocalBinder =
                iBinder as AppForegroundService.LocalBinder
            mService = binder.getService()
            mBound = true
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            mService = null
            mBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_foreground)

        LocalBroadcastManager.getInstance(this)
            .registerReceiver(mMessageReceiver, IntentFilter(UPDATE_UI))

        bindService(Intent(this, AppForegroundService::class.java),
            mServiceConnection,
            BIND_AUTO_CREATE)

        updateStartOrStopButtonState(false)
    }

    override fun onPause() {
        super.onPause()
        if (mBound) {
            unbindService(mServiceConnection)
            mBound = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Stops location updates from Broadcast.
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
    }


    private fun updateStartOrStopButtonState(receivingLocation: Boolean) {
        if (receivingLocation) {
            findViewById<Button>(R.id.btn_refresh).apply {
                text = getString(R.string.stop_receiving_location)
                setOnClickListener {
                    mService?.removeLocationUpdate("stopTrackingService")
                    updateStartOrStopButtonState(false)
                }
            }
        } else {
            findViewById<Button>(R.id.btn_refresh).apply {
                text = getString(R.string.start_receiving_location)
                setOnClickListener {
                    if(!hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
                        checkPermission()
                    }else {
                        mService?.requestLocationUpdate()
                        updateStartOrStopButtonState(true)
                    }
                }
            }
        }
    }

    // Ask user to get the required permissions
    private fun checkPermission() {
        PermissionFragment.newInstance(this, LocationType.FINE_LOCATION, PermissionUIData()) { isGranted ->
            if (isGranted) {
                updateStartOrStopButtonState(false)
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