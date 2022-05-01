package com.chazlakinger.typescontrol

class DeviceStatus(val mac: String, var connected: Boolean, var on: Boolean) {

    override fun equals(other: Any?): Boolean {
        return when (other) {
            is DeviceStatus -> {
                this.mac == other.mac &&
                        this.connected == other.connected &&
                        this.on == other.on
            }
            else -> false
        }
    }

    override fun hashCode(): Int {
        var result = mac.hashCode()
        result = 31 * result + connected.hashCode()
        result = 31 * result + on.hashCode()
        return result
    }
}