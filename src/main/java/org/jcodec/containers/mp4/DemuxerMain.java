package org.jcodec.containers.mp4;
import static org.jcodec.common.io.IOUtils.readFileToByteArray;
import static org.jcodec.common.io.NIOUtils.readableChannel;
import static org.jcodec.common.io.NIOUtils.readableFileChannel;
import static org.jcodec.common.io.NIOUtils.rwChannel;
import static org.jcodec.common.io.NIOUtils.writableChannel;
import static org.jcodec.common.model.ColorSpace.RGB;
import static org.jcodec.containers.mp4.TrackType.VIDEO;

import org.jcodec.codecs.prores.ProresDecoder;
import org.jcodec.codecs.wav.WavHeader;
import org.jcodec.codecs.wav.WavHeader.FmtChunk;
import org.jcodec.common.io.FileChannelWrapper;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Picture;
import org.jcodec.containers.mp4.MP4Util.Atom;
import org.jcodec.containers.mp4.boxes.AudioSampleEntry;
import org.jcodec.containers.mp4.boxes.MovieBox;
import org.jcodec.containers.mp4.boxes.VideoSampleEntry;
import org.jcodec.containers.mp4.demuxer.AbstractMP4DemuxerTrack;
import org.jcodec.containers.mp4.demuxer.MP4Demuxer;
import org.jcodec.containers.mp4.muxer.FramesMP4MuxerTrack;
import org.jcodec.containers.mp4.muxer.MP4Muxer;
import org.jcodec.containers.mp4.muxer.PCMMP4MuxerTrack;
import org.jcodec.platform.Platform;
import org.jcodec.scale.Yuv422pToRgb;

import js.io.BufferedInputStream;
import js.io.BufferedOutputStream;
import js.io.File;
import js.io.FileInputStream;
import js.io.FileNotFoundException;
import js.io.FileOutputStream;
import js.io.IOException;
import js.io.InputStream;
import js.io.OutputStream;
import js.io.RandomAccessFile;
import js.lang.System;
import js.nio.ByteBuffer;
import js.nio.ByteOrder;
import js.nio.channels.FileChannel;
import js.util.List;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class DemuxerMain {

    public static void main1(String[] args) throws Exception {
        MP4Demuxer demuxer = new MP4Demuxer(readableFileChannel(args[0]));
        AbstractMP4DemuxerTrack vt = demuxer.getVideoTrack();
        ProresDecoder decoder = new ProresDecoder();

        long duration = vt.getDuration().getNum();
        long frameCount = vt.getFrameCount();

        for (int t = 0;; t++) {
            randomPts(vt, decoder, duration, new Yuv422pToRgb(2, 0), new File(args[1]));
            // randomFrame(vt, decoder, frameCount, transform, dest);
        }
    }

    private static void randomPts(AbstractMP4DemuxerTrack vt, ProresDecoder decoder, long duration, Yuv422pToRgb transform,
            File base) throws IOException {

        long pts = (long) (Math.random() * duration);
        vt.seekPts(pts);
        for (int i = 0; i < 10; i++) {
            Packet frames = vt.nextFrame();
            Picture pic = decoder.decodeFrame(frames.getData(), allocBuffer(vt));
            Picture dest = Picture.create(pic.getWidth(), pic.getHeight(), RGB);
            transform.transform(pic, dest);

//            ImageIO.write(AWTUtil.toBufferedImage(dest), "jpg", new File(base, "pts_" + pts + "_" + i + ".jpg"));
        }
    }

    private static int[][] allocBuffer(AbstractMP4DemuxerTrack vt) {
        VideoSampleEntry vse = (VideoSampleEntry) vt.getSampleEntries()[0];
        int size = (int) ((11 * vse.getWidth() * vse.getHeight()) / 10);
        return new int[][] { new int[size], new int[size], new int[size] };
    }

    private static void randomFrame(AbstractMP4DemuxerTrack vt, ProresDecoder decoder, long frameCount, Yuv422pToRgb transform,
            File base) throws IOException {
        long frame = (long) (Math.random() * frameCount);
        System.out.println(frame);
        vt.gotoFrame((int) frame);
        for (int i = 0; i < 10; i++) {
            Packet frames = vt.nextFrame();
            Picture pic = decoder.decodeFrame(frames.getData(), allocBuffer(vt));
            Picture dest = Picture.create(pic.getWidth(), pic.getHeight(), RGB);

            transform.transform(pic, dest);
//            ImageIO.write(AWTUtil.toBufferedImage(dest), "jpg", new File(base, "frm_" + frame + "_" + i + ".jpg"));
        }
    }

    private static void testAudio(File src, File wavFile) throws Exception {
        MP4Demuxer demuxer = new MP4Demuxer(readableChannel(src));
        AbstractMP4DemuxerTrack demuxerTrack = demuxer.getAudioTracks().get(0);

        FileChannelWrapper fos = NIOUtils.writableChannel(wavFile);

        AudioSampleEntry se = (AudioSampleEntry) demuxerTrack.getSampleEntries()[0];

        WavHeader wav = new WavHeader("RIFF", 40, "WAVE", new FmtChunk((short) 1, (short) se.getChannelCount(),
                (int) se.getSampleRate(), (int) se.getSampleRate() * se.getBytesPerFrame(),
                (short) se.getBytesPerFrame(), (short) ((se.getBytesPerFrame() / se.getChannelCount()) << 3)), 44,
                se.getBytesPerFrame() * demuxerTrack.getFrameCount());
        wav.write(fos);

        while (true) {
            Packet packet = demuxerTrack.nextFrame();
            fos.write(packet.getData());
        }
    }

    private static void testProres(File base) throws Exception {
        ProresDecoder decoder = new ProresDecoder();
        for (int i = 1;; i++) {
            System.out.println(i);
            ByteBuffer buffer = NIOUtils.fetchFromFile(new File(base, String.format("frame%08d.raw", i)));

            int sz = 1920 * 1080 * 2;
            decoder.decodeFrame(buffer, new int[][] { new int[sz], new int[sz], new int[sz] });
        }
    }

    private static void testVideo(File src, File base) throws IOException, FileNotFoundException {
        int startFn = 7572;
        MP4Demuxer demuxer = new MP4Demuxer(readableChannel(src));
        AbstractMP4DemuxerTrack vt = demuxer.getVideoTrack();
        vt.gotoFrame(startFn);
        for (int i = 0;; i++) {
            byte[] expected = readFileToByteArray(new File(base, String.format("frame%08d.raw", i + startFn
                    + 1)));
            Packet pkt = vt.nextFrame();
            if(!Platform.arrayEqualsByte(expected, NIOUtils.toArray(pkt.getData())))
                throw new RuntimeException("not equal");
            System.out.print(".");
            if ((i % 100) == 0)
                System.out.println();
        }
    }

    private static void testAudioMuxer(File wav, File out) throws IOException {
        WavHeader header = WavHeader.read(wav);
        RandomAccessFile _in = new RandomAccessFile(wav, "r");
        _in.seek(header.dataOffset);
        FileChannel ch = _in.getChannel();
        MP4Muxer muxer = MP4Muxer.createMP4MuxerToChannel(writableChannel(out));
        PCMMP4MuxerTrack track = muxer.addPCMTrack(48000, 1, 3,
                MP4Muxer.audioSampleEntry("in24", 1, 3, 1, 48000, ByteOrder.LITTLE_ENDIAN));

        ByteBuffer buffer = ByteBuffer.allocate(3 * 24000);
        while (ch.read(buffer) != -1) {
            track.addSamples(buffer);
        }
        muxer.writeHeader();
    }

    private static void testRemux(File src, File dst) throws Exception {
        MP4Muxer muxer = MP4Muxer.createMP4MuxerToChannel(writableChannel(dst));

        MP4Demuxer demuxer1 = new MP4Demuxer(readableChannel(src));
        AbstractMP4DemuxerTrack vt1 = demuxer1.getVideoTrack();

        FramesMP4MuxerTrack outTrack = muxer.addTrack(VIDEO, (int) vt1.getTimescale());
        outTrack.addSampleEntry(vt1.getSampleEntries()[0]);
        for (int i = 0; i < vt1.getFrameCount(); i++) {
            outTrack.addFrame((MP4Packet)vt1.nextFrame());
        }

        muxer.writeHeader();
    }

    private static void storeMdat(File src, File dst) throws Exception {
        List<Atom> rootAtoms = MP4Util.getRootAtoms(readableChannel(src));
        long mdatOff = -1, mdatSize = 0;
        for (Atom atom : rootAtoms) {
            if ("mdat".equals(atom.getHeader().getFourcc())) {
                mdatOff = atom.getOffset();
                mdatOff += atom.getHeader().headerSize();
                mdatSize = atom.getHeader().getBodySize();
            }
        }
        if (mdatOff == -1) {
            System.out.println("no mdat");
            return;
        }

        InputStream _in = new BufferedInputStream(new FileInputStream(src));
        _in.skip(mdatOff);
        OutputStream out = new BufferedOutputStream(new FileOutputStream(dst));

        for (int i = 0; i < mdatSize; i++) {
            out.write(_in.read());
        }
    }

    private static void narrowDown(File src, File dst) throws Exception {
        SeekableByteChannel rw = rwChannel(dst);
        SeekableByteChannel inp = readableChannel(src);
        List<Atom> rootAtoms = MP4Util.getRootAtoms(inp);
        for (Atom atom : rootAtoms) {
            if ("moov".equals(atom.getHeader().getFourcc())) {
                MovieBox box = (MovieBox) atom.parseBox(inp);
                MP4Util.writeMovie(rw, box);
            } else {
                atom.copy(inp, rw);
            }
        }
        inp.close();
        rw.close();
    }
}