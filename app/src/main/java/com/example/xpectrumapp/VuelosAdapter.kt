package com.example.xpectrumapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.xpectrumapp.logic.VueloResponse

class VuelosAdapter(
    private var vuelos: List<VueloResponse>,
    private val onVueloClick: (VueloResponse) -> Unit = {}
) : RecyclerView.Adapter<VuelosAdapter.VueloViewHolder>() {

    class VueloViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDestino: TextView = itemView.findViewById(R.id.tvDestino)
        val tvTipoViaje: TextView = itemView.findViewById(R.id.tvTipoViaje)
        val tvFechaViaje: TextView = itemView.findViewById(R.id.tvFechaViaje)
        val tvClase: TextView = itemView.findViewById(R.id.tvClase)
        val tvPrecioUsd: TextView = itemView.findViewById(R.id.tvPrecioUsd)
        val tvPrecioPen: TextView = itemView.findViewById(R.id.tvPrecioPen)
        val tvBeneficio: TextView = itemView.findViewById(R.id.tvBeneficio)
        val tvTasas: TextView = itemView.findViewById(R.id.tvTasas)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VueloViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_vuelo, parent, false)
        return VueloViewHolder(view)
    }

    override fun onBindViewHolder(holder: VueloViewHolder, position: Int) {
        val vuelo = vuelos[position]

        // Información del vuelo
        holder.tvDestino.text = vuelo.aeropuertoDestino
        holder.tvTipoViaje.text = "👤 ${vuelo.tipoViaje.trim().uppercase()}"
        holder.tvFechaViaje.text = "${vuelo.fechaSalida} ${vuelo.horaSalida}"

        // Clase del vuelo
        holder.tvClase.text = "${vuelo.clase}"

        // Precios
        holder.tvPrecioUsd.text = "$${String.format("%.2f", vuelo.precioUSD)}"
        holder.tvPrecioPen.text = "S/${String.format("%.2f", vuelo.precioPEN)}"

        // Beneficios
        val beneficioTexto = vuelo.beneficio?.takeIf { it.isNotBlank() } ?: "Sin beneficios"
        holder.tvBeneficio.text = "$beneficioTexto"

        // Información adicional
        holder.tvTasas.text = "${vuelo.aeronaveModelo} (${vuelo.aeronaveCapacidad} pasajeros)"

        // Click listener para navegar al escáner QR
        holder.itemView.setOnClickListener {
            onVueloClick(vuelo)
        }
    }

    override fun getItemCount(): Int = vuelos.size

    fun updateVuelos(nuevosVuelos: List<VueloResponse>) {
        vuelos = nuevosVuelos
        notifyDataSetChanged()
    }
}