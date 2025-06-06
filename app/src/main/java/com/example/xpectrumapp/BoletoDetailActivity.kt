package com.example.xpectrumapp

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.xpectrumapp.logic.BoletoResponse
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class BoletoDetailActivity : AppCompatActivity() {

    private lateinit var boletoContainer: View
    private lateinit var tvCodigoBoleto: TextView
    private lateinit var tvBoletoId: TextView
    private lateinit var tvReservaId: TextView
    private lateinit var tvFechaEmision: TextView
    private lateinit var tvEstadoBoleto: TextView
    private lateinit var tvCodigoBarras: TextView
    private lateinit var btnDescargarPDF: Button
    private lateinit var btnVolver: Button

    private var boletoData: BoletoResponse? = null

    companion object {
        private const val STORAGE_PERMISSION_REQUEST = 101
        private const val TAG = "BoletoDetailActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_boleto_detail)

        // Configurar ActionBar
        supportActionBar?.title = "Detalle del Boleto"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        initViews()
        setupClickListeners()
        loadBoletoData()
    }

    private fun initViews() {
        boletoContainer = findViewById(R.id.boletoContainer)
        tvCodigoBoleto = findViewById(R.id.tvCodigoBoleto)
        tvBoletoId = findViewById(R.id.tvBoletoId)
        tvReservaId = findViewById(R.id.tvReservaId)
        tvFechaEmision = findViewById(R.id.tvFechaEmision)
        tvEstadoBoleto = findViewById(R.id.tvEstadoBoleto)
        tvCodigoBarras = findViewById(R.id.tvCodigoBarras)
        btnDescargarPDF = findViewById(R.id.btnDescargarPDF)
        btnVolver = findViewById(R.id.btnVolver)
    }

    private fun setupClickListeners() {
        btnDescargarPDF.setOnClickListener {
            if (checkStoragePermission()) {
                generarYGuardarPDF()
            } else {
                requestStoragePermission()
            }
        }

        btnVolver.setOnClickListener {
            finish()
        }
    }

    private fun loadBoletoData() {
        // Obtener datos del intent
        val codigoBoleto = intent.getStringExtra("CODIGO_BOLETO") ?: ""
        val boletoId = intent.getIntExtra("BOLETO_ID", 0)
        val reservaId = intent.getIntExtra("RESERVA_ID", 0)
        val fechaEmision = intent.getStringExtra("FECHA_EMISION") ?: ""
        val estadoBoleto = intent.getStringExtra("ESTADO_BOLETO") ?: ""

        // Crear objeto BoletoResponse
        boletoData = BoletoResponse(
            boletoid = boletoId,
            reservaid = reservaId,
            codigoboleto = codigoBoleto,
            fechaemision = fechaEmision,
            estadoboleto = estadoBoleto,
            reserva = null,
            checkins = null
        )

        // Mostrar datos en la UI
        mostrarDatosBoleto()
    }

    private fun mostrarDatosBoleto() {
        boletoData?.let { boleto ->
            tvCodigoBoleto.text = boleto.codigoboleto
            tvBoletoId.text = "ID: ${boleto.boletoid}"
            tvReservaId.text = "Reserva: ${boleto.reservaid}"

            // Formatear fecha
            val fechaFormateada = formatearFecha(boleto.fechaemision)
            tvFechaEmision.text = fechaFormateada

            tvEstadoBoleto.text = boleto.estadoboleto.uppercase()
            tvCodigoBarras.text = "||||| |||| ||||| |||| |||||"

            // Cambiar color según estado
            when (boleto.estadoboleto.lowercase()) {
                "emitido" -> {
                    tvEstadoBoleto.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
                }
                "cancelado" -> {
                    tvEstadoBoleto.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
                }
                else -> {
                    tvEstadoBoleto.setTextColor(ContextCompat.getColor(this, android.R.color.holo_orange_dark))
                }
            }
        }
    }

    private fun formatearFecha(fecha: String): String {
        return try {
            val formatoEntrada = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val formatoSalida = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val fechaDate = formatoEntrada.parse(fecha)
            formatoSalida.format(fechaDate ?: Date())
        } catch (e: Exception) {
            fecha
        }
    }

    private fun checkStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            true
        } else {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                STORAGE_PERMISSION_REQUEST
            )
        }
    }

    private fun generarYGuardarPDF() {
        try {
            Log.d(TAG, "=== INICIANDO GENERACIÓN DE PDF ===")

            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
            val page = pdfDocument.startPage(pageInfo)

            val canvas = page.canvas

            // Dibujar el boarding pass estilo VivaAir
            dibujarBoardingPassVivaAir(canvas)

            pdfDocument.finishPage(page)

            val nombreArchivo = "BoardingPass_${boletoData?.codigoboleto}_${System.currentTimeMillis()}.pdf"
            val uri = guardarPDFEnDescargas(pdfDocument, nombreArchivo)
            pdfDocument.close()

            if (uri != null) {
                Toast.makeText(this, "✅ Boarding Pass guardado en Descargas: $nombreArchivo", Toast.LENGTH_LONG).show()
                Log.d(TAG, "=== PDF GUARDADO EXITOSAMENTE ===")
            } else {
                Toast.makeText(this, "❌ Error al guardar el Boarding Pass", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "=== ERROR AL GUARDAR PDF ===")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Excepción en generarYGuardarPDF", e)
            Toast.makeText(this, "❌ Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun dibujarBoardingPassVivaAir(canvas: Canvas) {
        boletoData?.let { boleto ->
            val paint = Paint().apply {
                isAntiAlias = true
            }

            // === HEADER SUPERIOR ===
            paint.color = Color.BLACK
            paint.textSize = 12f
            paint.textAlign = Paint.Align.LEFT
            canvas.drawText(obtenerFechaActual(), 50f, 30f, paint)

            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText("Pase de abordar en línea", 545f, 30f, paint)
            paint.textAlign = Paint.Align.LEFT

            // Texto informativo
            paint.textSize = 10f
            paint.color = Color.GRAY
            canvas.drawText("Si viaja solo con equipaje de mano, vaya directamente a la sala de espera", 50f, 50f, paint)

            // === BORDE PRINCIPAL DEL BOARDING PASS ===
            paint.color = Color.BLACK
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2f
            canvas.drawRect(50f, 70f, 545f, 620f, paint)

            // === HEADER CON LOGO Y TÍTULO ===
            paint.style = Paint.Style.FILL

            // Logo "Xpectrum"
            paint.color = Color.parseColor("#E53E3E")
            paint.textSize = 24f
            paint.isFakeBoldText = true
            canvas.drawText("Xpectrum", 70f, 110f, paint)

            paint.textSize = 10f
            paint.isFakeBoldText = false
            canvas.drawText("Operated by Xpectrum Peru", 70f, 125f, paint)

            // "Pase de abordar" en el lado derecho
            paint.color = Color.BLACK
            paint.textSize = 18f
            paint.isFakeBoldText = true
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText("Pase de abordar", 525f, 110f, paint)

            paint.textSize = 10f
            paint.isFakeBoldText = false
            canvas.drawText("Online boarding pass", 525f, 125f, paint)
            paint.textAlign = Paint.Align.LEFT

            // === INFORMACIÓN DEL PASAJERO ===
            paint.color = Color.BLACK
            paint.textSize = 10f
            canvas.drawText("Nombre del pasajero/Name of passenger", 70f, 155f, paint)

            paint.textSize = 14f
            paint.isFakeBoldText = true
            canvas.drawText("PASAJERO/PASSENGER NAME", 70f, 175f, paint)

            // === INFORMACIÓN DEL VUELO ===
            paint.textSize = 10f
            paint.isFakeBoldText = false
            canvas.drawText("Vuelo No./Flight no.", 70f, 200f, paint)
            canvas.drawText("Grupo/Group", 170f, 200f, paint)
            canvas.drawText("Asiento/Seat", 250f, 200f, paint)

            paint.textSize = 20f
            paint.isFakeBoldText = true
            canvas.drawText("XP ${boleto.boletoid}", 70f, 225f, paint)
            canvas.drawText("4", 170f, 225f, paint)
            canvas.drawText("9B", 250f, 225f, paint)

            // === CÓDIGOS DE AEROPUERTOS ===
            paint.textSize = 40f
            paint.isFakeBoldText = true
            canvas.drawText("LIM", 420f, 200f, paint)
            paint.textSize = 10f
            paint.isFakeBoldText = false
            canvas.drawText("Lima - Jorge Chavez (LIM)", 420f, 215f, paint)

            // === CÓDIGO DE RESERVA ===
            paint.textSize = 10f
            canvas.drawText("Código de reserva/Booking number: ${boleto.codigoboleto}", 70f, 250f, paint)

            // === CÓDIGO DE BARRAS ===
            dibujarCodigoBarras(canvas, 70f, 260f, 200f, 30f)

            // === FECHA Y HORA ===
            paint.textSize = 24f
            paint.isFakeBoldText = true
            canvas.drawText("15 jul 17", 380f, 280f, paint)

            paint.textSize = 10f
            paint.isFakeBoldText = false
            canvas.drawText("Hora estimada/Boarding time", 380f, 295f, paint)
            canvas.drawText("13:05", 420f, 310f, paint)

            // === INFORMACIÓN ADICIONAL ===
            paint.textSize = 8f
            paint.color = Color.GRAY

            var yPos = 340f
            val lineHeight = 10f

            canvas.drawText("Recuerde que el artículo personal permitido sin costo por Xpectrum es una única pieza", 70f, yPos, paint)
            yPos += lineHeight
            canvas.drawText("de máximo 6 kg y 40x30x25 cm. Exceder las medidas y peso permitido tendrá un costo adicional.", 70f, yPos, paint)
            yPos += lineHeight * 1.5f

            canvas.drawText("Acérquese al counter, para reclamar el pase de abordar y entregar el equipaje, está disponible", 70f, yPos, paint)
            yPos += lineHeight
            canvas.drawText("entre 2 horas y 45 minutos antes de la salida programada para vuelos nacionales.", 70f, yPos, paint)
            yPos += lineHeight * 1.5f

            canvas.drawText("El equipaje de cabina, y en general cualquier pieza, que exceda los 55x40x25 cm y 12 kg,", 70f, yPos, paint)
            yPos += lineHeight
            canvas.drawText("deberá ser entregado en el counter de Xpectrum antes de ingresar a la espera.", 70f, yPos, paint)

            // === HASHTAG ===
            paint.textSize = 12f
            paint.color = Color.BLACK
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText("#YoSoyXpectrum", 297f, 430f, paint)
            paint.textAlign = Paint.Align.LEFT

            // === LÍNEA PUNTEADA DE SEPARACIÓN ===
            dibujarLineaPunteada(canvas, 70f, 450f, 525f, 450f)

            // === SECCIÓN INFERIOR DESPRENDIBLE ===
            paint.color = Color.BLACK
            paint.textSize = 12f
            paint.isFakeBoldText = true
            canvas.drawText("PASAJERO/PASSENGER NAME", 70f, 475f, paint)
            canvas.drawText("Código: ${boleto.codigoboleto}", 300f, 475f, paint)

            // Código de barras inferior
            dibujarCodigoBarras(canvas, 350f, 480f, 150f, 20f)

            // Información del vuelo inferior - HEADERS
            paint.textSize = 8f
            paint.isFakeBoldText = false
            paint.color = Color.GRAY
            canvas.drawText("Fecha/Date", 70f, 515f, paint)
            canvas.drawText("Vuelo/Flight", 130f, 515f, paint)
            canvas.drawText("From/To", 190f, 515f, paint)
            canvas.drawText("Seq. No.", 250f, 515f, paint)
            canvas.drawText("Grupo/Group", 310f, 515f, paint)
            canvas.drawText("Seat", 370f, 515f, paint)

            // Información del vuelo inferior - VALORES
            paint.textSize = 10f
            paint.isFakeBoldText = true
            paint.color = Color.BLACK
            canvas.drawText("15 jul. 17", 70f, 530f, paint)
            canvas.drawText("XP ${boleto.boletoid}", 130f, 530f, paint)
            canvas.drawText("LIM/AQP", 190f, 530f, paint)
            canvas.drawText("13", 250f, 530f, paint)
            canvas.drawText("4", 310f, 530f, paint)
            canvas.drawText("9B", 370f, 530f, paint)
        }
    }

    private fun dibujarCodigoBarras(canvas: Canvas, x: Float, y: Float, width: Float, height: Float) {
        val paint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.FILL
        }

        val barWidth = width / 50
        for (i in 0 until 50) {
            if (i % 2 == 0) {
                canvas.drawRect(x + i * barWidth, y, x + (i + 1) * barWidth, y + height, paint)
            }
        }
    }

    private fun dibujarLineaPunteada(canvas: Canvas, startX: Float, startY: Float, endX: Float, endY: Float) {
        val paint = Paint().apply {
            color = Color.BLACK
            strokeWidth = 1f
        }

        val dashLength = 5f
        val gapLength = 3f
        var currentX = startX

        while (currentX < endX) {
            canvas.drawLine(currentX, startY, Math.min(currentX + dashLength, endX), endY, paint)
            currentX += dashLength + gapLength
        }
    }

    private fun obtenerFechaActual(): String {
        val formato = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return formato.format(Date())
    }

    private fun guardarPDFEnDescargas(pdfDocument: PdfDocument, nombreArchivo: String): Uri? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ - Usar MediaStore
                val contentValues = ContentValues().apply {
                    put(MediaStore.Files.FileColumns.DISPLAY_NAME, nombreArchivo)
                    put(MediaStore.Files.FileColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.Files.FileColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }

                val uri = contentResolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
                uri?.let {
                    contentResolver.openOutputStream(it)?.use { outputStream ->
                        pdfDocument.writeTo(outputStream)
                    }
                }
                uri
            } else {
                // Android 9 y anteriores - Usar directorio público
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val archivo = File(downloadsDir, nombreArchivo)

                FileOutputStream(archivo).use { outputStream ->
                    pdfDocument.writeTo(outputStream)
                }

                Uri.fromFile(archivo)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "✅ Permiso concedido", Toast.LENGTH_SHORT).show()
                generarYGuardarPDF()
            } else {
                Toast.makeText(this, "⚠️ Se necesita permiso de almacenamiento", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}