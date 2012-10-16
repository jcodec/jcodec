package org.jcodec.common;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import org.jcodec.codecs.mpeg12.MPEGDecoder;
import org.jcodec.codecs.pcm.PCMDecoder;
import org.jcodec.codecs.prores.ProresDecoder;
import org.jcodec.codecs.s302.S302MDecoder;
import org.jcodec.common.io.Buffer;
import org.jcodec.containers.mp4.MP4Demuxer;
import org.jcodec.containers.mps.MPSDemuxer;
import org.jcodec.containers.mps.MTSDemuxer;
import org.jcodec.player.filters.MediaInfo;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class JCodecUtil {

    private static final VideoDecoder[] knownDecoders = new VideoDecoder[] { new ProresDecoder(), new MPEGDecoder() };

    public enum Format {
        MOV, MPEG_PS, MPEG_TS
    }

    public static Format detectFormat(File f) throws IOException {
        RandomAccessFile ff = null;
        try {
            ff = new RandomAccessFile(f, "r");
            return detectFormat(Buffer.fetchFrom(ff, 200 * 1024));
        } finally {
            if (ff != null)
                ff.close();
        }
    }

    public static Format detectFormat(Buffer b) {
        int movScore = MP4Demuxer.probe(b.fork());
        int psScore = MPSDemuxer.probe(b.fork());
        int tsScore = MTSDemuxer.probe(b.fork());

        if (movScore == 0 && psScore == 0 && tsScore == 0)
            return null;

        return movScore > psScore ? (movScore > tsScore ? Format.MOV : Format.MPEG_TS)
                : (psScore > tsScore ? Format.MPEG_PS : Format.MPEG_TS);
    }

    public static VideoDecoder detectDecoder(Buffer b) {
        int maxProbe = 0;
        VideoDecoder selected = null;
        for (VideoDecoder vd : knownDecoders) {
            int probe = vd.probe(b);
            if (probe > maxProbe) {
                selected = vd;
                maxProbe = probe;
            }
        }
        return selected;
    }

    public static VideoDecoder getVideoDecoder(String fourcc) {
        if ("apch".equals(fourcc) || "apcs".equals(fourcc) || "apco".equals(fourcc) || "apcn".equals(fourcc)
                || "ap4h".equals(fourcc))
            return new ProresDecoder();
        else if ("m2v1".equals(fourcc))
            return new MPEGDecoder();
        else
            return null;
    }

    public static AudioDecoder getAudioDecoder(String fourcc, MediaInfo.AudioInfo info) {
        if ("sowt".equals(fourcc) || "in24".equals(fourcc) || "twos".equals(fourcc) || "in32".equals(fourcc))
            return new PCMDecoder(info);
        else if ("s302".equals(fourcc))
            return new S302MDecoder();
        else
            return null;
    }
}