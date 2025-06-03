package com.example.xpectrumapp.logic

import android.util.Log
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.Response

// Modelo de datos para la respuesta de boletos
data class BoletoResponse(
    val boletoid: Int,
    val reservaid: Int,
    val codigoboleto: String,
    val fechaemision: String,
    val estadoboleto: String,
    val reserva: String?,
    val checkins: String?
)

// Interfaz API para boletos
interface ApiService {
    @GET("boletos/searchbycodigo/{codigo}")
    suspend fun obtenerBoleto(@Path("codigo") codigo: String): Response<List<BoletoResponse>>
}

class LectorQR {
    private val apiService: ApiService

    init {
        // Configurar Retrofit con tu API real
        val retrofit = Retrofit.Builder()
            .baseUrl("http://apiswagger.somee.com/api/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit.create(ApiService::class.java)
    }

    /**
     * Consulta la API con el código obtenido del QR
     */
    suspend fun consultarCodigoQR(codigo: String): BoletoResponse? {
        return try {
            Log.d("LectorQR", "Consultando código: $codigo")
            val response = apiService.obtenerBoleto(codigo)

            if (response.isSuccessful) {
                val boletos = response.body()
                if (!boletos.isNullOrEmpty()) {
                    Log.d("LectorQR", "Boleto encontrado: ${boletos[0]}")
                    boletos[0] // Retorna el primer boleto de la lista
                } else {
                    Log.w("LectorQR", "No se encontraron boletos para el código: $codigo")
                    null
                }
            } else {
                Log.e("LectorQR", "Error HTTP: ${response.code()} - ${response.message()}")
                null
            }
        } catch (e: Exception) {
            Log.e("LectorQR", "Error en consulta API", e)
            null
        }
    }
}