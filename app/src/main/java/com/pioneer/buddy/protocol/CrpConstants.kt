package com.pioneer.buddy.protocol

import java.util.UUID

object CrpConstants {
    const val SERVICE_NAME = "CarRemoteProtocol"
    val UUID_SPP: UUID = UUID.fromString("87301616-C0F8-48BB-AA38-5D79FAF3DE92")

    const val DLE: Byte = 0x9F.toByte()
    const val STX: Byte = 0x02.toByte()
    const val ETX: Byte = 0x03.toByte()

    // Packet IDs
    const val ID_AUTH: Byte = 0x00
    const val ID_DEVICE_CONTROL: Byte = 0x20.toByte() // Outgoing commands (Volume, Seek, Preset)
    const val ID_INCOMING_NOTIFICATION: Byte = 0x21.toByte() // Hardware keys, Media keys, Rotary
    const val ID_AUDIO_INFO: Byte = 0x36.toByte() // Outgoing Track Title/Artist to SPH-10BT screen
}

object PacketUtil {
    fun ubyteToInt(b: Byte): Int = b.toInt() and 0xFF

    fun createChecksum(id: Int, type: Int, data: ByteArray): Int {
        var checksum = id xor type
        for (b in data) {
            checksum = checksum xor ubyteToInt(b)
        }
        return checksum and 0xFF
    }

    fun escape(data: ByteArray): ByteArray {
        val out = ArrayList<Byte>(data.size + 16)
        for (b in data) {
            if (b == CrpConstants.DLE) {
                out.add(CrpConstants.DLE)
            }
            out.add(b)
        }
        return out.toByteArray()
    }

    fun buildFrame(id: Int, type: Int, data: ByteArray): ByteArray {
        val body = ArrayList<Byte>(data.size + 3)
        body.add(id.toByte())
        body.add(type.toByte())
        for (b in data) {
            body.add(b)
        }
        val checksum = createChecksum(id, type, data)
        body.add(checksum.toByte())

        val escapedBody = escape(body.toByteArray())

        val frame = ArrayList<Byte>(escapedBody.size + 4)
        frame.add(CrpConstants.DLE)
        frame.add(CrpConstants.STX)
        for (b in escapedBody) {
            frame.add(b)
        }
        frame.add(CrpConstants.DLE)
        frame.add(CrpConstants.ETX)
        return frame.toByteArray()
    }
}
