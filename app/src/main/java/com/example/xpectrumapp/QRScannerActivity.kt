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
import com.example.xpectrumapp.logic.BoletoResponse
import com.example.xpectrumapp.logic.LectorQR
import com.google.zxing.integration.android.IntentIntegrator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class QRScannerActivity : AppCompatActivity() {

    private lateinit var lectorQR: LectorQR
    private lateinit var btnEscanear: Button
    private lateinit var btnVolver: Button
    private lateinit var tvResultado: TextView

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
            btnVolver = findViewById(R.id.btnVolver)
            tvResultado = findViewById(R.id.tvResultado)

            // Inicializar lógica de negocio
            lectorQR = LectorQR()

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

        btnVolver.setOnClickListener {
            finish()
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
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)

        if (result != null) {
            if (result.contents != null) {
                val codigoQR = result.contents
                Log.d("QRScannerActivity", "Código QR escaneado: $codigoQR")

                // Mostrar mensaje de carga
                tvResultado.text = "📱 Código escaneado: $codigoQR\n\n🔍 Consultando información del boleto..."

                // Consultar la API con el código
                consultarAPI(codigoQR)
            } else {
                Toast.makeText(this, "Escaneo cancelado", Toast.LENGTH_SHORT).show()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun consultarAPI(codigo: String) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val boleto = withContext(Dispatchers.IO) {
                    lectorQR.consultarCodigoQR(codigo)
                }

                if (boleto != null) {
                    // Navegar a la nueva pantalla de detalle del boleto
                    navegarADetalleBoleto(boleto)
                } else {
                    tvResultado.text = """
                        ❌ BOLETO NO ENCONTRADO
                        
                        📱 Código escaneado: $codigo
                        
                        ⚠️ El código QR no corresponde a ningún boleto válido en el sistema.
                        
                        💡 Presiona SCAN para intentar nuevamente.
                    """.trimIndent()
                }
            } catch (e: Exception) {
                Log.e("QRScannerActivity", "Error en consultarAPI", e)
                tvResultado.text = """
                    ❌ ERROR DE CONEXIÓN
                    
                    📱 Código escaneado: $codigo
                    
                    🚫 Error: ${e.message}
                    
                    🔄 Verifica tu conexión a internet e intenta nuevamente.
                """.trimIndent()
            }
        }
    }

    private fun navegarADetalleBoleto(boleto: BoletoResponse) {
        val intent = Intent(this, BoletoDetailActivity::class.java).apply {
            putExtra("CODIGO_BOLETO", boleto.codigoboleto)
            putExtra("BOLETO_ID", boleto.boletoid)
            putExtra("RESERVA_ID", boleto.reservaid)
            putExtra("FECHA_EMISION", boleto.fechaemision)
            putExtra("ESTADO_BOLETO", boleto.estadoboleto)
        }
        startActivity(intent)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    companion object {
        private const val CAMERA_PERMISSION_REQUEST = 100
    }
}