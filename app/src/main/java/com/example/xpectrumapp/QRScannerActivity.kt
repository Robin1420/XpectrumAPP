package com.example.xpectrumapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.xpectrumapp.logic.LectorQR
import com.example.xpectrumapp.logic.VueloResponse
import com.example.xpectrumapp.logic.VuelosApiService
import com.google.gson.GsonBuilder
import com.google.zxing.integration.android.IntentIntegrator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class QRScannerActivity : AppCompatActivity() {

    companion object {
        private const val CAMERA_PERMISSION_REQUEST = 100
        private const val REQUEST_CODE_PICK_IMAGE = 101
    }

    private lateinit var tvResultado: TextView
    private lateinit var btnEscanear: Button
    private lateinit var btnCargarQR: Button
    private lateinit var lectorQR: LectorQR
    private lateinit var vuelosApiService: VuelosApiService
    private var codigoVueloActual: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            Log.d("QRScannerActivity", "Iniciando onCreate")
            setContentView(R.layout.activity_qr_scanner)

            // Configurar ActionBar
            supportActionBar?.title = "Escáner QR"
            supportActionBar?.setDisplayHomeAsUpEnabled(true)

            // Inicializar vistas
            btnEscanear = findViewById(R.id.btnEscanear)
            btnCargarQR = findViewById(R.id.btnVolver)
            tvResultado = findViewById(R.id.tvResultado)

            // Inicializar lógica de negocio
            lectorQR = LectorQR()

            // Configurar API
            val retrofit = Retrofit.Builder()
                .baseUrl("http://apiswagger.somee.com/api/")
                .addConverterFactory(GsonConverterFactory.create(GsonBuilder().create()))
                .build()
            vuelosApiService = retrofit.create(VuelosApiService::class.java)

            // Configurar botones
            setupClickListeners()

            // Mostrar información del vuelo si viene de la lista
            mostrarInformacionVuelo()

            Log.d("QRScannerActivity", "onCreate completado exitosamente")

        } catch (e: Exception) {
            Log.e("QRScannerActivity", "Error en onCreate", e)
            Toast.makeText(this, "Error al inicializar la aplicación", Toast.LENGTH_LONG).show()
        }
    }

    private fun mostrarInformacionVuelo() {
        val destino = intent.getStringExtra("DESTINO")
        val fechaViaje = intent.getStringExtra("FECHA_VIAJE")
        val tipoViaje = intent.getStringExtra("TIPO_VIAJE")
        val clase = intent.getStringExtra("CLASE")
        val precioUsd = intent.getDoubleExtra("PRECIO_USD", 0.0)
        val precioPen = intent.getDoubleExtra("PRECIO_PEN", 0.0)

        if (destino != null) {
            val infoVuelo = """
                ✈️ INFORMACIÓN DEL VUELO SELECCIONADO
                
                🎯 Destino: $destino
                📅 Fecha: ${fechaViaje ?: "No especificada"}
                👤 Tipo: ${tipoViaje?.trim() ?: "No especificado"}
                🎫 Clase: ${clase ?: "No especificada"}
                💰 Precio: USD ${String.format("%.2f", precioUsd)} / PEN ${String.format("%.2f", precioPen)}
                
                📱 Presiona SCAN para escanear el código QR del boleto
            """.trimIndent()

            tvResultado.text = infoVuelo
        } else {
            tvResultado.text = "📱 Presiona el botón SCAN para escanear un código QR de boleto"
        }
    }

    private fun setupClickListeners() {
        btnEscanear.setOnClickListener {
            if (tienePermisoCamara()) {
                iniciarEscaneoQR()
            } else {
                solicitarPermisoCamara()
            }
        }

        btnCargarQR.setOnClickListener {
            // Abrir galería para seleccionar imagen
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, REQUEST_CODE_PICK_IMAGE)
        }
    }

    private fun tienePermisoCamara(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun solicitarPermisoCamara() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_REQUEST
        )
    }

    private fun iniciarEscaneoQR() {
        try {
            val integrator = IntentIntegrator(this)
            integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
            integrator.setPrompt("Escanea el código QR del boleto")
            integrator.setCameraId(0)
            integrator.setBeepEnabled(true)
            integrator.initiateScan()
        } catch (e: Exception) {
            Log.e("QRScannerActivity", "Error al iniciar escaneo QR", e)
            Toast.makeText(this, "Error al abrir la cámara", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                iniciarEscaneoQR()
            } else {
                Toast.makeText(this, "Se necesita permiso de cámara para escanear códigos QR", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            val imageUri = data.data
            if (imageUri != null) {
                // Procesar la imagen seleccionada en un hilo de fondo
                CoroutineScope(Dispatchers.Main).launch {
                    val qrContent = withContext(Dispatchers.IO) {
                        decodeQRCodeFromImage(imageUri)
                    }
                    if (qrContent != null) {
                        tvResultado.text = "🔍 Buscando información del vuelo $qrContent..."
                        buscarVueloPorCodigo(qrContent)
                    } else {
                        Toast.makeText(this@QRScannerActivity, "No se detectó un QR en la imagen", Toast.LENGTH_LONG).show()
                    }
                }
            }
        } else {
            val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
            if (result != null) {
                if (result.contents != null) {
                    val codigoVuelo = result.contents.trim()
                    Log.d("QRScannerActivity", "Código de vuelo escaneado: $codigoVuelo")
                    tvResultado.text = "🔍 Buscando información del vuelo $codigoVuelo..."
                    buscarVueloPorCodigo(codigoVuelo)
                } else {
                    Toast.makeText(this, "Escaneo cancelado", Toast.LENGTH_SHORT).show()
                }
            } else {
                super.onActivityResult(requestCode, resultCode, data)
            }
        }
    }

    // Decodifica el QR de una imagen seleccionada
    private fun decodeQRCodeFromImage(imageUri: android.net.Uri): String? {
        try {
            val inputStream = contentResolver.openInputStream(imageUri)
            val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            if (bitmap != null) {
                val intArray = IntArray(bitmap.width * bitmap.height)
                bitmap.getPixels(intArray, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
                val source = com.google.zxing.RGBLuminanceSource(bitmap.width, bitmap.height, intArray)
                val binaryBitmap = com.google.zxing.BinaryBitmap(com.google.zxing.common.HybridBinarizer(source))
                val reader = com.google.zxing.MultiFormatReader()
                val result = reader.decode(binaryBitmap)
                return result.text
            }
        } catch (e: Exception) {
            Log.e("QRScannerActivity", "Error al decodificar QR de imagen", e)
        }
        return null
    }

    private fun buscarVueloPorCodigo(codigoVuelo: String) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    vuelosApiService.obtenerVueloPorCodigo(codigoVuelo)
                }

                if (response.isSuccessful) {
                    val vuelo = response.body()
                    if (vuelo != null) {
                        // Navegar a la pantalla de detalle del boleto con la información del vuelo
                        val intent = Intent(this@QRScannerActivity, BoletoDetailActivity::class.java).apply {
                            putExtra("VUELO_CODIGO", vuelo.codigoVuelo)
                            putExtra("DESTINO", vuelo.aeropuertoDestino)
                            putExtra("CIUDAD_DESTINO", vuelo.ciudadDestino)
                            putExtra("FECHA_SALIDA", "${vuelo.fechaSalida} ${vuelo.horaSalida}")
                            putExtra("FECHA_LLEGADA", "${vuelo.fechaLlegada} ${vuelo.horaLlegada}")
                            putExtra("TIPO_VIAJE", vuelo.tipoViaje)
                            putExtra("CLASE", vuelo.clase)
                            putExtra("PRECIO_USD", vuelo.precioUSD)
                            putExtra("PRECIO_PEN", vuelo.precioPEN)
                            putExtra("AERONAVE", "${vuelo.aeronaveModelo} (${vuelo.aeronaveCapacidad} pasajeros)")
                            putExtra("ESTADO", vuelo.estadoVuelo)
                        }
                        startActivity(intent)
                    } else {
                        tvResultado.text = "❌ No se encontró información para el vuelo $codigoVuelo"
                    }
                } else {
                    tvResultado.text = "❌ Error al buscar el vuelo: ${response.code()}"
                    Log.e("QRScannerActivity", "Error HTTP: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("QRScannerActivity", "Error al buscar vuelo", e)
                tvResultado.text = "❌ Error de conexión: ${e.message}"
            }
        }
    }
}