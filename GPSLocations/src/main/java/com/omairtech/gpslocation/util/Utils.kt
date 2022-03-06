package com.omairtech.gpslocation.util

import android.Manifest
import android.app.Activity
import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Geocoder
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import com.omairtech.gpslocation.R
import com.omairtech.gpslocation.data.SessionPreference
import com.omairtech.gpslocation.model.AddressData
import com.omairtech.gpslocation.model.toAddress
import java.io.IOException
import java.util.*


enum class LocationType {
    FINE_LOCATION, BACKGROUND_LOCATION
}

/**
 * Helper functions to check if google play services is supported or not
 */
val Context.isGooglePlayServicesAvailable: Boolean
    get() {
        val apiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = apiAvailability.isGooglePlayServicesAvailable(this)
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode) && this is Activity) {
                apiAvailability.getErrorDialog(
                    this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST)?.show()
            } else {
                Log.d(TAG, "This device is not supported Google Play Services.")
            }
            return false
        }
        Log.d(TAG, "This device is supported Google Play Services.")
        return true
    }

/**
 * Helper functions to simplify permission checks/requests.
 */
fun Context.hasPermission(permission: String): Boolean {
    // Background permissions didn't exit prior to Q, so it's approved by default.
    if (permission == Manifest.permission.ACCESS_BACKGROUND_LOCATION &&
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        return true
    }

    return ActivityCompat.checkSelfPermission(this, permission) ==
            PackageManager.PERMISSION_GRANTED
}


/**
 * Requests permission and if the user denied a previous request, but didn't check
 * "Don't ask again", we provide additional rationale.
 *
 * Note: The Snackbar should have an action to request the permission.
 */
fun Fragment.requestPermissionWithRationale(
    permissions: Array<String>,
    requestPermissionLauncher: ActivityResultLauncher<Array<String>>,
    snackbar: Snackbar? = null
) {
    val provideRationale = shouldShowRequestPermissionRationale(permissions[0])
    if (provideRationale && snackbar != null) snackbar.show()
    else requestPermissionLauncher.launch(permissions)
}


fun BottomSheetDialogFragment.fullScreen() {
    try {
        val dialog = dialog
        if (dialog != null) {
            val bottomSheet = dialog.findViewById<View>(R.id.design_bottom_sheet)
            bottomSheet.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            (bottomSheet.parent as View).setBackgroundColor(Color.TRANSPARENT)
        }
        val view = view
        view?.post {
            val parent = view.parent as View
            val params = parent.layoutParams as CoordinatorLayout.LayoutParams
            val behavior = params.behavior
            val bottomSheetBehavior = behavior as BottomSheetBehavior<*>?
            bottomSheetBehavior!!.setPeekHeight(view.measuredHeight)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

/**
 * Create a Notification that is shown as a heads-up notification if possible.
 *
 * @param message Message shown on the notification
 * @param context Context needed to create Toast
 */
fun makeStatusNotification(title: String, message: String, context: Context) {

    // Make a channel if necessary
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        val name = VERBOSE_NOTIFICATION_CHANNEL_NAME
        val description = VERBOSE_NOTIFICATION_CHANNEL_DESCRIPTION
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(CHANNEL_ID, name, importance)
        channel.description = description

        // Add the channel
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?

        notificationManager?.createNotificationChannel(channel)
    }

    // Create the notification
    val builder = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_my_location_24px)
        .setContentTitle(title)
        .setContentText(message)
        .setSilent(true)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setOngoing(SessionPreference.getInstance(context).isBackgroundOn)
        .setVibrate(LongArray(0))

    // Show the notification
    NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
}

fun cancelStatusNotification(context: Context) {
    NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
}


// Note: This function's implementation is only for debugging purposes. If you are going to do
// this in a production app, you should instead track the state of all your activities in a
// process via android.app.Application.ActivityLifecycleCallbacks's
// unregisterActivityLifecycleCallbacks(). For more information, check out the link:
// https://developer.android.com/reference/android/app/Application.html#unregisterActivityLifecycleCallbacks(android.app.Application.ActivityLifecycleCallbacks
fun isAppInForeground(context: Context): Boolean {
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val appProcesses = activityManager.runningAppProcesses ?: return false

    appProcesses.forEach { appProcess ->
        if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
            && appProcess.processName == context.packageName) {
            return true
        }
    }
    return false
}

/**
 * Get address data from [Geocoder]
 */
fun retrieveAddressDataFromGeocoder(
    context: Context,
    latitude: Double,
    longitude: Double,
    callback: (AddressData) -> Unit,
) {
    val thread: Thread = object : Thread() {
        override fun run() {
            try {
                if (!Geocoder.isPresent()) {
                    Log.d("Geocoder", "No geocoder available")
                    return
                }
                val geocoder = Geocoder(context, Locale.getDefault())
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                if (addresses != null && addresses.size > 0) {
                    Log.d("Geocoder", addresses[0].toString())
                    val addressData = toAddress(addresses[0], true)
                    if(context is Activity)
                       context.runOnUiThread { callback(addressData) }
                    else callback(addressData)
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
fun showLocationOnGoogleMapsApplication(context: Activity, latitude: Double, longitude: Double) {
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