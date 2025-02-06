package com.movtery.zalithlauncher.utils.group

import com.movtery.zalithlauncher.InfoDistributor
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64

class QQGroup {
    companion object {
        fun hasKey(): Boolean = InfoDistributor.QQ_KEY_1 != "NULL" && InfoDistributor.QQ_KEY_2 != "NULL"

        fun generateQQJoinGroupCode(qqNumber: Long): String {
            val blockSize = 64
            val combinedKey = "${InfoDistributor.QQ_KEY_1}${InfoDistributor.QQ_KEY_2}".toByteArray(StandardCharsets.UTF_8)

            val processedKey = when {
                combinedKey.size > blockSize -> {
                    val md = MessageDigest.getInstance("SHA-256")
                    md.digest(combinedKey).copyOf(blockSize)
                }
                combinedKey.size < blockSize -> {
                    combinedKey.copyOf(blockSize)
                }
                else -> combinedKey
            }

            fun generateXorPad(padValue: Byte) = ByteArray(blockSize).apply {
                for (i in indices) {
                    this[i] = (padValue.toInt() xor processedKey[i].toInt()).toByte()
                }
            }

            val ipad = generateXorPad(0x36)
            val opad = generateXorPad(0x5C)

            val innerHash = MessageDigest.getInstance("SHA-256").apply {
                update(ipad)
                update(qqNumber.toString().toByteArray(StandardCharsets.UTF_8))
            }.digest()

            return Base64.getEncoder().encodeToString(
                MessageDigest.getInstance("SHA-256").apply {
                    update(opad)
                    update(innerHash)
                }.digest()
            )
        }
    }
}