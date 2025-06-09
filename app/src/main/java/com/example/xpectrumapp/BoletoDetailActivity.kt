package com.example.xpectrumapp

import android.Manifest
import android.content.ContentValues
import android.content.Intent
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
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class BoletoDetailActivity : AppCompatActivity() {

    // Views
    private lateinit var boletoContainer: View
    private lateinit var tvCodigoVuelo: TextView
    private lateinit var tvDestino: TextView
    private lateinit var tvCiudadDestino: TextView
    private lateinit var tvFechaSalida: TextView
    private lateinit var tvFechaLlegada: TextView
    private lateinit var tvTipoViaje: TextView
    private lateinit var tvClase: TextView
    private lateinit var tvPrecioUSD: TextView
    private lateinit var tvPrecioPEN: TextView
    private lateinit var tvAeronave: TextView
    private lateinit var tvEstadoVuelo: TextView
    private lateinit var btnDescargarPDF: Button
    private lateinit var btnVolver: Button
    
    // Datos del vuelo
    private var codigoVuelo: String = ""
    private var destino: String = ""
    private var ciudadDestino: String = ""
    private var fechaSalida: String = ""
    private var fechaLlegada: String = ""
    private var tipoViaje: String = ""
    private var clase: String = ""
    private var precioUSD: Double = 0.0
    private var precioPEN: Double = 0.0
    private var aeronave: String = ""
    private var estadoVuelo: String = ""

    companion object {
        private const val STORAGE_PERMISSION_REQUEST = 101
        private const val TAG = "BoletoDetailActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_boleto_detail)

        // Configurar ActionBar
        supportActionBar?.title = "Detalle del Vuelo"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        initViews()
        setupClickListeners()
        loadVueloData()
    }

    private fun initViews() {
        boletoContainer = findViewById(R.id.boletoContainer)
        tvCodigoVuelo = findViewById(R.id.tvCodigoVuelo)
        tvDestino = findViewById(R.id.tvDestino)
        tvCiudadDestino = findViewById(R.id.tvCiudadDestino)
        tvFechaSalida = findViewById(R.id.tvFechaSalida)
        tvFechaLlegada = findViewById(R.id.tvFechaLlegada)
        tvTipoViaje = findViewById(R.id.tvTipoViaje)
        tvClase = findViewById(R.id.tvClase)
        tvPrecioUSD = findViewById(R.id.tvPrecioUSD)
        tvPrecioPEN = findViewById(R.id.tvPrecioPEN)
        tvAeronave = findViewById(R.id.tvAeronave)
        tvEstadoVuelo = findViewById(R.id.tvEstadoVuelo)
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

    private fun loadVueloData() {
        // Obtener datos del intent
        codigoVuelo = intent.getStringExtra("VUELO_CODIGO") ?: ""
        destino = intent.getStringExtra("DESTINO") ?: ""
        ciudadDestino = intent.getStringExtra("CIUDAD_DESTINO") ?: ""
        fechaSalida = intent.getStringExtra("FECHA_SALIDA") ?: ""
        fechaLlegada = intent.getStringExtra("FECHA_LLEGADA") ?: ""
        tipoViaje = intent.getStringExtra("TIPO_VIAJE") ?: ""
        clase = intent.getStringExtra("CLASE") ?: ""
        precioUSD = intent.getDoubleExtra("PRECIO_USD", 0.0)
        precioPEN = intent.getDoubleExtra("PRECIO_PEN", 0.0)
        aeronave = intent.getStringExtra("AERONAVE") ?: ""
        estadoVuelo = intent.getStringExtra("ESTADO") ?: ""

        // Mostrar datos en la UI
        mostrarDatosVuelo()
    }

    private fun mostrarDatosVuelo() {
        tvCodigoVuelo.text = "Vuelo: $codigoVuelo"
        tvDestino.text = destino
        tvCiudadDestino.text = ciudadDestino
        tvFechaSalida.text = "Salida: $fechaSalida"
        tvFechaLlegada.text = "Llegada: $fechaLlegada"
        tvTipoViaje.text = "Tipo: ${tipoViaje.uppercase()}"
        tvClase.text = "Clase: ${clase.uppercase()}"
        tvPrecioUSD.text = "Precio USD: $${String.format("%.2f", precioUSD)}"
        tvPrecioPEN.text = "Precio PEN: S/${String.format("%.2f", precioPEN)}"
        tvAeronave.text = aeronave
        tvEstadoVuelo.text = estadoVuelo.uppercase()

        // Cambiar color según estado
        when (estadoVuelo.lowercase()) {
            "programado" -> {
                tvEstadoVuelo.setTextColor(ContextCompat.getColor(this, android.R.color.holo_blue_dark))
            }
            "en vuelo" -> {
                tvEstadoVuelo.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
            }
            "aterrizado" -> {
                tvEstadoVuelo.setTextColor(ContextCompat.getColor(this, android.R.color.holo_purple))
            }
            "cancelado" -> {
                tvEstadoVuelo.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
            }
            else -> {
                tvEstadoVuelo.setTextColor(ContextCompat.getColor(this, android.R.color.holo_orange_dark))
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

            dibujarBoardingPassVivaAir(canvas)

            pdfDocument.finishPage(page)

            val nombreArchivo = "BoardingPass_${codigoVuelo}_${System.currentTimeMillis()}.pdf"
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
        val paint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
        }

        // Fondo
        paint.color = Color.WHITE
        canvas.drawRect(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat(), paint)

        // Encabezado - Logo y título
        paint.color = Color.parseColor("#FF0000") // Rojo VivaAir
        paint.textSize = 24f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("Xpectrum Airlines", canvas.width / 2f, 50f, paint)
        paint.textSize = 18f
        canvas.drawText("BOARDING PASS", canvas.width / 2f, 80f, paint)

        // Información del vuelo
        paint.textAlign = Paint.Align.LEFT
        paint.color = Color.BLACK
        paint.textSize = 14f
        
        var yPos = 130f
        val lineHeight = 30f
        
        canvas.drawText("Vuelo: $codigoVuelo", 50f, yPos, paint)
        yPos += lineHeight
        canvas.drawText("Destino: $destino, $ciudadDestino", 50f, yPos, paint)
        yPos += lineHeight
        canvas.drawText("Salida: $fechaSalida", 50f, yPos, paint)
        yPos += lineHeight
        canvas.drawText("Llegada: $fechaLlegada", 50f, yPos, paint)
        yPos += lineHeight
        canvas.drawText("Tipo: ${tipoViaje.uppercase()}", 50f, yPos, paint)
        yPos += lineHeight
        canvas.drawText("Clase: ${clase.uppercase()}", 50f, yPos, paint)
        yPos += lineHeight
        canvas.drawText("Precio USD: $${String.format("%.2f", precioUSD)}", 50f, yPos, paint)
        yPos += lineHeight
        canvas.drawText("Precio PEN: S/${String.format("%.2f", precioPEN)}", 50f, yPos, paint)
        yPos += lineHeight
        canvas.drawText("Aeronave: $aeronave", 50f, yPos, paint)
        yPos += lineHeight
        
        // Estado
        paint.color = when (estadoVuelo.lowercase()) {
            "programado" -> Color.BLUE
            "en vuelo" -> Color.GREEN
            "aterrizado" -> Color.MAGENTA
            "cancelado" -> Color.RED
            else -> Color.DKGRAY
        }
        canvas.drawText("Estado: ${estadoVuelo.uppercase()}", 50f, yPos, paint)
        
        // Código de barras simulado
        yPos += lineHeight * 2
        paint.color = Color.BLACK
        paint.textSize = 12f
        canvas.drawText("Código de barras:", 50f, yPos, paint)
        yPos += 20f
        paint.textSize = 20f
        canvas.drawText("||||| |||| ||||| |||| |||||", 50f, yPos, paint)
        
        // Pie de página
        yPos = canvas.height - 50f
        paint.color = Color.GRAY
        paint.textSize = 10f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("Gracias por volar con Xpectrum Airlines", canvas.width / 2f, yPos, paint)
        yPos += 15f
        canvas.drawText("Para cualquier consulta, contacte a soporte@xpectrum.com", canvas.width / 2f, yPos, paint)
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