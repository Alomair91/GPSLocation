package com.omairtech.gpslocation.util

import android.app.Activity
import android.content.Intent
import android.location.Geocoder
import android.net.Uri
import android.util.Log
import com.omairtech.gpslocation.R
import com.omairtech.gpslocation.listener.LocationListener
import com.omairtech.gpslocation.model.AddressData
import java.io.IOException
import java.util.*

/**
 * Created by OmairTech on 01/04/2021.
 */
open class LocationUtils {
    val CURRENT_LOCATION = -1

    fun getAddressDetails(
        activity: Activity,
        locationListener: LocationListener,
        lat: Double,
        lng: Double
    ) {
        val thread: Thread = object : Thread() {
            override fun run() {
                try {
                    if (!Geocoder.isPresent()) {
                        Log.e("Geocoder", "No geocoder available")
                        return
                    }
                    val geocoder = Geocoder(activity, Locale.getDefault())
                    val addresses = geocoder.getFromLocation(lat, lng, 1)
                    if (addresses != null && addresses.size > 0) {
                        val addressData = AddressData(
                            CURRENT_LOCATION,
                            activity.getString(R.string.current_location),
                            addresses[0].getAddressLine(0),
                            addresses[0].latitude,
                            addresses[0].longitude,
                            addresses[0].countryName,
                            addresses[0].adminArea,
                            addresses[0].subAdminArea,
                            addresses[0].locality,
                            addresses[0].subLocality,
                            addresses[0].thoroughfare,
                            addresses[0].subThoroughfare,
                        )
                        Log.e("Geocoder", addressData.toString())
                        activity.runOnUiThread {
                            locationListener.onFindCurrentAddress(addressData)
                        }
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
        thread.start()
    }

    /**
     * View the best route from current point to the destination point (daddr)
     *
     * @param context   Activity
     * @param latitude  destination point
     * @param longitude destination point
     */
    fun showLocationOnMap(context: Activity, latitude: Double, longitude: Double) {
        val uri = "http://maps.google.com/maps?daddr=$latitude,$longitude"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
        intent.setPackage("com.google.android.apps.maps")
        context.startActivity(intent)
    }


    /**
     * // https://www.geodatasource.com/developers/java
     * System.out.println(distance(32.9697, -96.80322, 29.46786, -98.53506, "M") + " Miles\n");
     * System.out.println(distance(32.9697, -96.80322, 29.46786, -98.53506, "K") + " Kilometers\n");
     * System.out.println(distance(32.9697, -96.80322, 29.46786, -98.53506, "N") + " Nautical Miles\n");
     *
     * @param lat1 target point
     * @param lon1 target point
     * @param lat2 destination point
     * @param lon2 destination point
     * @param unit M for Miles, K for Kilometers, N for Nautical Miles
     * @return double
     */
    fun distance(lat1: Double, lon1: Double, lat2: Double, lon2: Double, unit: String): Double {
        return if (lat1 == lat2 && lon1 == lon2) {
            0.0
        } else {
            val theta = lon1 - lon2
            var dist = Math.sin(Math.toRadians(lat1)) * Math.sin(
                Math.toRadians(lat2)
            ) + Math.cos(Math.toRadians(lat1)) * Math.cos(
                Math.toRadians(
                    lat2
                )
            ) * Math.cos(Math.toRadians(theta))
            dist = Math.acos(dist)
            dist = Math.toDegrees(dist)
            dist *= 60 * 1.1515
            if (unit == "K") {
                dist *= 1.609344
            } else if (unit == "N") {
                dist *= 0.8684
            }
            dist
        }
    }
}