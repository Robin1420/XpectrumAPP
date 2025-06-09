package com.example.xpectrumapp

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class WelcomeActivity : AppCompatActivity() {

    private lateinit var btnContinuar: Button
    private lateinit var tvBienvenida: TextView
    private lateinit var tvDescripcion: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Configurar la ventana para mostrar la barra de navegación y notificaciones
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        )
        
        // Asegurar que el contenido se dibuje detrás de las barras del sistema
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
        
        setContentView(R.layout.activity_welcome)

        // Inicializar vistas
        initViews()

        // Configurar eventos
        setupClickListeners()

        // Animación de bienvenida
        animateWelcome()
    }

    private fun initViews() {
        btnContinuar = findViewById(R.id.btnContinuar)
        tvBienvenida = findViewById(R.id.tvBienvenida)
        tvDescripcion = findViewById(R.id.tvDescripcion)
    }

    private fun setupClickListeners() {
        btnContinuar.setOnClickListener {
            // Navegar a la pantalla de vuelos
            val intent = Intent(this, VuelosActivity::class.java)
            startActivity(intent)
        }
    }

    private fun animateWelcome() {
        // Animación simple de fade in
        tvBienvenida.alpha = 0f
        tvDescripcion.alpha = 0f
        btnContinuar.alpha = 0f

        tvBienvenida.animate().alpha(1f).setDuration(1000).start()

        Handler(Looper.getMainLooper()).postDelayed({
            tvDescripcion.animate().alpha(1f).setDuration(800).start()
        }, 500)

        Handler(Looper.getMainLooper()).postDelayed({
            btnContinuar.animate().alpha(1f).setDuration(600).start()
        }, 1000)
    }
}