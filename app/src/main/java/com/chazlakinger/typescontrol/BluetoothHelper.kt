package com.chazlakinger.typescontrol

import android.bluetooth.*
import android.content.Context
import android.util.Log
import java.util.*
import kotlin.collections.HashMap


class BluetoothHelper (private val context: Context) {
    val macs = arrayListOf(Pair("Front", "E0:50:91:D0:DC:4A"), Pair("Sides", "C6:D3:3C:D1:02:DE"), Pair("Rear", "EA:6F:48:49:98:D6"))
    private val serviceUuid = UUID.fromString("8d96a001-0106-64c2-0001-9acc4838521c")
    private val characteristicUuid = UUID.fromString("8d96b001-0106-64c2-0001-9acc4838521c")
    private val bluetoothEventListenerListener = context as? BluetoothEventListener

    interface BluetoothEventListener {
        fun deviceConnectionChange(device: DeviceStatus)
        fun deviceStateChanged(device: DeviceStatus)
    }

    private val bluetoothAdapter: BluetoothAdapter by lazy {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    private var turnOnAfterConnect = false
    private var turnOffAfterConnect = false
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

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val deviceAddress = gatt.device.address
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.w("BluetoothGattCallback", "Successfully connected to $deviceAddress")
                    gatt.discoverServices()
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    bluetoothEventListenerListener?.deviceConnectionChange(DeviceStatus(deviceAddress, false, null))
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
                bluetoothEventListenerListener?.deviceConnectionChange(DeviceStatus(gatt.device.address, true, null))
                if (turnOnAfterConnect) {
                    turnOn(gatt.device.address)
                } else if (turnOffAfterConnect) {
                    turnOff(gatt.device.address)
                }
                Log.w("BluetoothGattCallback", "Discovered ${services.size} services for ${device.address}")
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
                        characteristic.value?.let { sentValue ->
                            bluetoothEventListenerListener?.deviceStateChanged(DeviceStatus(
                                    gatt.device.address,
                                    null,
                                    sentValue.contentEquals(onByteArray)
                                )
                            )
                        }
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

    fun connect() {
        for (mac in macs) {
            val device = bluetoothAdapter.getRemoteDevice(mac.second)
            device.connectGatt(context, false, gattCallback)
        }
    }

    fun disconnect() {
        for (gatt in gatts) {
            gatt.value.disconnect()
        }
    }

    fun turnOn() {
        for (device in macs) {
            gatts[device.second]?.let { gatt ->
                val characteristic =
                    gatt.getService(serviceUuid).getCharacteristic(characteristicUuid)
                writeCharacteristic(characteristic, onByteArray, gatt)
            }
        }
    }

    fun turnOn(mac: String) {
        gatts[mac]?.let { gatt ->
            val characteristic =
                gatt.getService(serviceUuid).getCharacteristic(characteristicUuid)
            writeCharacteristic(characteristic, onByteArray, gatt)
        }
    }

    fun turnOff() {
        for (device in macs) {
            gatts[device.second]?.let { gatt ->
                val characteristic =
                    gatt.getService(serviceUuid).getCharacteristic(characteristicUuid)
                writeCharacteristic(characteristic, offByteArray, gatt)
            }
        }
    }

    fun turnOff(mac: String) {
        gatts[mac]?.let { gatt ->
            val characteristic =
                gatt.getService(serviceUuid).getCharacteristic(characteristicUuid)
            writeCharacteristic(characteristic, offByteArray, gatt)
        }
    }

    fun connectAndTurnOn() {
        turnOnAfterConnect = true
        turnOffAfterConnect = false
        connect()
    }

    fun connectAndTurnOff() {
        turnOnAfterConnect = false
        turnOffAfterConnect = true
        connect()
    }

}