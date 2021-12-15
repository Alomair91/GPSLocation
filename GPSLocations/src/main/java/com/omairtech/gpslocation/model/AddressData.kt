package com.omairtech.gpslocation.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class AddressData(
    @SerializedName("id") var id: Int = 0,
    @SerializedName("name") var name: String = "",
    @SerializedName("address") var address: String = "",
    @SerializedName("lat") var lat: Double = 0.0,
    @SerializedName("lng") var lng: Double = 0.0,
    @SerializedName("countryName") var countryName: String? = "",
    @SerializedName("adminArea") var adminArea: String? = "",
    @SerializedName("subAdminArea") var subAdminArea: String? = "",
    @SerializedName("locality") var locality: String? = "",
    @SerializedName("subLocality") var subLocality: String? = "",
    @SerializedName("thoroughfare") var thoroughfare: String? = "",
    @SerializedName("subThoroughfare") var subThoroughfare: String? = "",
) :  Serializable