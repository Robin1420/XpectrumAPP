package com.example.xpectrumapp.logic

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

// Modelo de datos para la respuesta de vuelos (actualizado para tu nueva API)
data class VueloResponse(
    val destino: String,
    val tipo_viaje: String,
    val fecha_viaje: String,
    val clase: String?,
    val beneficio: String?,
    val precio_usd: Double,
    val precio_pen: Double,
    val tasas_incluidas: String
)

// Interfaz API para vuelos (actualizada para tu nueva API)
interface VuelosApiService {
    @GET("vueloes/resumen")
    suspend fun obtenerVuelosProgramados(
        @Query("estadoVuelo") estadoVuelo: String = "programado"
    ): Response<List<VueloResponse>>
}