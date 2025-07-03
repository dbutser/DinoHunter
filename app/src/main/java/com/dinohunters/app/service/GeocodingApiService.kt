package com.dinohunters.app.service

import com.dinohunters.app.data.model.GeocodingResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface GeocodingApiService {

    @GET("maps/api/geocode/json")
    suspend fun reverseGeocode(
        @Query("latlng") latlng: String,
        @Query("key") apiKey: String,
        // Фильтруем результаты, чтобы не получать лишнего
        @Query("result_type") resultType: String = "street_address|route|intersection|political|locality"
    ): GeocodingResponse
}