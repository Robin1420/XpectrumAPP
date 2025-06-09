package com.example.xpectrumapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
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
    private lateinit var gifImageView: ImageView

    init {
        // Inicializar el adapter con una lista vacía
        vuelosAdapter = VuelosAdapter(emptyList()) { vuelo ->
            // Cuando se hace click en un vuelo, ir al escáner QR con la información
            val intent = Intent(this, QRScannerActivity::class.java).apply {
                putExtra("DESTINO", vuelo.aeropuertoDestino)
                putExtra("FECHA_VIAJE", "${vuelo.fechaSalida} ${vuelo.horaSalida}")
                putExtra("TIPO_VIAJE", vuelo.tipoViaje)
                putExtra("CLASE", vuelo.clase)
                putExtra("PRECIO_USD", vuelo.precioUSD)
                putExtra("PRECIO_PEN", vuelo.precioPEN)
            }
            startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configurar la ventana para mostrar la barra de estado y navegación
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        
        // Configurar colores de las barras del sistema
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        
        // Configurar el comportamiento de las barras del sistema
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // Asegurar que el contenido no se dibuje detrás de las barras del sistema
        window.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        
        setContentView(R.layout.activity_vuelos)
        
        // Ocultar ActionBar
        supportActionBar?.hide()

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
        gifImageView = findViewById(R.id.gifImageView)
    }

    private fun setupRecyclerView() {
        recyclerVuelos.layoutManager = LinearLayoutManager(this)
        recyclerVuelos.adapter = vuelosAdapter
    }

    private fun setupApi() {
        val retrofit = Retrofit.Builder()
            .baseUrl("http://apiswagger.somee.com/api/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        vuelosApiService = retrofit.create(VuelosApiService::class.java)
        
        // Cargar GIF usando Glide
        Glide.with(this)
            .asGif()
            .load(R.drawable.gif_vuelos) // Asegúrate de tener un GIF llamado gif_vuelos en tu carpeta drawable
            .apply(RequestOptions.centerCropTransform())
            .into(gifImageView)
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
                    vuelosApiService.obtenerVuelosProgramados()
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