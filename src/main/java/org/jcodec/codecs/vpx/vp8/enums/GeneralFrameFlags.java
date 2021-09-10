package org.jcodec.codecs.vpx.vp8.enums;

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
/*!\brief Compressed Frame Flags
*
* This type represents a bitfield containing information about a compressed
* frame that may be useful to an application*/
public enum GeneralFrameFlags {
    FRAME_IS_KEY /** < frame is the start of a GOP */,
    FRAME_IS_DROPPABLE /*
                            * !\brief frame can be dropped without affecting the stream (no future frame
                            * depends on this one)
                            */,
    FRAME_IS_INVISIBLE /* !\brief frame should be decoded but will not be shown */,
    FRAME_IS_FRAGMENT /* !\brief this is a fragment of the encoded frame */
}
