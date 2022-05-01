package com.chazlakinger.typescontrol

import android.Manifest
import android.app.Activity
import android.bluetooth.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


private const val ENABLE_BLUETOOTH_REQUEST_CODE = 1
private const val LOCATION_PERMISSION_REQUEST_CODE = 2
private val serviceUuid = UUID.fromString("8d96a001-0106-64c2-0001-9acc4838521c")
private val characteristicUuid = UUID.fromString("8d96b001-0106-64c2-0001-9acc4838521c")
private val macs = arrayListOf(Pair("Front", "E0:50:91:D0:DC:4A"), Pair("Sides", "C6:D3:3C:D1:02:DE"), Pair("Rear", "EA:6F:48:49:98:D6"))

class MainActivity : AppCompatActivity() {

    private lateinit var bleConnectButton: Button
    private lateinit var bleDisconnectButton: Button
    private lateinit var bleTurnOnButton: Button
    private lateinit var bleTurnOffButton: Button
    private lateinit var devicesView: RecyclerView
    private lateinit var devicesForDisplay: ArrayList<DeviceStatus>
    private var gatts: HashMap<String, BluetoothGatt> = HashMap()

    private val offByteArray: ByteArray = hexStringToByteArray("9e5bd20d07ce3a42ee63e508d60a57c600000000")
    private val onByteArray: ByteArray = hexStringToByteArray("466b8bb0c60bdf57ee63e508d60a57c600000019")
    private fun hexStringToByteArray(s: String): ByteArray {
        val len = s.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(s[i], 16) shl 4)
                    + Character.digit(s[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }

    private val bluetoothAdapter: BluetoothAdapter by lazy {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
                setContentView(R.layout.activity_main)

        generateDevicesForDisplay()
        bleConnectButton = findViewById(R.id.ble_connect)
        bleDisconnectButton = findViewById(R.id.ble_disconnect)
        bleTurnOnButton = findViewById(R.id.ble_turn_on)
        bleTurnOffButton = findViewById(R.id.ble_turn_off)
        devicesView = findViewById(R.id.device_adapter)
        devicesView.layoutManager = LinearLayoutManager(this)
        devicesView.adapter = DeviceAdapter(devicesForDisplay, this)
        bleConnectButton.setOnClickListener {
            connect()
        }
        bleDisconnectButton.setOnClickListener {
            disconnect()
        }
        bleTurnOnButton.setOnClickListener {
            turnOn()
        }
        bleTurnOffButton.setOnClickListener {
            turnOff()
        }
    }

    override fun onResume() {
        super.onResume()
        if (!bluetoothAdapter.isEnabled) {
            promptEnableBluetooth()
        }
    }

    private fun promptEnableBluetooth() {
        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, ENABLE_BLUETOOTH_REQUEST_CODE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            ENABLE_BLUETOOTH_REQUEST_CODE -> {
                if (resultCode != Activity.RESULT_OK) {
                    promptEnableBluetooth()
                }
            }
        }
    }

    val isLocationPermissionGranted
        get() = hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    fun Context.hasPermission(permissionType: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permissionType) ==
                PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        if (isLocationPermissionGranted) {
            return
        }
        runOnUiThread {
            requestPermission(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun Activity.requestPermission(permission: String, requestCode: Int) {
        ActivityCompat.requestPermissions(this, arrayOf(permission), requestCode)
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.firstOrNull() == PackageManager.PERMISSION_DENIED) {
                    requestLocationPermission()
                }
            }
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val deviceAddress = gatt.device.address

            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.w("BluetoothGattCallback", "Successfully connected to $deviceAddress")
                    gatt.discoverServices()
                    runOnUiThread { setDeviceConnected(gatt.device.address, true) }
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.w("BluetoothGattCallback", "Successfully disconnected from $deviceAddress")
                    gatt.close()
                }
            } else {
                Log.w("BluetoothGattCallback", "Error $status encountered for $deviceAddress! Disconnecting...")
                gatt.close()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            with(gatt) {
                gatts[gatt.device.address] = gatt
                Log.w("BluetoothGattCallback", "Discovered ${services.size} services for ${device.address}")
                runOnUiThread {
                    bleTurnOnButton.isEnabled = true
                    bleTurnOffButton.isEnabled = true
                }
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            with(characteristic) {
                when (status) {
                    BluetoothGatt.GATT_SUCCESS -> {
                        runOnUiThread { setDeviceOn(gatt.device.address,
                            characteristic.value.contentEquals(onByteArray)
                        ) }
                        Log.i("BluetoothGattCallback", "Wrote to characteristic $uuid | value: $value")
                    }
                    BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH -> {
                        Log.e("BluetoothGattCallback", "Write exceeded connection ATT MTU!")
                    }
                    BluetoothGatt.GATT_WRITE_NOT_PERMITTED -> {
                        Log.e("BluetoothGattCallback", "Write not permitted for $uuid!")
                    }
                    else -> {
                        Log.e("BluetoothGattCallback", "Characteristic write failed for $uuid, error: $status")
                    }
                }
            }
        }
    }

    fun BluetoothGattCharacteristic.isReadable(): Boolean =
        containsProperty(BluetoothGattCharacteristic.PROPERTY_READ)

    fun BluetoothGattCharacteristic.isWritable(): Boolean =
    containsProperty(BluetoothGattCharacteristic.PROPERTY_WRITE)

    fun BluetoothGattCharacteristic.isWritableWithoutResponse(): Boolean =
        containsProperty(BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)

    fun BluetoothGattCharacteristic.containsProperty(property: Int): Boolean {
        return properties and property != 0
    }

    fun writeCharacteristic(characteristic: BluetoothGattCharacteristic, payload: ByteArray, bluetoothGatt: BluetoothGatt) {
        val writeType = when {
            characteristic.isWritable() -> BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            characteristic.isWritableWithoutResponse() -> {
                BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            }
            else -> error("Characteristic ${characteristic.uuid} cannot be written to")
        }

        bluetoothGatt?.let { gatt ->
            characteristic.writeType = writeType
            characteristic.value = payload
            gatt.writeCharacteristic(characteristic)
        } ?: error("Not connected to a BLE device!")
    }

    private fun turnOn() {
        for (device in macs) {
            gatts[device.second]?.let { gatt ->
                val characteristic =
                    gatt.getService(serviceUuid).getCharacteristic(characteristicUuid)
                writeCharacteristic(characteristic, onByteArray, gatt)
            }
        }
    }

    private fun turnOff() {
        for (device in macs) {
            gatts[device.second]?.let { gatt ->
                val characteristic =
                    gatt.getService(serviceUuid).getCharacteristic(characteristicUuid)
                writeCharacteristic(characteristic, offByteArray, gatt)
            }
        }
    }

    private fun connect() {
        for (mac in macs) {
            val device = bluetoothAdapter.getRemoteDevice(mac.second)
            device.connectGatt(this, false, gattCallback)
        }
    }

    private fun disconnect() {
        for (gatt in gatts) {
            gatt.value.disconnect()
            setDeviceConnected(gatt.key, false)
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

    private fun generateDevicesForDisplay() {
        devicesForDisplay = ArrayList()
        for (entry in macs) {
            val device = DeviceStatus(entry.second, false, false)
            devicesForDisplay.add(device)
        }
    }
}

