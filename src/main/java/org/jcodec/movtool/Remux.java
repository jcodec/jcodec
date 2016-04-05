package org.jcodec.movtool;
import js.lang.IllegalStateException;
import js.lang.System;


import static org.jcodec.common.io.NIOUtils.readableChannel;
import static org.jcodec.common.io.NIOUtils.writableChannel;
import static org.jcodec.containers.mp4.TrackType.VIDEO;

import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Rational;
import org.jcodec.containers.mp4.Brand;
import org.jcodec.containers.mp4.MP4Packet;
import org.jcodec.containers.mp4.WebOptimizedMP4Muxer;
import org.jcodec.containers.mp4.boxes.AudioSampleEntry;
import org.jcodec.containers.mp4.boxes.Edit;
import org.jcodec.containers.mp4.boxes.MovieBox;
import org.jcodec.containers.mp4.demuxer.AbstractMP4DemuxerTrack;
import org.jcodec.containers.mp4.demuxer.MP4Demuxer;
import org.jcodec.containers.mp4.demuxer.TimecodeMP4DemuxerTrack;
import org.jcodec.containers.mp4.muxer.AbstractMP4MuxerTrack;
import org.jcodec.containers.mp4.muxer.FramesMP4MuxerTrack;
import org.jcodec.containers.mp4.muxer.MP4Muxer;
import org.jcodec.containers.mp4.muxer.PCMMP4MuxerTrack;

import js.io.File;
import js.io.IOException;
import js.util.ArrayList;
import js.util.List;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class Remux {
    public static void main1(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("remux <movie>");
            return;
        }

        File tgt = new File(args[0]);
        File src = hidFile(tgt);
        tgt.renameTo(src);

        try {
            new Remux().remux(tgt, src, null, null);
        } catch (Throwable t) {
            tgt.renameTo(new File(tgt.getParentFile(), tgt.getName() + ".error"));
            src.renameTo(tgt);
        }
    }

    public interface Handler {
        public void handle(MovieBox mov) throws IOException;
    }

    public void remux(File tgt, File src, File timecode, Handler handler) throws IOException {
        SeekableByteChannel input = null;
        SeekableByteChannel output = null;
        SeekableByteChannel tci = null;
        try {
            input = readableChannel(src);
            output = writableChannel(tgt);
            MP4Demuxer demuxer = new MP4Demuxer(input);

            
            TimecodeMP4DemuxerTrack tt = null;
            if (timecode != null) {
                tci = readableChannel(src);
                MP4Demuxer tcd = new MP4Demuxer(tci);
                tt = tcd.getTimecodeTrack();
            }

            MP4Muxer muxer = WebOptimizedMP4Muxer.withOldHeader(output, Brand.MOV, demuxer.getMovie());

            List<AbstractMP4DemuxerTrack> at = demuxer.getAudioTracks();
            List<PCMMP4MuxerTrack> audioTracks = new ArrayList<PCMMP4MuxerTrack>();
            for (AbstractMP4DemuxerTrack demuxerTrack : at) {
                PCMMP4MuxerTrack att = muxer.addPCMAudioTrack(((AudioSampleEntry) demuxerTrack
                        .getSampleEntries()[0]).getFormat());
                audioTracks.add(att);
                att.setEdits(demuxerTrack.getEdits());
                att.setName(demuxerTrack.getName());
            }

            AbstractMP4DemuxerTrack vt = demuxer.getVideoTrack();
            FramesMP4MuxerTrack video = muxer.addTrack(VIDEO, (int) vt.getTimescale());
            // vt.open(input);
            video.setTimecode(muxer.addTimecodeTrack((int) vt.getTimescale()));
            copyEdits(vt, video, new Rational((int)vt.getTimescale(), demuxer.getMovie().getTimescale()));
            video.addSampleEntries(vt.getSampleEntries());
            MP4Packet pkt = null;
            while ((pkt = (MP4Packet)vt.nextFrame()) != null) {
                if (tt != null)
                    pkt = tt.getTimecode(pkt);
                pkt = processFrame(pkt);
                video.addFrame(pkt);

                for (int i = 0; i < at.size(); i++) {
                    AudioSampleEntry ase = (AudioSampleEntry) at.get(i).getSampleEntries()[0];
                    int frames = (int) (ase.getSampleRate() * pkt.getDuration() / vt.getTimescale());
                    MP4Packet apkt = (MP4Packet)at.get(i).nextFrame();
                    audioTracks.get(i).addSamples(apkt.getData());
                }
            }

            MovieBox movie = muxer.finalizeHeader();
            if (handler != null)
                handler.handle(movie);
            muxer.storeHeader(movie);

        } finally {
            NIOUtils.closeQuietly(input);
            NIOUtils.closeQuietly(output);
            NIOUtils.closeQuietly(tci);
        }
    }
    
    private void copyEdits(AbstractMP4DemuxerTrack from, AbstractMP4MuxerTrack two, Rational tsRatio) {
        List<Edit> edits = from.getEdits(), result = new ArrayList<Edit>();
        if(edits == null)
            return;
        for (Edit edit : edits) {
            result.add(new Edit(tsRatio.multiplyLong(edit.getDuration()), edit.getMediaTime(), edit.getRate()));
        }

        two.setEdits(result);
    }

    protected MP4Packet processFrame(MP4Packet pkt) {
        return pkt;
    }

    public static File hidFile(File tgt) {
        File src = new File(tgt.getParentFile(), "." + tgt.getName());
        if (src.exists()) {
            int i = 1;
            do {
                src = new File(tgt.getParentFile(), "." + tgt.getName() + "." + (i++));
            } while (src.exists());
        }
        return src;
    }
}
