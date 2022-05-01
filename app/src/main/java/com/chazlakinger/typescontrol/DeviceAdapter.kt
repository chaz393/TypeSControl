package com.chazlakinger.typescontrol

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView


class DeviceAdapter(private val devices: ArrayList<DeviceStatus>, private val context: Context) : RecyclerView.Adapter<DeviceAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.device_item, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return devices.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.mac.text = devices[position].mac
        holder.connected.text = if (devices[position].connected)
            context.getString(R.string.device_connected) else
            context.getString(R.string.device_disconnected)
        holder.on.text = if (devices[position].on)
            context.getString(R.string.on) else
            context.getString(R.string.off)
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val mac: TextView
        val connected: TextView
        val on: TextView

        init {
            mac = view.findViewById(R.id.device_mac)
            connected = view.findViewById(R.id.device_connected)
            on = view.findViewById(R.id.device_on)
        }
    }
}