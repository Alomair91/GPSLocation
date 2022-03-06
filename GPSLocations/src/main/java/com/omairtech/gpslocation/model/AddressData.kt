package com.omairtech.gpslocation.model

import android.location.Address
import android.location.Location
import androidx.annotation.Keep
import com.omairtech.gpslocation.util.CURRENT_LOCATION
import java.io.Serializable

@Keep
data class AddressData(
    var id: Int = 0,
    var name: String = "",
    var address: String = "",
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var countryName: String? = "",
    var adminArea: String? = "",
    var subAdminArea: String? = "",
    var locality: String? = "",
    var subLocality: String? = "",
    var thoroughfare: String? = "",
    var subThoroughfare: String? = "",
    var foreground: Boolean? = true,
) : Serializable


fun toLocation(address: Location,foreground: Boolean): AddressData {
    return AddressData(
        id = CURRENT_LOCATION,
        name = "Current Location",
        latitude = address.latitude,
        longitude = address.longitude,
        foreground = foreground,
    )
}

fun toAddress(address: Address,foreground: Boolean): AddressData {
    return AddressData(
        CURRENT_LOCATION,
        "Current Address",
        address.getAddressLine(0),
        address.latitude,
        address.longitude,
        address.countryName,
        address.adminArea,
        address.subAdminArea,
        address.locality,
        address.subLocality,
        address.thoroughfare,
        address.subThoroughfare,
        foreground = foreground,
    )
}