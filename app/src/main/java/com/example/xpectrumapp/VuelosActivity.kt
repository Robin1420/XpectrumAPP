package com.example.xpectrumapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.xpectrumapp.logic.VueloResponse
import com.example.xpectrumapp.logic.VuelosApiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class VuelosActivity : AppCompatActivity() {

    private lateinit var recyclerVuelos: RecyclerView
    private lateinit var btnEscanearQR: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEstado: TextView
    private lateinit var vuelosAdapter: VuelosAdapter
    private lateinit var vuelosApiService: VuelosApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vuelos)

        // Configurar ActionBar
        supportActionBar?.title = "Vuelos Programados"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Inicializar vistas
        initViews()

        // Configurar RecyclerView
        setupRecyclerView()

        // Configurar API
        setupApi()

        // Configurar eventos
        setupClickListeners()

        // Cargar vuelos
        cargarVuelosProgramados()
    }

    private fun initViews() {
        recyclerVuelos = findViewById(R.id.recyclerVuelos)
        btnEscanearQR = findViewById(R.id.btnEscanearQR)
        progressBar = findViewById(R.id.progressBar)
        tvEstado = findViewById(R.id.tvEstado)
    }

    private fun setupRecyclerView() {
        vuelosAdapter = VuelosAdapter(emptyList()) { vuelo ->
            // Cuando se hace click en un vuelo, ir al escáner QR con la información
            val intent = Intent(this, QRScannerActivity::class.java).apply {
                putExtra("DESTINO", vuelo.destino)
                putExtra("FECHA_VIAJE", vuelo.fecha_viaje)
                putExtra("TIPO_VIAJE", vuelo.tipo_viaje)
                putExtra("CLASE", vuelo.clase ?: "No especificada")
                putExtra("PRECIO_USD", vuelo.precio_usd)
                putExtra("PRECIO_PEN", vuelo.precio_pen)
            }
            startActivity(intent)
        }
        recyclerVuelos.layoutManager = LinearLayoutManager(this)
        recyclerVuelos.adapter = vuelosAdapter
    }

    private fun setupApi() {
        val retrofit = Retrofit.Builder()
            .baseUrl("http://www.apiswagger.somee.com/api/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        vuelosApiService = retrofit.create(VuelosApiService::class.java)
    }

    private fun setupClickListeners() {
        btnEscanearQR.setOnClickListener {
            val intent = Intent(this, QRScannerActivity::class.java)
            startActivity(intent)
        }
    }

    private fun cargarVuelosProgramados() {
        mostrarCargando(true)

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    vuelosApiService.obtenerVuelosProgramados("programado")
                }

                if (response.isSuccessful) {
                    val vuelos = response.body() ?: emptyList()
                    mostrarVuelos(vuelos)
                    Log.d("VuelosActivity", "Vuelos cargados: ${vuelos.size}")
                } else {
                    mostrarError("Error al cargar vuelos: ${response.code()}")
                    Log.e("VuelosActivity", "Error HTTP: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("VuelosActivity", "Error al cargar vuelos", e)
                mostrarError("Error de conexión: ${e.message}")
            } finally {
                mostrarCargando(false)
            }
        }
    }

    private fun mostrarVuelos(vuelos: List<VueloResponse>) {
        if (vuelos.isEmpty()) {
            tvEstado.text = "No hay vuelos programados disponibles"
            tvEstado.visibility = View.VISIBLE
            recyclerVuelos.visibility = View.GONE
        } else {
            vuelosAdapter.updateVuelos(vuelos)
            tvEstado.visibility = View.GONE
            recyclerVuelos.visibility = View.VISIBLE
            Toast.makeText(this, "Se cargaron ${vuelos.size} vuelos", Toast.LENGTH_SHORT).show()
        }
    }

    private fun mostrarError(mensaje: String) {
        tvEstado.text = mensaje
        tvEstado.visibility = View.VISIBLE
        recyclerVuelos.visibility = View.GONE
        Toast.makeText(this, mensaje, Toast.LENGTH_LONG).show()
    }

    private fun mostrarCargando(mostrar: Boolean) {
        progressBar.visibility = if (mostrar) View.VISIBLE else View.GONE
        btnEscanearQR.isEnabled = !mostrar
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}