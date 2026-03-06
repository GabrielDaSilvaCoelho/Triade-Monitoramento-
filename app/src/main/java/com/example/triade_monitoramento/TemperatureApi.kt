package com.example.triade_monitoramento

import retrofit2.http.GET
import retrofit2.http.Query

interface TemperatureApi {

    @GET("api/temperatura/latest")
    suspend fun getLatest(
        @Query("id") id: String
    ): TemperatureLatestDto

    @GET("api/temperatura/history")
    suspend fun getHistory(
        @Query("id") id: String,
        @Query("range") range: String = "1h",
        @Query("every") every: String = "10s"
    ): List<TemperaturePointDto>

    @GET("api/temperatura/history")
    suspend fun getHistoryByPeriod(
        @Query("id") id: String,
        @Query("start") start: String,
        @Query("stop") stop: String,
        @Query("every") every: String = "10s"
    ): List<TemperaturePointDto>
}