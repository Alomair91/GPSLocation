Android GPSLocation
-------

- Help library written in Kotlin for handling GPS in Android Studio.

- This library facilitates GPS handling for developers who need to get the current location and address from a user's phone.

- The library allows you to retrieve the final location when the user is using the application or
  the background location when the user leaves the application, it also allows the user to choose
  the permissions through an interface that explains the reason for the service with the ability
  to modify the content of the interface, also when the user runs the location service in the
  background, the application will inform him by Send a permanent notice until the service stops.

- You can request permissions to take user location while the user is using the app or while the app is running in the background.

To get a Git project into your build:
-------

#### gradle
- Step 1. Add the JitPack repository to your build file
  (Add it in your root build.gradle at the end of repositories):

      allprojects {
            repositories {
        	    ...
        	    maven { url 'https://jitpack.io' }
            }
        }

- Step 2. Add the dependency

  [![](https://jitpack.io/v/Alomair91/GPSLocation.svg)](https://jitpack.io/#Alomair91/GPSLocation)

        dependencies {
            implementation 'com.github.Alomair91:GPSLocation:1.05'
        }


#### maven
- Step 1.

        <repositories>
            <repository>
                <id>jitpack.io</id>
                <url>https://jitpack.io</url>
            </repository>
        </repositories>

- Step 2. Add the dependency

        <dependency>
            <groupId>com.github.Alomair91</groupId>
            <artifactId>GPSLocation</artifactId>
            <version>1.05</version>
        </dependency>


How to use it?
-------
#### Foreground Location

- Step 1. Declare the [registerForActivityResult] object in your [Activity/Fragment] in case you want to start
  GPS directly from the application or pass null to [LocationRepository]:

        private val activityResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->
            viewModel.setIsGPSRequested(false)
            when (result.resultCode) {
                Activity.RESULT_OK ->
                    viewModel.startLocationUpdates()
                else -> {
                    // keep asking if imp or do whatever
                    viewModel.startLocationUpdates()
                }
            }
        }

- Step 2. Declare object of [GPSLocationViewModel] in your [Activity/Fragment]:

        private val locationType = LocationType.FINE_LOCATION
        private val intervalInSecond: Long = 60
        private val fastIntervalInSecond: Long = 30

        // You can customize the resources of [permission dialog] if you wish
        private val foregroundUiData = PermissionUIData.Foreground(
            hideUi = true,
            btn_approve = R.string.approve_location_access,
            btn_cancel = android.R.string.cancel,
        )

        private val viewModel: GPSLocationViewModel by viewModels {
            GPSLocationViewModelFactory(application, LocationRepository.getInstance(
                this, locationType, intervalInSecond, fastIntervalInSecond, activityResultLauncher,
                PermissionUIData(foreground = foregroundUiData)
            ))
        }

- Step 3. Handle [GPSLocationViewModel] events in your [Activity/Fragment]:

        // Receiving location updates if it's starting or not
        viewModel.receivingLocationUpdates.observe(this) { isRunning ->
            // Update UI...
        }

        // Receiving location data from [FusedLocationProviderClient]
        viewModel.receivingLocation.observe(this) { location ->
            Log.d(TAG, "CurrentLocation:  ${location.latitude} - ${location.longitude}")

            // Retrieve address data from [Geocoder] with the retrieved location
            viewModel.retrieveAddressDataFromGeocoder {
                Log.d(TAG, "Current Address: ${it.name} - ${it.address}")
            }
            // OR Retrieve address data from [Geocoder] using custom location
            viewModel.retrieveAddressDataFromGeocoder(location.latitude, location.longitude) {
                Log.d(TAG, "Current Address: ${it.name} - ${it.address}")
            }
        }

- Step 4. Finally, you can start/stop retrieve location update:

        // To start retrieve location updates
        viewModel.startLocationUpdates()
        
        // To stop location updates
        // Don't forget to stops location updates in [Activity/Fragment onPause]  if background  
        // permissions aren't approved.
        if (!viewModel.hasBackgroundPermissions) {
            viewModel.stopLocationUpdates()
        }




#### Background Location

- Step 1. Declare the broadcast receiver class in your app:

        private const val TAG = "LocationReceiver"
        class LocationBroadcastReceiver : BroadcastReceiver() {

            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == ACTION_PROCESS_UPDATES) {
            
                    // Checks for location availability changes.
                    LocationAvailability.extractLocationAvailability(intent).let { locationAvailability ->
                        @Suppress("SENSELESS_COMPARISON")
                        if (null != locationAvailability &&  !locationAvailability.isLocationAvailable) {
                            Log.d(TAG, "Location services in App are no longer available!")
                        }
                    }
        
                    LocationResult.extractResult(intent).let { locationResult ->
                        @Suppress("SENSELESS_COMPARISON")
                        if (null != locationResult && locationResult.locations.isNotEmpty()) {
                            var addressData:AddressData = toLocation(locationResult.locations.last(), isAppInForeground(context))
                            Log.d(TAG, "Background Location in App ${addressData.latitude} - ${addressData.longitude}")
        
                            // Retrieve address data from [Geocoder]
                            retrieveAddressDataFromGeocoder(context,addressData.latitude,addressData.longitude){
                                addressData = it
                                Log.d(TAG, "Background Location in App ${it.address}")
        
                                // Make notification to inform user when you retrieve his location in the background
                                makeStatusNotification(context.getString(R.string.background_location_starting),it.address,context)
                              
                                // Send address data [addressData] to the server or do whatever
        
                            }
        
                            // Send location data [addressData] to the server or do whatever
                        }
                    }
                }
            }
        }

- Step 2. Declare the [LocationBroadcastReceiver] in the [AndroidManifest] file in your app:

        <receiver
            android:name=".LocationBroadcastReceiver"
            android:exported="true">
            <intent-filter>
                <!--  Do not change the next line -->
                <action android:name="com.omairtech.gpslocation.util.ACTION_PROCESS_UPDATES" />
            </intent-filter>
        </receiver>

- Step 4. Do the first step in the foreground location instructions in case you want to start  
  GPS directly from your app.


- Step 5. Declare object of [GPSLocationViewModel] in your activity:

        private val locationType = LocationType.BACKGRPUND_LOCATION
        private val intervalInSecond: Long = 60
        private val fastIntervalInSecond: Long = 30

        // You can customize the resources of [permission dialog] if you wish
        private val backgroundUiData = PermissionUIData.Background(
            hideUi = false,
            btn_approve = R.string.approve_location_access,
            btn_cancel = android.R.string.cancel,
        )

        private val viewModel: GPSLocationViewModel by viewModels {
            @Suppress("UNCHECKED_CAST")
            GPSLocationViewModelFactory(application, LocationRepository.getInstance(
                this, locationType, intervalInSecond, fastIntervalInSecond, activityResultLauncher,
                PermissionUIData(background = foregroundUiData),
                // TODO: Don't forget to pass broadcast receiver class here 
                LocationBroadcastReceiver::class.java as Class<BroadcastReceiver>
            ))
        }

- Step 6. Finally Do the steps 3 and 4 from foreground location instructions

#### Select Location from map

- Step 1. Do the first step in the foreground location instructions in case you want to start  
  GPS directly from your app.

- Step 2. Do the second step in the foreground location instructions to create viewModel.

- Step 3. Declare the google api meta-data in the [AndroidManifest] file in your app:

          <!-- Add tools:replace="android:value" To replace default key in the library -->
          <meta-data
              android:name="com.google.android.geo.API_KEY"
              tools:replace="android:value" // 
              android:value="@string/google_api_key" />

- Step 4. Create an instance of [SelectLocationDialog] which will show a dialog to select location from map

        var selectedAddress:AddressData? = null
        SelectLocationDialog.newInstance(this,viewModel,selectedAddress){
            selectedAddress = it
            Log.d(TAG, "Selected Address: ${it.name} - ${it.address}")
        }



#### Please follow the simple project if you don't understand any of these instructions.

-------
#### Warning: If you are not going to use the background location, you must add the following line to the [AndroidManifest] file to remove the permission

        <uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" tools:node="remove" />
-------

Contributors:
-------
* [Eng: Mohammed Alomair](https://github.com/Alomair91)

License
-------

    Copyright (C) 2011 readyState Software Ltd
    Copyright (C) 2007 The Android Open Source Project

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

[![](https://jitpack.io/v/Alomair91/GPSLocation.svg)](https://jitpack.io/#Alomair91/GPSLocation)
