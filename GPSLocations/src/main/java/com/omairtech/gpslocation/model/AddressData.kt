package com.omairtech.gpslocation.model

import android.location.Address
import android.location.Location
import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import com.omairtech.gpslocation.util.CURRENT_LOCATION
import java.io.Serializable

@Keep
data class AddressData(
    @SerializedName("id") var id: Int = 0,
    @SerializedName("name") var name: String = "",
    @SerializedName("address") var address: String = "",
    @SerializedName("latitude") var latitude: Double = 0.0,
    @SerializedName("longitude") var longitude: Double = 0.0,
    @SerializedName("countryName") var countryName: String? = "",
    @SerializedName("adminArea") var adminArea: String? = "",
    @SerializedName("subAdminArea") var subAdminArea: String? = "",
    @SerializedName("locality") var locality: String? = "",
    @SerializedName("subLocality") var subLocality: String? = "",
    @SerializedName("thoroughfare") var thoroughfare: String? = "",
    @SerializedName("subThoroughfare") var subThoroughfare: String? = "",
    @SerializedName("foreground") var foreground: Boolean? = true,

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