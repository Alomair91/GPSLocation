package com.omairtech.gpslocation.data

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.location.Location
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.omairtech.gpslocation.R
import com.omairtech.gpslocation.util.ACTION_STARTED_FROM_NOTIFICATION
import com.omairtech.gpslocation.util.CHANNEL_ID
import com.omairtech.gpslocation.util.NOTIFICATION_ID
import java.text.DateFormat
import java.util.*
import java.util.concurrent.TimeUnit


private const val TAG = "ForegroundService"

interface LocationListener {
    fun onLocation(location: Location)
    fun onRemoved(tag: String) {}
}

open class ForegroundService : Service() {

    // The Fused Location Provider provides access to location APIs.
    private var fusedLocationClient: FusedLocationProviderClient? = null

    // Stores parameters for requests to the FusedLocationProviderApi.
    private var locationRequest: LocationRequest = LocationRequest.create().apply {
        // Sets the desired interval for active location updates. This interval is inexact. You
        // may not receive updates at all if no location sources are available, or you may
        // receive them slower than requested. You may also receive updates faster than
        // requested if other applications are requesting location at a faster interval.
        //
        // IMPORTANT NOTE: Apps running on "O" devices (regardless of targetSdkVersion) may
        // receive updates less frequently than this interval when the app is no longer in the
        // foreground.
        interval = TimeUnit.SECONDS.toMillis(intervalInSecond)

        // Sets the fastest rate for active location updates. This interval is exact, and your
        // application will never receive updates faster than this value.
        fastestInterval = TimeUnit.SECONDS.toMillis(fastIntervalInSecond)

        // Sets the maximum time when batched location updates are delivered. Updates may be
        // delivered sooner than this interval.
        maxWaitTime = TimeUnit.MINUTES.toMillis(0)

        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }

    // Creates default LocationCallback for location changes.
    private var locationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            //The last location in the list is the newest
            locationResult.lastLocation.let {
                Log.d(TAG, "CurrentLocation:  ${it.latitude} - ${it.longitude}")
                locationListener?.onLocation(locationResult.lastLocation)
                updateNotification()
            }
        }
    }

    private var mServiceHandler: Handler? = null
    private var mChangingConfiguration = false

    private var locationListener: LocationListener? = null
    private var intervalInSecond: Long = 10
    private var fastIntervalInSecond: Long = intervalInSecond / 2
    private var removeAfterInSecond: Long = 60 * 60
    var notificationTitle: String? = locationTitle
    var notificationMessage: String? = null
    var mBinder: IBinder? = null
    private var appService: Class<*>? = null

    fun init(
        locationListener: LocationListener?,
        intervalInSecond: Long = 10,
        fastIntervalInSecond: Long = intervalInSecond / 2,
        removeAfterInSecond: Long = 60 * 60,
        notificationTitle: String? = "",
        notificationMessage: String? = "",
        mBinder: IBinder? = LocalBinder(),
        appService: Class<*>? = ForegroundService::class.java,
    ) {
        this.locationListener = locationListener
        this.intervalInSecond = intervalInSecond
        this.fastIntervalInSecond = fastIntervalInSecond
        this.removeAfterInSecond = removeAfterInSecond
        this.notificationTitle = notificationTitle
        this.notificationMessage = notificationMessage
        this.mBinder = mBinder
        this.appService = appService

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }
    private fun createHandler() {
        val handlerThread = HandlerThread("SPHandler")
        handlerThread.start()
        val looper = handlerThread.looper
        mServiceHandler = Handler(looper)
    }


    inner class LocalBinder : Binder() {
        fun getService() = this@ForegroundService
    }

    override fun onCreate() {
        createHandler()


        // Remove location update after a period of time
        Handler(Looper.getMainLooper()).postDelayed({
            Log.e(TAG, "removeLocationUpdate: 4")
            removeLocationUpdate("remove after " + removeAfterInSecond / 60.0 + " minutes")
        }, TimeUnit.SECONDS.toMillis(removeAfterInSecond))
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        mChangingConfiguration = true
    }

    override fun onBind(intent: Intent): IBinder? {
        stopForeground(true)
        Log.e(TAG, "onBind()")
        mChangingConfiguration = false
        return mBinder
    }

    override fun onRebind(intent: Intent) {
        //  stopForeground(true);
        Log.e(TAG, "onRebind()")
        mChangingConfiguration = false
        super.onRebind(intent)
    }

    override fun onUnbind(intent: Intent): Boolean {
        if (!mChangingConfiguration && SessionPreference.getInstance(this).requestingLocationUpdates) {
            Log.e(TAG, "onUnbind(): Start Foreground Service")
            startForeground(NOTIFICATION_ID, getNotification(this))
        }
        return true
    }

    override fun onDestroy() {
        mServiceHandler?.removeCallbacks(Runnable { })
        super.onDestroy()
    }

    fun requestLocationUpdate() {
        try {
            Log.e(TAG, "requestLocationUpdate()")
            SessionPreference.getInstance(this).setRequestingLocationUpdate(true)
            startService(Intent(applicationContext, appService))
            fusedLocationClient?.requestLocationUpdates(locationRequest,
                locationCallback, Looper.myLooper()!!)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    fun removeLocationUpdate(tag: String) {
        try {
            Log.e(TAG, "removeLocationUpdate(): $tag")
            this.fusedLocationClient?.removeLocationUpdates(locationCallback)
            SessionPreference.getInstance(this).setRequestingLocationUpdate(false)
            locationListener?.onRemoved(tag)
            stopSelf()
        } catch (e: SecurityException) {
            e.printStackTrace()
            SessionPreference.getInstance(this).setRequestingLocationUpdate(true)
        }
    }

    fun checkIfServiceRunningForeGround(context: Context): Boolean {
        val manager = context.getSystemService(ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (javaClass.name == service.service.className) {
                if (service.foreground) return true
            } else if (appService?.name == service.service.className) {
                if (service.foreground) return true
            }
        }
        return false
    }

    private var mNotificationManager: NotificationManager? = null
    private fun updateNotification() {
        if (mNotificationManager == null) {
            mNotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val mChannel = NotificationChannel(CHANNEL_ID,
                    notificationTitle,
                    NotificationManager.IMPORTANCE_DEFAULT)
                mNotificationManager!!.createNotificationChannel(mChannel)
            }
        }
        if (checkIfServiceRunningForeGround(this)) {
            mNotificationManager?.notify(NOTIFICATION_ID, getNotification(this))
        }
    }

    private fun getNotification(context: Context): Notification {
        val intent = Intent(context, appService)
        intent.putExtra(ACTION_STARTED_FROM_NOTIFICATION, true)

        val resultPendingIntent = PendingIntent.getActivities(
            context, 0, arrayOf(intent, intent), PendingIntent.FLAG_ONE_SHOT)

        val builder: NotificationCompat.Builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentText(notificationMessage)
            .setContentTitle(locationTitle)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(resultPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setSmallIcon(R.drawable.ic_my_location_24px)
            .setTicker(notificationMessage)
            .setDefaults(Notification.DEFAULT_LIGHTS)
            .setWhen(System.currentTimeMillis())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(CHANNEL_ID)
        }
        return builder.build()
    }

    companion object {
        private val locationTitle = String.format(Locale.ENGLISH, "Location Update: %s",
            DateFormat.getDateInstance().format(Date()))
    }
}