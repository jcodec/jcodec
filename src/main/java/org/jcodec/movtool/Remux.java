package org.jcodec.movtool;

import static org.jcodec.common.JCodecUtil.bufin;
import static org.jcodec.containers.mp4.TrackType.VIDEO;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jcodec.common.io.FileRAOutputStream;
import org.jcodec.common.io.RAInputStream;
import org.jcodec.common.io.RAOutputStream;
import org.jcodec.containers.mp4.Brand;
import org.jcodec.containers.mp4.MP4Demuxer;
import org.jcodec.containers.mp4.MP4Demuxer.DemuxerTrack;
import org.jcodec.containers.mp4.MP4Muxer;
import org.jcodec.containers.mp4.MP4Muxer.CompressedTrack;
import org.jcodec.containers.mp4.MP4Muxer.UncompressedTrack;
import org.jcodec.containers.mp4.MP4Packet;
import org.jcodec.containers.mp4.WebOptimizedMP4Muxer;
import org.jcodec.containers.mp4.boxes.AudioSampleEntry;
import org.jcodec.containers.mp4.boxes.MovieBox;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class Remux {
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("remux <movie>");
            return;
        }

        File tgt = new File(args[0]);
        File src = hidFile(tgt);
        tgt.renameTo(src);

        try {
            new Remux().remux(tgt, src, null);
        } catch (Throwable t) {
            tgt.renameTo(new File(tgt.getParentFile(), tgt.getName() + ".error"));
            src.renameTo(tgt);
        }
    }

    public interface Handler {
        public void handle(MovieBox mov) throws IOException;
    }

    public void remux(File tgt, File src, Handler handler) throws IOException {
        RAInputStream input = null;
        RAOutputStream output = null;
        try {
            input = bufin(src);
            output = new FileRAOutputStream(tgt);
            MP4Demuxer demuxer = new MP4Demuxer(input);

            MP4Muxer muxer = WebOptimizedMP4Muxer.withOldHeader(output, Brand.MOV, demuxer.getMovie());

            List<DemuxerTrack> at = demuxer.getAudioTracks();
            List<MP4Muxer.UncompressedTrack> audioTracks = new ArrayList<MP4Muxer.UncompressedTrack>();
            for (DemuxerTrack demuxerTrack : at) {
                UncompressedTrack att = muxer.addUncompressedAudioTrack(((AudioSampleEntry) demuxerTrack
                        .getSampleEntries()[0]).getFormat());
                audioTracks.add(att);
                att.setEdits(demuxerTrack.getEdits());
                att.setName(demuxerTrack.getName());
            }

            DemuxerTrack vt = demuxer.getVideoTrack();
            CompressedTrack video = muxer.addTrackForCompressed(VIDEO, (int) vt.getTimescale());
            // vt.open(input);
            video.setTimecode(muxer.addTimecodeTrack((int) vt.getTimescale()));
            video.setEdits(vt.getEdits());
            video.addSampleEntries(vt.getSampleEntries());
            MP4Packet pkt = null;
            while ((pkt = vt.getFrames(1)) != null) {
                pkt = processFrame(pkt);
                video.addFrame(pkt);

                for (int i = 0; i < at.size(); i++) {
                    AudioSampleEntry ase = (AudioSampleEntry) at.get(i).getSampleEntries()[0];
                    int frames = (int) (ase.getSampleRate() * pkt.getDuration() / vt.getTimescale());
                    MP4Packet apkt = at.get(i).getFrames(frames);
                    audioTracks.get(i).addSamples(apkt.getData());
                }
            }

            MovieBox movie = muxer.finalizeHeader();
            movie.setTimescale(demuxer.getMovie().getTimescale());
            movie.setDuration(demuxer.getMovie().getDuration());
            if (handler != null) {
                handler.handle(movie);
            }
            muxer.storeHeader(movie);

            output.flush();
        } finally {
            if (input != null)
                input.close();
            if (output != null)
                output.close();
        }
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
