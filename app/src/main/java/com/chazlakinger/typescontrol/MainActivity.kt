package com.chazlakinger.typescontrol

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView


class MainActivity : AppCompatActivity(), BluetoothHelper.BluetoothEventListener {

    private lateinit var bleConnectButton: Button
    private lateinit var bleDisconnectButton: Button
    private lateinit var bleTurnOnButton: Button
    private lateinit var bleTurnOffButton: Button
    private lateinit var bleConnectAndTurnOn: Button
    private lateinit var bleConnectAndTurnOff: Button
    private lateinit var devicesView: RecyclerView
    private lateinit var devicesForDisplay: ArrayList<DeviceStatus>

    private val bluetoothHelper = BluetoothHelper(this, this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
                setContentView(R.layout.activity_main)

        generateDevicesForDisplay()
        bleConnectButton = findViewById(R.id.ble_connect)
        bleDisconnectButton = findViewById(R.id.ble_disconnect)
        bleTurnOnButton = findViewById(R.id.ble_turn_on)
        bleTurnOffButton = findViewById(R.id.ble_turn_off)
        bleConnectAndTurnOn = findViewById(R.id.ble_connect_and_turn_on)
        bleConnectAndTurnOff = findViewById(R.id.ble_connect_and_turn_off)
        devicesView = findViewById(R.id.device_adapter)
        devicesView.layoutManager = LinearLayoutManager(this)
        devicesView.adapter = DeviceAdapter(devicesForDisplay, this)
        bleConnectButton.setOnClickListener {
            bluetoothHelper.connect()
        }
        bleDisconnectButton.setOnClickListener {
            bluetoothHelper.disconnect()
        }
        bleTurnOnButton.setOnClickListener {
            bluetoothHelper.turnOn()
        }
        bleTurnOffButton.setOnClickListener {
            bluetoothHelper.turnOff()
        }
        bleConnectAndTurnOn.setOnClickListener {
            bluetoothHelper.connectAndTurnOn()
        }
        bleConnectAndTurnOff.setOnClickListener {
            bluetoothHelper.connectAndTurnOff()
        }
    }

    private fun setDeviceConnected(mac: String, connected: Boolean) {
        devicesForDisplay.forEachIndexed { index, element ->
            if (element.mac == mac) {
                val newDevice = DeviceStatus(element.mac, connected, element.on)
                devicesForDisplay[index] = newDevice
                devicesView.adapter?.notifyDataSetChanged()
            }
        }
    }

    private fun setDeviceOn(mac: String, on: Boolean) {
        devicesForDisplay.forEachIndexed { index, element ->
            if (element.mac == mac) {
                val newDevice = DeviceStatus(element.mac, element.connected, on)
                devicesForDisplay[index] = newDevice
                devicesView.adapter?.notifyDataSetChanged()
            }
        }
    }

    override fun deviceConnectionChange(device: DeviceStatus) {
        runOnUiThread {
            if (device.connected == true) {
                bleTurnOnButton.isEnabled = true
                bleTurnOffButton.isEnabled = true
            }
            setDeviceConnected(device.mac, device.connected ?: false)
        }

    }

    override fun deviceStateChanged(device: DeviceStatus) {
        runOnUiThread {
            setDeviceOn(device.mac, device.on ?: false)
        }

    }

    private fun generateDevicesForDisplay() {
        devicesForDisplay = ArrayList()
        for (entry in bluetoothHelper.macs) {
            val device = DeviceStatus(entry.second, false, false)
            devicesForDisplay.add(device)
        }
    }
}

