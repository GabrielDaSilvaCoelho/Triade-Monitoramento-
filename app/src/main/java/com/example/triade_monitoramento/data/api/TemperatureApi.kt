package com.example.triade_monitoramento.data.api

import com.example.triade_monitoramento.data.model.LatestTemperatureDto
import com.example.triade_monitoramento.data.model.TemperaturePointDto
import retrofit2.http.GET
import retrofit2.http.Query

interface TemperatureApi {

    @GET("api/temperatura/latest")
    suspend fun getLatest(
        @Query("id") id: String
    ): LatestTemperatureDto

    @GET("api/temperatura/history")
    suspend fun getHistory(
        @Query("id") id: String,
        @Query("range") range: String,
        @Query("every") every: String
    ): List<TemperaturePointDto>

    @GET("api/temperatura/history")
    suspend fun getHistoryByPeriod(
        @Query("id") id: String,
        @Query("start") start: String,
        @Query("stop") stop: String,
        @Query("every") every: String
    ): List<TemperaturePointDto>
}