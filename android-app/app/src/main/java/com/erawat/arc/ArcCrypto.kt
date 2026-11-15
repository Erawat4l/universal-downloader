package com.erawat.arc

import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import java.util.Arrays

/**
 * Native hardware-accelerated AES crypto implementation for Telegram MTProto.
 * Replaces pure-Python pyaes to accelerate Telegram file downloads by up to 1000x.
 */
object ArcCrypto {
    @JvmStatic
    fun ige256Decrypt(data: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/ECB/NoPadding")
        val secretKey = SecretKeySpec(key, "AES")
        cipher.init(Cipher.DECRYPT_MODE, secretKey)

        var iv1 = Arrays.copyOfRange(iv, 0, 16)
        var iv2 = Arrays.copyOfRange(iv, 16, 32)
        val out = ByteArray(data.size)
        val tempBlock = ByteArray(16)

        var offset = 0
        while (offset < data.size) {
            val chunkLen = Math.min(16, data.size - offset)
            if (chunkLen < 16) {
                System.arraycopy(data, offset, out, offset, chunkLen)
                break
            }
            for (j in 0 until 16) {
                tempBlock[j] = (data[offset + j].toInt() xor iv2[j].toInt()).toByte()
            }
            val dec = cipher.update(tempBlock) ?: cipher.doFinal(tempBlock)
            for (j in 0 until 16) {
                out[offset + j] = (dec[j].toInt() xor iv1[j].toInt()).toByte()
            }
            System.arraycopy(data, offset, iv1, 0, 16)
            System.arraycopy(out, offset, iv2, 0, 16)
            offset += 16
        }
        return out
    }

    @JvmStatic
    fun ige256Encrypt(data: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/ECB/NoPadding")
        val secretKey = SecretKeySpec(key, "AES")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)

        var iv1 = Arrays.copyOfRange(iv, 0, 16)
        var iv2 = Arrays.copyOfRange(iv, 16, 32)
        val out = ByteArray(data.size)
        val tempBlock = ByteArray(16)

        var offset = 0
        while (offset < data.size) {
            val chunkLen = Math.min(16, data.size - offset)
            if (chunkLen < 16) {
                System.arraycopy(data, offset, out, offset, chunkLen)
                break
            }
            for (j in 0 until 16) {
                tempBlock[j] = (data[offset + j].toInt() xor iv1[j].toInt()).toByte()
            }
            val enc = cipher.update(tempBlock) ?: cipher.doFinal(tempBlock)
            for (j in 0 until 16) {
                out[offset + j] = (enc[j].toInt() xor iv2[j].toInt()).toByte()
            }
            System.arraycopy(out, offset, iv1, 0, 16)
            System.arraycopy(data, offset, iv2, 0, 16)
            offset += 16
        }
        return out
    }

    @JvmStatic
    fun ctr256(data: ByteArray, key: ByteArray, iv: ByteArray, state: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/ECB/NoPadding")
        val secretKey = SecretKeySpec(key, "AES")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)

        val out = Arrays.copyOf(data, data.size)
        val curIv = Arrays.copyOf(iv, 16)
        var chunk = cipher.doFinal(curIv)
        var stateIdx = (state.getOrNull(0)?.toInt() ?: 0) and 0xFF

        for (i in data.indices) {
            out[i] = (out[i].toInt() xor chunk[stateIdx].toInt()).toByte()
            stateIdx++
            if (stateIdx >= 16) {
                stateIdx = 0
                for (k in 15 downTo 0) {
                    val v = (curIv[k].toInt() and 0xFF) + 1
                    curIv[k] = v.toByte()
                    if (v <= 255) break
                    curIv[k] = 0
                }
                chunk = cipher.doFinal(curIv)
            }
        }
        if (state.isNotEmpty()) {
            state[0] = stateIdx.toByte()
        }
        System.arraycopy(curIv, 0, iv, 0, 16)
        return out
    }
}
