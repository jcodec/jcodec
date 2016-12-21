package org.jcodec.codecs.aac;

import java.nio.ByteBuffer;

import org.jcodec.common.io.BitReader;
import org.jcodec.common.io.BitWriter;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class ADTSParser {

    public static class Header {
        private int objectType;
        private int chanConfig;
        private int crcAbsent;
        private int numAACFrames;
        private int samplingIndex;
        private int samples;
        private int size;

        public Header(int object_type, int chanConfig, int crcAbsent, int numAACFrames, int samplingIndex, int size) {
            this.objectType = object_type;
            this.chanConfig = chanConfig;
            this.crcAbsent = crcAbsent;
            this.numAACFrames = numAACFrames;
            this.samplingIndex = samplingIndex;
            this.size = size;
        }

        public int getObjectType() {
            return objectType;
        }

        public int getChanConfig() {
            return chanConfig;
        }

        public int getCrcAbsent() {
            return crcAbsent;
        }

        public int getNumAACFrames() {
            return numAACFrames;
        }

        public int getSamplingIndex() {
            return samplingIndex;
        }

        public int getSamples() {
            return samples;
        }

        public int getSize() {
            return size;
        }
    }

    public static Header read(ByteBuffer data) {
        ByteBuffer dup = data.duplicate();
        BitReader br = BitReader.createBitReader(dup);
        // int size, rdb, ch, sr;
        // int aot, crc_abs;

        if (br.readNBit(12) != 0xfff) {
            return null;
        }

        int id = br.read1Bit(); /* id */
        int layer = br.readNBit(2); /* layer */
        int crc_abs = br.read1Bit(); /* protection_absent */
        int aot = br.readNBit(2); /* profile_objecttype */
        int sr = br.readNBit(4); /* sample_frequency_index */
        int pb = br.read1Bit(); /* private_bit */
        int ch = br.readNBit(3); /* channel_configuration */

        int origCopy = br.read1Bit(); /* original/copy */
        int home = br.read1Bit(); /* home */

        /* adts_variable_header */
        int copy = br.read1Bit(); /* copyright_identification_bit */
        int copyStart = br.read1Bit(); /* copyright_identification_start */
        int size = br.readNBit(13); /* aac_frame_length */
        if (size < 7)
            return null;

        int buffer = br.readNBit(11); /* adts_buffer_fullness */
        int rdb = br.readNBit(2); /* number_of_raw_data_blocks_in_frame */
        br.stop();

        data.position(dup.position());

        return new Header(aot + 1, ch, crc_abs, rdb + 1, sr, size);
    }

    public static ByteBuffer write(Header header, ByteBuffer buf, int frameSize) {
        ByteBuffer data = buf.duplicate();
        BitWriter br = new BitWriter(data);
        // int size, rdb, ch, sr;
        // int aot, crc_abs;

        br.writeNBit(0xfff, 12);

        br.write1Bit(1); /* id */
        br.writeNBit(0, 2); /* layer */
        br.write1Bit(header.getCrcAbsent()); /* protection_absent */
        br.writeNBit(header.getObjectType(), 2); /* profile_objecttype */
        br.writeNBit(header.getSamplingIndex(), 4); /* sample_frequency_index */
        br.write1Bit(0); /* private_bit */
        br.writeNBit(header.getChanConfig(), 3); /* channel_configuration */

        br.write1Bit(0); /* original/copy */
        br.write1Bit(0); /* home */

        /* adts_variable_header */
        br.write1Bit(0); /* copyright_identification_bit */
        br.write1Bit(0); /* copyright_identification_start */
        br.writeNBit(frameSize + 7, 13); /* aac_frame_length */

        br.writeNBit(0, 11); /* adts_buffer_fullness */
        br.writeNBit(header.getNumAACFrames(), 2); /* number_of_raw_data_blocks_in_frame */
        br.flush();

        data.flip();
        return data;
    }
}