package org.jcodec.containers.mp4.demuxer

import java.nio.ByteBuffer

interface DemuxerProbe {
    fun probe(b: ByteBuffer): Int
}