@file:JvmName("Constants")

package com.omairtech.gpslocation.util

// Notification Channel constants
// Name of Notification Channel for verbose notifications of background location
@JvmField val VERBOSE_NOTIFICATION_CHANNEL_NAME: CharSequence = "Verbose GPSLocations Notifications"
const val VERBOSE_NOTIFICATION_CHANNEL_DESCRIPTION = "Shows notifications whenever work starts"
const val CHANNEL_ID = "VERBOSE_NOTIFICATION"
const val NOTIFICATION_ID = 651234

// The key of location
const val TAG = "GPSLocations"
const val ACTION_PROCESS_UPDATES = "com.omairtech.gpslocation.action.PROCESS_UPDATES"
const val CURRENT_LOCATION = -1

// Other keys
const val PLAY_SERVICES_RESOLUTION_REQUEST = 0x23167
