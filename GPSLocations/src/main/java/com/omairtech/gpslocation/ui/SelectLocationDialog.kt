package com.omairtech.gpslocation.ui

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.omairtech.gpslocation.R
import com.omairtech.gpslocation.model.AddressData
import com.omairtech.gpslocation.util.fullScreen
import com.omairtech.gpslocation.viewmodels.GPSLocationViewModel


/**
 * Allows users to select location from map.
 */
class SelectLocationDialog : BottomSheetDialogFragment(), OnMapReadyCallback {
    private var viewModel: GPSLocationViewModel? = null
    private var callback: ((AddressData) -> Unit?)? = null

    private lateinit var root: View

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, s: Bundle?): View {
        root = inflater.inflate(R.layout.dialog_select_location, container, false)
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        try {
            val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
            mapFragment.getMapAsync(this)

            // This callback will only be called when Back button pressed.
            dialog?.setOnKeyListener { _, keyCode, _ ->
                if (keyCode == KeyEvent.KEYCODE_BACK) dismiss()
                true
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }

        root.apply {
            findViewById<ImageView>(R.id.btn_close).setOnClickListener {
                dismiss()
            }
            findViewById<ImageView>(R.id.btnGetCurrentLocation).setOnClickListener {
                // remove old location
                selectedAddress = null
                // get current location
                createLocationRequest()
            }
            findViewById<ImageView>(R.id.btnSave).setOnClickListener {
                if (selectedAddress == null) {
                    Toast.makeText(requireContext(),
                        R.string.select_your_location,
                        Toast.LENGTH_SHORT).show()
                    createLocationRequest()
                    return@setOnClickListener
                }
                callback?.let { it(selectedAddress!!) }
                dismiss()
            }
        }
        handleEvent()
        createLocationRequest()
    }

    override fun onStart() {
        super.onStart()
        fullScreen()
    }

    override fun onPause() {
        super.onPause()
        // To stop location updates
        viewModel?.stopLocationUpdates()
    }

    // To start retrieve location updates
    private fun createLocationRequest() {
        viewModel?.stopLocationUpdates()
        viewModel?.startLocationUpdates()
        if (zoom != 16f) zoom = 16f
    }

    private fun handleEvent() {
//        // Receiving location updates if it's starting or not
//        viewModel.receivingLocationUpdates.observe(viewLifecycleOwner) { isRunning ->
//            // Update UI...
//        }

        // Receiving location data from [FusedLocationProviderClient]
        viewModel?.receivingLocation?.observe(viewLifecycleOwner) {
            if (!isDetached) {
                Log.v("CurrentLocation", "Lat: ${it.latitude} - Long: ${it.longitude}")

                // The marker will move the map and then it will call retrieveAddressData
                createMarker(LatLng(it.latitude, it.longitude), true)

                // set current location if selectedAddress is null
                if (selectedAddress == null) {
                    selectedAddress = it
                } else {
                    Thread.sleep(300)
                    createMarker(LatLng(selectedAddress!!.latitude, selectedAddress!!.longitude),
                        false)
                }

                retrieveAddressData(selectedAddress!!)
            }
        }
    }

    private fun retrieveAddressData(location: AddressData) {
        try {
            if (isDetached) return
            Log.v("CurrentLocation", "Lat: ${location.latitude} - Long: ${location.longitude}")
            selectedAddress = location

            // Retrieve address data from [Geocoder]
            viewModel?.retrieveAddressDataFromGeocoder(location.latitude, location.longitude) {
                Log.v("CurrentAddress", "Name: ${it.name} - Address: ${it.address}")
                if (isDetached) return@retrieveAddressDataFromGeocoder
                selectedAddress = it
                root.findViewById<TextView>(R.id.txt_address).text = selectedAddress?.address
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    private var selectedAddress: AddressData? = null
    private var mMap: GoogleMap? = null
    private var zoom = 16f
    private var isRunning = false
    override fun onMapReady(googleMap: GoogleMap) {
        try {
            mMap = googleMap
            mMap?.apply {
                mapType = GoogleMap.MAP_TYPE_NORMAL
                uiSettings.isMapToolbarEnabled = false
//                setMyLocationEnabled(true);
//                etBuildingsEnabled(true);
//                uiSettings.setZoomGesturesEnabled(true);

                setOnMapLongClickListener { latLng: LatLng ->
                    // The marker will move the map and then it will call retrieveAddressData
                    createMarker(latLng, false)
                }

                setOnCameraIdleListener {
                    if (isRunning) {
                        val center: LatLng = mMap!!.cameraPosition.target
                        zoom = mMap!!.cameraPosition.zoom
                        isRunning = false
                        // Retrieve address data from [Geocoder]
                        retrieveAddressData(AddressData(latitude = center.latitude,
                            longitude = center.longitude))
                    }
                }

                setOnCameraMoveStartedListener { isRunning = true }
                setOnCameraMoveCanceledListener { isRunning = false }

                selectedAddress?.let {
                    moveToCurrentLocation(LatLng(it.latitude, it.longitude))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun moveToCurrentLocation(currentLocation: LatLng) {
        if (mMap != null)
            mMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, zoom))
    }

    private fun createMarker(latLng: LatLng, isYou: Boolean) {
        try {
            mMap?.let {
                if (isYou) {
                    it.clear()
                    it.addMarker(MarkerOptions()
                        .position(latLng)
                        .title(getString(R.string.you))
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                    )?.showInfoWindow()
                }

                val cameraPosition: CameraPosition =
                    CameraPosition.builder().target(latLng).zoom(zoom).bearing(0f).build()
//            if (type.equals("GPS"))
//                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17));
//            else
                it.animateCamera(CameraUpdateFactory
                    .newCameraPosition(cameraPosition), 1000, null)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of this fragment using the provided parameters.
         *
         * @return A new instance of SelectLocationDialog.
         */
        @JvmStatic
        fun newInstance(
            context: Context,
            viewModel: GPSLocationViewModel,
            addressData: AddressData? = null,
            callback: (AddressData) -> Unit,
        ) = SelectLocationDialog().apply {

            if(context is Activity && context.isDestroyed) return@apply

            this.viewModel = viewModel
            this.selectedAddress = addressData
            this.callback = callback
            isCancelable = false
            show((context as FragmentActivity).supportFragmentManager, null)
        }
    }
}