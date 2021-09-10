package org.jcodec.codecs.vpx.vp8.data;

import org.jcodec.codecs.vpx.vp8.enums.FrameType;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License.
 * 
 * The class is a direct java port of libvpx's
 * (https://github.com/webmproject/libvpx) relevant VP8 code with significant
 * java oriented refactoring.
 * 
 * @author The JCodec project
 * 
 */
public class Header {
    public static final int VP8_HEADER_SIZE = 3;

    public FrameType type;
    public byte version;
    public boolean show_frame;
    public int first_partition_length_in_bytes;

    public short[] asThreeBytes() {
        /*
         * Pack partition size, show frame, version and frame type into to 24 bits.
         * Store it 8 bits at a time. https://tools.ietf.org/html/rfc6386 9.1.
         * Uncompressed Data Chunk The uncompressed data chunk comprises a common (for
         * key frames and interframes) 3-byte frame tag that contains four fields, as
         * follows:
         *
         * 1. A 1-bit frame type (0 for key frames, 1 for interframes).
         *
         * 2. A 3-bit version number (0 - 3 are defined as four different profiles with
         * different decoding complexity; other values may be defined for future
         * variants of the VP8 data format).
         *
         * 3. A 1-bit show_frame flag (0 when current frame is not for display, 1 when
         * current frame is for display).
         *
         * 4. A 19-bit field containing the size of the first data partition in bytes
         */

        int header = (first_partition_length_in_bytes << 5) | ((show_frame ? 1 : 0) << 4) | (version << 1)
                | (type == FrameType.KEY_FRAME ? 0 : 1);
        return new short[] { (short)(header & 0xff), (short)((header >> 8) & 0xff), (short)((header >> 16) & 0xff) };
    }
}
