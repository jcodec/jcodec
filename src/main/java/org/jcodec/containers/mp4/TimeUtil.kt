package org.jcodec.containers.mp4

import java.util.*

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
object TimeUtil {
    var MOV_TIME_OFFSET: Long = 0
    fun macTimeToDate(movSec: Int): Date {
        return Date(fromMovTime(movSec))
    }

    fun fromMovTime(movSec: Int): Long {
        return movSec.toLong() * 1000L + MOV_TIME_OFFSET
    }

    fun toMovTime(millis: Long): Int {
        return ((millis - MOV_TIME_OFFSET) / 1000L).toInt()
    }

    init {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"))
        calendar.set(1904, 0, 1, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        MOV_TIME_OFFSET = calendar.timeInMillis
    }
}