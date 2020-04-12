package org.jcodec.common.tools

import org.jcodec.common.and
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

/**
 * This class is part of JCodec ( www.jcodec.org )
 * This software is distributed under FreeBSD License
 *
 * @author The JCodec project
 */
object MD5 {
    @JvmStatic
    fun md5sumBytes(bytes: ByteArray?): String {
        val md5 = digest
        md5.update(bytes)
        return digestToString(md5.digest())
    }

    private fun digestToString(digest: ByteArray): String {
        val sb = StringBuilder()
        for (i in digest.indices) {
            val item = digest[i]
            val b: Int = item and 0xFF
            if (b < 0x10) sb.append("0")
            sb.append(Integer.toHexString(b))
        }
        return sb.toString()
    }

    @JvmStatic
    fun md5sum(bytes: ByteBuffer?): String {
        val md5 = digest
        md5.update(bytes)
        val digest = md5.digest()
        return digestToString(digest)
    }

    val digest: MessageDigest
        get() {
            val md5: MessageDigest
            md5 = try {
                MessageDigest.getInstance("MD5")
            } catch (e: NoSuchAlgorithmException) {
                throw RuntimeException(e)
            }
            return md5
        }
}