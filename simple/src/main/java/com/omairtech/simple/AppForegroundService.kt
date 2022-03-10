package com.omairtech.simple

import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.annotation.Nullable
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.omairtech.gpslocation.data.ForegroundService
import com.omairtech.gpslocation.data.LocationListener
import com.omairtech.gpslocation.util.ACTION_STARTED_FROM_NOTIFICATION


private const val TAG = "AppForegroundService"
const val UPDATE_INTERVAL_IN_SECOND = (10).toLong()
const val FASTEST_UPDATE_INTERVAL_IN_SECOND = UPDATE_INTERVAL_IN_SECOND / 2
const val DELETE_AFTER_INTERVAL_IN_SECOND = (60 * 60 ).toLong()
const val notifyMessage = "You are in your way to: location"

const val UPDATE_UI = "UPDATE_UI"

class AppForegroundService : ForegroundService() {

    inner class LocalBinder : Binder() {
        fun getService() = this@AppForegroundService
    }

    override fun onCreate() {
        init(
            locationListener = locationListener,
            intervalInSecond = UPDATE_INTERVAL_IN_SECOND,
            fastIntervalInSecond = FASTEST_UPDATE_INTERVAL_IN_SECOND,
            removeAfterInSecond = DELETE_AFTER_INTERVAL_IN_SECOND,
            notificationTitle = getString(R.string.app_name),
            notificationMessage = notifyMessage,
            mBinder = LocalBinder(),
            appService = AppForegroundService::class.java
        )
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val startedFromNotification: Boolean =
            intent.getBooleanExtra(ACTION_STARTED_FROM_NOTIFICATION, false)
        if (startedFromNotification) {
            // Remove location update form notification
            Log.e(TAG, "removeLocationUpdate: 2")
            removeLocationUpdate("Form notification")
            stopSelf()
        }
        return START_NOT_STICKY
    }

    @Nullable
    override fun onBind(intent: Intent): IBinder? {
        return mBinder
    }

    private val locationListener: LocationListener = object : LocationListener {
        override  fun onLocation(location: Location) {
            Log.e(TAG, "onLocation: " + location.latitude + " - " + location.longitude)
            // To Update notify message
            notificationMessage = "onLocation: " + location.latitude + " - " + location.longitude

            onNewLocation(location)
        }

        override fun onRemoved(tag: String) {
            Log.e(TAG, "onRemoved: $tag")

        }
    }

    private fun onNewLocation(location: Location) {
        if (checkIfServiceRunningForeGround(this)) {
             updateUI(location)
        }
    }

    private fun updateUI(location: Location) {
        val intent = Intent(UPDATE_UI)
        intent.putExtra("location", location)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }
}