package org.jcodec.containers.avi

import org.jcodec.common.io.NIOUtils
import org.junit.Test
import java.io.File

class AVIReaderTest {
    @Test
    //TODO: fix it
    fun read() {
        val reader = AVIReader(NIOUtils.readableChannel(File("src/test/resources/test.avi")))
        reader.parse()
    }
}