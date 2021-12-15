package com.omairtech.gpslocation.util

import com.omairtech.gpslocation.model.AddressData

/**
 * Created by OmairTech on 01/04/2021.
 */
interface LocationListener {
    fun onFindCurrentLocation(latitude: Double, longitude: Double) {}
    fun onFindCurrentAddress(address: AddressData) {}
}