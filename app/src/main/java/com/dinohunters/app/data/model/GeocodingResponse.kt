package com.dinohunters.app.data.model

import com.google.gson.annotations.SerializedName

data class GeocodingResponse(
    val results: List<GeocodingResult>,
    val status: String
)

data class GeocodingResult(
    @SerializedName("formatted_address")
    val formattedAddress: String?,
    val types: List<String>
)