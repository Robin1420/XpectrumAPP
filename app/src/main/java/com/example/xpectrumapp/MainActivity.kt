package com.example.xpectrumapp

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.xpectrumapp.logic.BoletoResponse
import com.example.xpectrumapp.logic.LectorQR
import com.google.zxing.integration.android.IntentIntegrator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : Activity() {

    private lateinit var lectorQR: LectorQR
    private lateinit var btnEscanear: Button
    private lateinit var tvResultado: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            Log.d("MainActivity", "Iniciando onCreate")
            setContentView(R.layout.activity_main)

            // Inicializar vistas
            btnEscanear = findViewById(R.id.btnEscanear)
            tvResultado = findViewById(R.id.tvResultado)

            // Inicializar lógica de negocio
            lectorQR = LectorQR()

            // Configurar botón de escaneo
            btnEscanear.setOnClickListener {
                if (tienePermisoCamara()) {
                    iniciarEscaneoQR()
                } else {
                    solicitarPermisoCamara()
                }
            }

            // Mensaje inicial
            tvResultado.text = "Presiona el botón para escanear un código QR de boleto"

            Log.d("MainActivity", "onCreate completado exitosamente")

        } catch (e: Exception) {
            Log.e("MainActivity", "Error en onCreate", e)
            Toast.makeText(this, "Error al inicializar la aplicación", Toast.LENGTH_LONG).show()
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
            Log.e("MainActivity", "Error al iniciar escaneo QR", e)
            Toast.makeText(this, "Error al abrir la cámara", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
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
                Log.d("MainActivity", "Código QR escaneado: $codigoQR")

                // Mostrar mensaje de carga
                tvResultado.text = "Código escaneado: $codigoQR\n\nConsultando información del boleto..."

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
        // Usamos CoroutineScope en lugar de lifecycleScope
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val boleto = withContext(Dispatchers.IO) {
                    lectorQR.consultarCodigoQR(codigo)
                }

                if (boleto != null) {
                    mostrarDatosBoleto(boleto)
                } else {
                    tvResultado.text = """
                        ❌ Boleto no encontrado
                        
                        Código escaneado: $codigo
                        
                        El código QR no corresponde a ningún boleto válido en el sistema.
                    """.trimIndent()
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error en consultarAPI", e)
                tvResultado.text = """
                    ❌ Error de conexión
                    
                    Código escaneado: $codigo
                    
                    Error: ${e.message}
                    
                    Verifica tu conexión a internet e intenta nuevamente.
                """.trimIndent()
            }
        }
    }

    private fun mostrarDatosBoleto(boleto: BoletoResponse) {
        // Formatear la fecha para mostrarla más legible
        val fechaFormateada = try {
            val formatoEntrada = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val formatoSalida = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val fecha = formatoEntrada.parse(boleto.fechaemision)
            formatoSalida.format(fecha ?: Date())
        } catch (e: Exception) {
            boleto.fechaemision
        }

        // Determinar el emoji según el estado
        val estadoEmoji = when (boleto.estadoboleto.lowercase()) {
            "emitido" -> "✅"
            "cancelado" -> "❌"
            "usado" -> "🎫"
            else -> "📋"
        }

        val resultado = """
            $estadoEmoji INFORMACIÓN DEL BOLETO
            
            🎫 Código: ${boleto.codigoboleto}
            🆔 ID Boleto: ${boleto.boletoid}
            🔗 ID Reserva: ${boleto.reservaid}
            📅 Fecha Emisión: $fechaFormateada
            📊 Estado: ${boleto.estadoboleto}
            
            ${if (boleto.estadoboleto.equals("Emitido", ignoreCase = true))
            "✅ Boleto válido y listo para usar"
        else
            "⚠️ Verificar estado del boleto"}
        """.trimIndent()

        tvResultado.text = resultado

        // Mostrar toast con el resultado
        val mensaje = when (boleto.estadoboleto.lowercase()) {
            "emitido" -> "Boleto válido encontrado"
            "cancelado" -> "Boleto cancelado"
            "usado" -> "Boleto ya utilizado"
            else -> "Boleto encontrado"
        }
        Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val CAMERA_PERMISSION_REQUEST = 100
    }
}