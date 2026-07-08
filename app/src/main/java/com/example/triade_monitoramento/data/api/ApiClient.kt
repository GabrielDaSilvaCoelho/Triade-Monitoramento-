package com.example.triade_monitoramento.data.api

import okhttp3.Credentials
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    private const val BASE_URL = "http://192.168.1.25:1880/"

    private const val USERNAME = "reader"
    private const val PASSWORD = "reader"

    private val httpClient: OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .callTimeout(20, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val credentials = Credentials.basic(
                    USERNAME,
                    PASSWORD
                )

                val request = chain.request()
                    .newBuilder()
                    .header("Authorization", credentials)
                    .build()

                chain.proceed(request)
            }
            .build()

    val temperatureApi: TemperatureApi =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(httpClient)
            .addConverterFactory(
                GsonConverterFactory.create()
            )
            .build()
            .create(TemperatureApi::class.java)
}