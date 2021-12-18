package com.omairtech.simple

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import com.omairtech.gpslocation.GPSLocation
import com.omairtech.gpslocation.model.AddressData
import com.omairtech.gpslocation.listener.LocationListener

class MainActivity : AppCompatActivity() {
    private  val TAG = "MainActivity"

    private lateinit var gpsLocation: GPSLocation
    private var selectedAddress: AddressData? = null

    private lateinit var textView:TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        gpsLocation = GPSLocation(
            this,
            5 * 60 * 1000,
            10 * 1000,
            requestPermissionLauncher,
            activityResultLauncher,
            listener,
        )
        textView = findViewById(R.id.textView)

        findViewById<Button>(R.id.button).setOnClickListener { refreshLocation() }
    }

    override fun onStart() {
        super.onStart()
        gpsLocation.onStart()
    }

    override fun onStop() {
        super.onStop()
        gpsLocation.onStop()
    }

    private fun refreshLocation() {
        gpsLocation.refreshLocation()
    }


    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // If request is cancelled, the result arrays are empty.
        permissions.entries.forEach {
            if (it.value) {
                gpsLocation.checkLocationPermission(true)
                return@forEach
            }
        }
    }
    private val activityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        gpsLocation.setIsGPSRequested(false)
        when (result.resultCode) {
            Activity.RESULT_OK ->
                gpsLocation.refreshLocation()
            else ->
               gpsLocation.createLocationRequest() //keep asking if imp or do whatever
        }
    }
    private val listener : LocationListener = object : LocationListener {
        override fun onFindCurrentLocation(latitude: Double, longitude: Double) {
            super.onFindCurrentLocation(latitude, longitude)
            Log.d(TAG, "CurrentLocation:  $latitude - $longitude")
        }
        override fun onFindCurrentAddress(address: AddressData) {
            super.onFindCurrentAddress(address)

            Log.d(TAG,"Current Address: " + address.name + " - " + address.address)

            selectedAddress = address
            textView.text = selectedAddress?.address
        }
    }
}