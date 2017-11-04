package org.jcodec.codecs.mpa;

import static org.jcodec.codecs.mpa.MpaConst.JOINT_STEREO;
import static org.jcodec.codecs.mpa.MpaConst.MPEG1;
import static org.jcodec.codecs.mpa.MpaConst.MPEG25_LSF;
import static org.jcodec.codecs.mpa.MpaConst.MPEG2_LSF;
import static org.jcodec.codecs.mpa.MpaConst.SAMPLE_FREQ_32K;
import static org.jcodec.codecs.mpa.MpaConst.SAMPLE_FREQ_48K;
import static org.jcodec.codecs.mpa.MpaConst.SINGLE_CHANNEL;
import static org.jcodec.codecs.mpa.MpaConst.bitrates;
import static org.jcodec.codecs.mpa.MpaConst.frequencies;

import java.nio.ByteBuffer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Header for MPEG Audio (MPEG 1 audio layer 2 and 3).
 * 
 * @author The JCodec project
 */
class MpaHeader {
    int layer;
    int protectionBit;
    int bitrateIndex;
    int paddingBit;
    int modeExtension;
    int version;
    int mode;
    int sampleFreq;
    int numSubbands;
    int intensityStereoBound;
    boolean copyright;
    boolean original;
    int framesize;
    int frameBytes;

    MpaHeader() {
    }

    static MpaHeader read_header(ByteBuffer bb) {
        MpaHeader ret = new MpaHeader();
        int headerstring;
        int channel_bitrate;
        headerstring = bb.getInt();
        ret.version = ((headerstring >>> 19) & 1);
        if (((headerstring >>> 20) & 1) == 0)
            if (ret.version == MPEG2_LSF)
                ret.version = MPEG25_LSF;
            else
                throw new RuntimeException("UNKNOWN_ERROR");
        if ((ret.sampleFreq = ((headerstring >>> 10) & 3)) == 3) {
            throw new RuntimeException("UNKNOWN_ERROR");
        }
        ret.layer = 4 - (headerstring >>> 17) & 3;
        ret.protectionBit = (headerstring >>> 16) & 1;
        ret.bitrateIndex = (headerstring >>> 12) & 0xF;
        ret.paddingBit = (headerstring >>> 9) & 1;
        ret.mode = ((headerstring >>> 6) & 3);
        ret.modeExtension = (headerstring >>> 4) & 3;
        if (ret.mode == JOINT_STEREO)
            ret.intensityStereoBound = (ret.modeExtension << 2) + 4;
        else
            ret.intensityStereoBound = 0;
        if (((headerstring >>> 3) & 1) == 1)
            ret.copyright = true;
        if (((headerstring >>> 2) & 1) == 1)
            ret.original = true;
        if (ret.layer == 1)
            ret.numSubbands = 32;
        else {
            channel_bitrate = ret.bitrateIndex;
            if (ret.mode != SINGLE_CHANNEL)
                if (channel_bitrate == 4)
                    channel_bitrate = 1;
                else
                    channel_bitrate -= 4;
            if ((channel_bitrate == 1) || (channel_bitrate == 2))
                if (ret.sampleFreq == SAMPLE_FREQ_32K)
                    ret.numSubbands = 12;
                else
                    ret.numSubbands = 8;
            else if ((ret.sampleFreq == SAMPLE_FREQ_48K) || ((channel_bitrate >= 3) && (channel_bitrate <= 5)))
                ret.numSubbands = 27;
            else
                ret.numSubbands = 30;
        }
        if (ret.intensityStereoBound > ret.numSubbands)
            ret.intensityStereoBound = ret.numSubbands;
        calculateFramesize(ret);

        return ret;
    }

    public static void calculateFramesize(MpaHeader ret) {
        if (ret.layer == 1) {
            ret.framesize = (12 * bitrates[ret.version][0][ret.bitrateIndex])
                    / frequencies[ret.version][ret.sampleFreq];
            if (ret.paddingBit != 0)
                ret.framesize++;
            ret.framesize <<= 2;
            ret.frameBytes = 0;
        } else {
            ret.framesize = (144 * bitrates[ret.version][ret.layer - 1][ret.bitrateIndex])
                    / frequencies[ret.version][ret.sampleFreq];
            if (ret.version == MPEG2_LSF || ret.version == MPEG25_LSF)
                ret.framesize >>= 1;
            if (ret.paddingBit != 0)
                ret.framesize++;
            if (ret.layer == 3) {
                if (ret.version == MPEG1) {
                    ret.frameBytes = ret.framesize - ((ret.mode == SINGLE_CHANNEL) ? 17 : 32)
                            - ((ret.protectionBit != 0) ? 0 : 2) - 4;
                } else {
                    ret.frameBytes = ret.framesize - ((ret.mode == SINGLE_CHANNEL) ? 9 : 17)
                            - ((ret.protectionBit != 0) ? 0 : 2) - 4;
                }
            } else {
                ret.frameBytes = 0;
            }
        }
        ret.framesize -= 4;
    }
}
