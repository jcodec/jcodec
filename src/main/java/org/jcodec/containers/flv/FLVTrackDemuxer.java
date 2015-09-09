package org.jcodec.containers.flv;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;

import org.jcodec.common.Codec;
import org.jcodec.common.DemuxerTrack;
import org.jcodec.common.DemuxerTrackMeta;
import org.jcodec.common.LongArrayList;
import org.jcodec.common.SeekableByteChannel;
import org.jcodec.common.SeekableDemuxerTrack;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Size;
import org.jcodec.containers.flv.FLVPacket.Type;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Demuxer frontend for FLV, track based wrapper
 * 
 * @author The JCodec project
 * 
 */
public class FLVTrackDemuxer {

    private FLVDemuxer demuxer;

    private FLVDemuxerTrack video;
    private FLVDemuxerTrack audio;
    private LinkedList<FLVPacket> packets = new LinkedList<FLVPacket>();

    private SeekableByteChannel in;

    public class FLVDemuxerTrack implements SeekableDemuxerTrack {

        private Type type;
        private int curFrame;
        private Codec codec;
        private LongArrayList framePositions = new LongArrayList();

        public FLVDemuxerTrack(Type type) throws IOException {
            this.type = type;
            FLVPacket frame = nextFrameI(type, false);
            codec = frame.getTagHeader().getCodec();
        }

        @Override
        public Packet nextFrame() throws IOException {
            FLVPacket frame = nextFrameI(type, true);
            framePositions.add(frame.getPosition());
            return frame;
        }

        public Packet prevFrame() throws IOException {
            FLVPacket frame = prevFrameI(type, true);
            // framePositions.add(nextFrameI.getPosition());
            return frame;
        }

        public Packet pickFrame() throws IOException {
            FLVPacket frame = nextFrameI(type, false);
            // framePositions.add(nextFrameI.getPosition());
            return frame;
        }

        @Override
        public DemuxerTrackMeta getMeta() {
            return new DemuxerTrackMeta(type == Type.VIDEO ? DemuxerTrackMeta.Type.VIDEO : DemuxerTrackMeta.Type.AUDIO,
                    null, 0, 0, new Size(0, 0));
        }

        public Codec getCodec() {
            return codec;
        }

        @Override
        public boolean gotoFrame(long i) throws IOException {
            if (i >= framePositions.size())
                return false;
            resetToPosition(framePositions.get((int) i));
            return true;
        }

        @Override
        public boolean gotoSyncFrame(long i) {
            throw new RuntimeException();
        }

        @Override
        public long getCurFrame() {
            return curFrame;
        }

        @Override
        public void seek(double second) throws IOException {
            seekI(second);
        }
    }

    public FLVTrackDemuxer(SeekableByteChannel in) throws IOException {
        this.in = in;
        in.position(0);
        demuxer = new FLVDemuxer(in);
        video = new FLVDemuxerTrack(Type.VIDEO);
        audio = new FLVDemuxerTrack(Type.AUDIO);
    }

    private void resetToPosition(long position) throws IOException {
        in.position(position);
        demuxer.reset();
        packets.clear();
    }

    private void seekI(double second) throws IOException {
        packets.clear();
        FLVPacket base;
        while ((base = demuxer.getPacket()) != null && base.getPtsD() == 0)
            ;

        in.position(base.getPosition() + 0x100000);
        demuxer.reposition();
        FLVPacket off = demuxer.getPacket();

        int byteRate = (int) ((off.getPosition() - base.getPosition()) / (off.getPtsD() - base.getPtsD()));
        long offset = base.getPosition() + (long) ((second - base.getPtsD()) * byteRate);

        in.position(offset);
        demuxer.reposition();
        // 5 reposition attempts
        for (int i = 0; i < 5; ++i) {
            FLVPacket pkt = demuxer.getPacket();
            double distance = second - pkt.getPtsD();
            if (distance > 0 && distance < 10) {
                // Read to the right frame
                System.out.println("Crawling forward: " + distance);
                FLVPacket cool;
                while ((cool = demuxer.getPacket()) != null && cool.getPtsD() < second)
                    ;
                if (cool != null)
                    packets.add(pkt);
                return;
            } else if (distance < 0 && distance > -10) {
                // Read back to the frame
                System.out.println("Overshoot by: " + (-distance));
                in.position(pkt.getPosition() + (long) ((distance - 1) * byteRate));
                demuxer.reposition();
            }
        }
    }

    private FLVPacket nextFrameI(Type type, boolean remove) throws IOException {
        for (Iterator<FLVPacket> it = packets.iterator(); it.hasNext();) {
            FLVPacket pkt = it.next();
            if (pkt.getType() == type) {
                if (remove)
                    it.remove();
                return pkt;
            }
        }
        FLVPacket pkt;
        while ((pkt = demuxer.getPacket()) != null && pkt.getType() != type)
            packets.add(pkt);
        if (!remove)
            packets.add(pkt);

        return pkt;
    }

    private FLVPacket prevFrameI(Type type, boolean remove) throws IOException {
        for (ListIterator<FLVPacket> it = packets.listIterator(); it.hasPrevious();) {
            FLVPacket pkt = it.previous();
            if (pkt.getType() == type) {
                if (remove)
                    it.remove();
                return pkt;
            }
        }
        FLVPacket pkt;
        while ((pkt = demuxer.prevPacket()) != null && pkt.getType() != type)
            packets.add(0, pkt);
        if (!remove)
            packets.add(0, pkt);

        return pkt;

    }

    public DemuxerTrack[] getTracks() {
        return new DemuxerTrack[] { video, audio };
    }

    public DemuxerTrack getVideoTrack() {
        return video;
    }

    public DemuxerTrack getAudioTrack() {
        return video;
    }
}
