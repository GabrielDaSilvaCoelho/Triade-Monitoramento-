package com.example.triade_monitoramento.data.api

import com.example.triade_monitoramento.data.model.LatestTemperatureDto
import com.example.triade_monitoramento.data.model.TemperaturePointDto
import com.example.triade_monitoramento.data.remote.dto.PortaEventoDto
import com.example.triade_monitoramento.data.remote.dto.PortaEventosResponseDto
import com.example.triade_monitoramento.ui.sensor.PortaConfigDto
import com.example.triade_monitoramento.ui.sensor.PortaConfigRequestDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface TemperatureApi {

    @GET("api/porta/config")
    suspend fun buscarConfigPorta(
        @Query("id") sensorId: String
    ): PortaConfigDto

    @POST("api/porta/config")
    suspend fun salvarConfigPorta(
        @Body body: PortaConfigRequestDto
    ): PortaConfigDto

    @GET("api/porta/eventos")
    suspend fun buscarEventosPorta(
        @Query("id") sensorId: String,
        @Query("yellow") yellow: Int,
        @Query("red") red: Int
    ): PortaEventosResponseDto


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