package org.jcodec.codecs.aac

import org.jcodec.common.io.BitReader.Companion.createBitReader
import org.jcodec.common.io.BitWriter
import java.nio.ByteBuffer

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
object ADTSParser {
    @JvmStatic
    fun adtsToStreamInfo(hdr: Header): ByteBuffer {
        val si = ByteBuffer.allocate(2)
        val wr = BitWriter(si)
        wr.writeNBit(hdr.objectType, 5)
        wr.writeNBit(hdr.samplingIndex, 4)
        wr.writeNBit(hdr.chanConfig, 4)
        wr.flush()
        si.clear()
        return si
    }

    @JvmStatic
    fun read(data: ByteBuffer): Header? {
        val dup = data.duplicate()
        val br = createBitReader(dup)
        // int size, rdb, ch, sr;
        // int aot, crc_abs;
        if (br.readNBit(12) != 0xfff) {
            return null
        }
        val id = br.read1Bit() /* id */
        val layer = br.readNBit(2) /* layer */
        val crc_abs = br.read1Bit() /* protection_absent */
        val aot = br.readNBit(2) /* profile_objecttype */
        val sr = br.readNBit(4) /* sample_frequency_index */
        val pb = br.read1Bit() /* private_bit */
        val ch = br.readNBit(3) /* channel_configuration */
        val origCopy = br.read1Bit() /* original/copy */
        val home = br.read1Bit() /* home */

        /* adts_variable_header */
        val copy = br.read1Bit() /* copyright_identification_bit */
        val copyStart = br.read1Bit() /* copyright_identification_start */
        val size = br.readNBit(13) /* aac_frame_length */
        if (size < 7) return null
        val buffer = br.readNBit(11) /* adts_buffer_fullness */
        val rdb = br.readNBit(2) /* number_of_raw_data_blocks_in_frame */
        br.stop()
        data.position(dup.position())
        return Header(aot + 1, ch, crc_abs, rdb + 1, sr, size)
    }

    fun write(header: Header, buf: ByteBuffer): ByteBuffer {
        val data = buf.duplicate()
        val br = BitWriter(data)
        // int size, rdb, ch, sr;
        // int aot, crc_abs;
        br.writeNBit(0xfff, 12)
        br.write1Bit(1) /* id */
        br.writeNBit(0, 2) /* layer */
        br.write1Bit(header.crcAbsent) /* protection_absent */
        br.writeNBit(header.objectType - 1, 2) /* profile_objecttype */
        br.writeNBit(header.samplingIndex, 4) /* sample_frequency_index */
        br.write1Bit(0) /* private_bit */
        br.writeNBit(header.chanConfig, 3) /* channel_configuration */
        br.write1Bit(0) /* original/copy */
        br.write1Bit(0) /* home */

        /* adts_variable_header */br.write1Bit(0) /* copyright_identification_bit */
        br.write1Bit(0) /* copyright_identification_start */
        br.writeNBit(header.size, 13) /* aac_frame_length */
        br.writeNBit(0, 11) /* adts_buffer_fullness */
        br.writeNBit(header.numAACFrames - 1, 2) /* number_of_raw_data_blocks_in_frame */
        br.flush()
        data.flip()
        return data
    }

    class Header(val objectType: Int, val chanConfig: Int, val crcAbsent: Int, val numAACFrames: Int, val samplingIndex: Int, val size: Int) {
        val samples = 0

        val sampleRate: Int
            get() = AACConts.AAC_SAMPLE_RATES[samplingIndex]

    }
}