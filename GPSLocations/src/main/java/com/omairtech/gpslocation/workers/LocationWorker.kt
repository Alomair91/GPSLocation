package com.omairtech.gpslocation.workers

import android.content.Context
import android.location.Geocoder
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.google.gson.Gson
import com.omairtech.gpslocation.model.toAddress
import com.omairtech.gpslocation.util.KEY_ADDRESS_DATA
import com.omairtech.gpslocation.util.KEY_LAT
import com.omairtech.gpslocation.util.KEY_LONG
import java.util.*

private const val TAG = "LocationWorker"
/**
 * Get address data from [Geocoder]
 */
internal class LocationWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {

    override fun doWork(): Result {
        val appContext = applicationContext

        val lat:Double = inputData.getDouble(KEY_LAT,0.0)
        val long:Double = inputData.getDouble(KEY_LONG,0.0)

        return try {
            if (lat == 0.0 || long == 0.0 ) {
                Log.e(TAG, "Geocoder: " +  "Invalid input data")
                throw IllegalArgumentException("Invalid input uri")
            }

            if (!Geocoder.isPresent()) {
                Log.e(TAG, "Geocoder: " +  "No geocoder available")
                Result.failure()
            }

            val geocoder = Geocoder(appContext, Locale.getDefault())
            val addresses = geocoder.getFromLocation(lat, long, 1)
            if (addresses != null && addresses.size > 0) {
                val addressData = toAddress(addresses[0],true)
                Log.d(TAG, "Geocoder: $addressData")

                val outputData = workDataOf(KEY_ADDRESS_DATA to Gson().toJson(addressData))

                Result.success(outputData)
            } else {
                Log.e(TAG, "Geocoder: " +  "No address found")
                Result.failure()
            }
        } catch (throwable: Throwable) {
            Log.e("Geocoder","Error",throwable)
            Result.failure()
        }
    }
}