package org.jcodec.common.io

import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit

/**
 * This class is part of JCodec ( www.jcodec.org )
 * This software is distributed under FreeBSD License
 *
 * @author The JCodec project
 */
class AutoPool private constructor() {
    private val resources: MutableList<AutoResource>
    private val scheduler: ScheduledExecutorService
    private fun daemonThreadFactory(): ThreadFactory {
        return ThreadFactory { r ->
            val t = Thread(r)
            t.isDaemon = true
            t.name = AutoPool::class.java.name
            t
        }
    }

    fun add(res: AutoResource) {
        resources.add(res)
    }

    companion object {

        @JvmField
        val instance = AutoPool()
    }

    init {
        resources = Collections.synchronizedList(ArrayList())
        scheduler = Executors.newScheduledThreadPool(1, daemonThreadFactory())
        val res: List<AutoResource> = resources
        scheduler.scheduleAtFixedRate({
            val curTime = System.currentTimeMillis()
            for (autoResource in res) {
                autoResource.setCurTime(curTime)
            }
        }, 0, 100, TimeUnit.MILLISECONDS)
    }
}