package com.example.xpectrumapp.logic

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

// Modelo de datos para la respuesta de vuelos (actualizado para tu nueva API)
data class VueloResponse(
    val codigoVuelo: String,
    val fechaSalida: String,
    val horaSalida: String,
    val fechaLlegada: String,
    val horaLlegada: String,
    val duracionHoras: Int,
    val duracionMinutos: Int,
    val estadoVueloFinal: String,
    val aeropuertoOrigen: String,
    val aeropuertoDestino: String,
    val aeronaveModelo: String,
    val aeronaveCapacidad: Int,
    val estadoVuelo: String,
    val tipoViaje: String,
    val clase: String,
    val beneficio: String?,
    val precioUSD: Double,
    val precioPEN: Double,
    val ciudadOrigen: String,
    val ciudadDestino: String,
    val nombreUsuario: String?,
    val diaSemana: String?
)

// Interfaz API para vuelos (actualizada para tu nueva API)
interface VuelosApiService {
    @GET("Vuelos/GetVuelos")
    suspend fun obtenerVuelosProgramados(): Response<List<VueloResponse>>
    
    @GET("Vuelos/GetVueloByCodigo/{codigoVuelo}")
    suspend fun obtenerVueloPorCodigo(
        @Path("codigoVuelo") codigoVuelo: String
    ): Response<VueloResponse>
}