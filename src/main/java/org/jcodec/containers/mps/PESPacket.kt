package org.jcodec.containers.mps

import java.nio.ByteBuffer

class PESPacket(@JvmField var data: ByteBuffer,
                @JvmField var pts: Long,
                @JvmField var streamId: Int,
                @JvmField var length: Int,
                @JvmField var pos: Long,
                @JvmField var dts: Long)