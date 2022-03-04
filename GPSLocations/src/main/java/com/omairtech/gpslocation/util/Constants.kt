/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:JvmName("Constants")

package com.omairtech.gpslocation.util

// Notification Channel constants

// Name of Notification Channel for verbose notifications of [location & background work]
@JvmField val VERBOSE_NOTIFICATION_CHANNEL_NAME: CharSequence = "Verbose GPSLocations Notifications"
const val VERBOSE_NOTIFICATION_CHANNEL_DESCRIPTION = "Shows notifications whenever work starts"
const val CHANNEL_ID = "VERBOSE_NOTIFICATION"
const val NOTIFICATION_ID = 651234

const val TAG = "GPSLocations"
const val ACTION_PROCESS_UPDATES = "com.omairtech.gpslocation.action.PROCESS_UPDATES"


// The key of location
const val KEY_LAT = "KEY_LAT"
const val KEY_LONG = "KEY_LONG"
const val TAG_FULL_ADDRESS = "TAG_FULL_ADDRESS"
const val KEY_ADDRESS_DATA = "KEY_ADDRESS_DATA"
const val CURRENT_LOCATION = -1


// The name of the location work
const val LOCATION_WORK_NAME = "location_work"

// Other keys
const val PLAY_SERVICES_RESOLUTION_REQUEST = 0x23167
