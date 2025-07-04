// Путь: app/src/main/java/com/dinohunters/app/service/GeocodingApiService.kt

package com.dinohunters.app.service

import com.dinohunters.app.data.model.GeocodingResponse
import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

interface GeocodingApiService {

    /**
     * Старый метод для обратного геокодирования. Полезен, чтобы узнать адрес, но не для проверки доступности.
     * Он остается без изменений.
     */
    @GET("maps/api/geocode/json")
    suspend fun reverseGeocode(
        @Query("latlng") latlng: String,
        @Query("key") apiKey: String,
        // Фильтруем результаты, чтобы не получать лишнего
        @Query("result_type") resultType: String = "street_address|route|intersection|political|locality"
    ): GeocodingResponse


    /**
     * [НОВЫЙ МЕТОД] Привязывает заданные GPS-координаты к ближайшей дороге.
     * Это поможет нам размещать зоны в доступных для игрока местах.
     * Endpoint: https://roads.googleapis.com/v1/snapToRoads
     */
    @GET("https://roads.googleapis.com/v1/snapToRoads")
    suspend fun snapToRoad(
        @Query("path") path: String, // Координаты в формате "широта,долгота"
        @Query("interpolate") interpolate: Boolean = false, // Нам не нужна интерполяция, только одна точка
        @Query("key") apiKey: String
    ): SnapToRoadResponse // Возвращает нашу новую модель данных
}


// --- [НОВЫЕ МОДЕЛИ ДАННЫХ] для ответа от Roads API ---

/**
 * Корневой объект ответа от Roads API.
 */
data class SnapToRoadResponse(
    // Список "привязанных" точек. В нашем случае там будет либо 0, либо 1 элемент.
    @SerializedName("snappedPoints")
    val snappedPoints: List<SnappedPoint>?
)

/**
 * Представляет одну точку, привязанную к дороге.
 */
data class SnappedPoint(
    // Содержит сами координаты
    @SerializedName("location")
    val location: RoadLocation,

    // Индекс исходной точки в запросе. Для нас всегда будет 0.
    @SerializedName("originalIndex")
    val originalIndex: Int,

    // Уникальный идентификатор дороги. Нам он пока не нужен.
    @SerializedName("placeId")
    val placeId: String
)

/**
 * Непосредственно координаты точки на дороге.
 */
data class RoadLocation(
    @SerializedName("latitude")
    val latitude: Double,

    @SerializedName("longitude")
    val longitude: Double
)