package org.jcodec.common;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import javax.imageio.ImageIO;

import org.jcodec.codecs.mpeg12.MPEGDecoder;
import org.jcodec.codecs.pcm.PCMDecoder;
import org.jcodec.codecs.ppm.PPMEncoder;
import org.jcodec.codecs.prores.ProresDecoder;
import org.jcodec.codecs.s302.S302MDecoder;
import org.jcodec.common.io.Buffer;
import org.jcodec.common.io.BufferedRAInputStream;
import org.jcodec.common.io.FileRAInputStream;
import org.jcodec.common.io.MappedRAInputStream;
import org.jcodec.common.io.RAInputStream;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.containers.mp4.MP4Demuxer;
import org.jcodec.containers.mps.MPSDemuxer;
import org.jcodec.containers.mps.MTSDemuxer;
import org.jcodec.player.filters.MediaInfo;
import org.jcodec.scale.AWTUtil;
import org.jcodec.scale.ColorUtil;
import org.jcodec.scale.Transform;

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

    public static RAInputStream bufin(File f) throws IOException {
        return new BufferedRAInputStream(new FileRAInputStream(f));
    }

    public static RAInputStream mapin(File f) throws IOException {
        return new MappedRAInputStream(new FileRAInputStream(f), 16, 20);
    }

    public static void savePicture(Picture pic, String format, File file) throws IOException {
        Transform transform = ColorUtil.getTransform(pic.getColor(), ColorSpace.RGB);
        Picture rgb = Picture.create(pic.getWidth(), pic.getHeight(), ColorSpace.RGB);
        transform.transform(pic, rgb);
        BufferedImage bi = AWTUtil.toBufferedImage(rgb);
        ImageIO.write(bi, format, file);
    }

    public static void savePictureAsPPM(Picture pic, File file) throws IOException {
        Transform transform = ColorUtil.getTransform(pic.getColor(), ColorSpace.RGB);
        Picture rgb = Picture.create(pic.getWidth(), pic.getHeight(), ColorSpace.RGB);
        transform.transform(pic, rgb);
        new PPMEncoder().encodeFrame(rgb).writeTo(file);
    }
}
