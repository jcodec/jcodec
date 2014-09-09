package org.jcodec.containers.mkv.demuxer;

import static org.jcodec.containers.mkv.MKVType.Audio;
import static org.jcodec.containers.mkv.MKVType.Cluster;
import static org.jcodec.containers.mkv.MKVType.CodecPrivate;
import static org.jcodec.containers.mkv.MKVType.DisplayHeight;
import static org.jcodec.containers.mkv.MKVType.DisplayUnit;
import static org.jcodec.containers.mkv.MKVType.DisplayWidth;
import static org.jcodec.containers.mkv.MKVType.Info;
import static org.jcodec.containers.mkv.MKVType.PixelHeight;
import static org.jcodec.containers.mkv.MKVType.PixelWidth;
import static org.jcodec.containers.mkv.MKVType.SamplingFrequency;
import static org.jcodec.containers.mkv.MKVType.Segment;
import static org.jcodec.containers.mkv.MKVType.Timecode;
import static org.jcodec.containers.mkv.MKVType.TimecodeScale;
import static org.jcodec.containers.mkv.MKVType.TrackEntry;
import static org.jcodec.containers.mkv.MKVType.TrackNumber;
import static org.jcodec.containers.mkv.MKVType.TrackType;
import static org.jcodec.containers.mkv.MKVType.Tracks;
import static org.jcodec.containers.mkv.MKVType.Video;
import static org.jcodec.containers.mkv.MKVType.findFirst;
import static org.jcodec.containers.mkv.MKVType.findList;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.jcodec.common.DemuxerTrack;
import org.jcodec.common.SeekableByteChannel;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.TapeTimecode;
import org.jcodec.containers.mkv.MKVParser;
import org.jcodec.containers.mkv.MKVType;
import org.jcodec.containers.mkv.boxes.EbmlBase;
import org.jcodec.containers.mkv.boxes.EbmlBin;
import org.jcodec.containers.mkv.boxes.EbmlFloat;
import org.jcodec.containers.mkv.boxes.EbmlMaster;
import org.jcodec.containers.mkv.boxes.EbmlUint;
import org.jcodec.containers.mkv.boxes.MkvBlock;

public final class MKVDemuxer {
    private VideoTrack vTrack = null;
    private List<DemuxerTrack> aTracks = new ArrayList<DemuxerTrack>();
    private List<EbmlMaster> t;
    private SeekableByteChannel channel;
    long timescale = 1L;
    int pictureWidth;
    int pictureHeight;

    public MKVDemuxer(List<EbmlMaster> t, SeekableByteChannel fileChannelWrapper) {
        this.t = t;
        this.channel = fileChannelWrapper;
        demux();
    }

    private void demux() {
        EbmlUint ts = findFirst(t, Segment, Info, TimecodeScale);
        if (ts != null)
            timescale = ts.get();

        for (EbmlMaster aTrack : findList(t, EbmlMaster.class, Segment, Tracks, TrackEntry)) {
            long type = ((EbmlUint) findFirst(aTrack, TrackEntry, TrackType)).get();
            long id = ((EbmlUint) findFirst(aTrack, TrackEntry, TrackNumber)).get();
            if (type == 1) {
                // video
                if (vTrack != null)
                    throw new RuntimeException("More then 1 video track, can not compute...");
                EbmlBin videoCodecState = (EbmlBin) findFirst(aTrack, TrackEntry, CodecPrivate);
                ByteBuffer state = null;
                if (videoCodecState != null)
                    state = videoCodecState.data;
                
                EbmlUint width = (EbmlUint) findFirst(aTrack, TrackEntry, Video, PixelWidth);
                EbmlUint height = (EbmlUint) findFirst(aTrack, TrackEntry, Video, PixelHeight);
                EbmlUint dwidth = (EbmlUint) findFirst(aTrack, TrackEntry, Video, DisplayWidth);
                EbmlUint dheight = (EbmlUint) findFirst(aTrack, TrackEntry, Video, DisplayHeight);  
                EbmlUint unit = (EbmlUint) findFirst(aTrack, TrackEntry, Video, DisplayUnit);
                if (width != null && height != null){
                    pictureWidth = (int) width.get();
                    pictureHeight = (int) height.get();
                } else if (dwidth != null && dheight != null){
                    if (unit == null || unit.get() == 0){
                        pictureHeight = (int) dheight.get();
                        pictureWidth  = (int) dwidth.get();
                    } else {
                        throw new RuntimeException("DisplayUnits other then 0 are not implemented yet");
                    }
                }

                vTrack = new VideoTrack((int) id, state);

            } else if (type == 2) {
                AudioTrack audioTrack = new AudioTrack((int) id);
                EbmlFloat sf = (EbmlFloat) findFirst(aTrack, TrackEntry, Audio, SamplingFrequency);
                if (sf != null)
                    audioTrack.samplingFrequency = sf.get();
                
                aTracks.add(audioTrack);
            }
        }
        for (EbmlMaster aCluster : findList(t, EbmlMaster.class, Segment, Cluster)) {
            long baseTimecode = ((EbmlUint) findFirst(aCluster, Cluster, Timecode)).get();
            for (EbmlBase child : aCluster.children)
                if (MKVType.SimpleBlock.equals(child.type)) {
                    MkvBlock b = (MkvBlock) child;
                    b.absoluteTimecode = b.timecode + baseTimecode;
                    putIntoRightBasket(b);
                } else if (MKVType.BlockGroup.equals(child.type)) {
                    EbmlMaster group = (EbmlMaster) child;
                    for (EbmlBase grandChild : group.children) {
                        if (MKVType.Block.equals(grandChild)) {
                            MkvBlock b = (MkvBlock) child;
                            b.absoluteTimecode = b.timecode + baseTimecode;
                            putIntoRightBasket(b);
                        }
                    }
                }
        }
    }

    private void putIntoRightBasket(MkvBlock b) {
        if (b.trackNumber == vTrack.trackNo) {
            vTrack.blocks.add(b);

        } else {
            for (int i = 0; i < aTracks.size(); i++) {
                AudioTrack audio = (AudioTrack) aTracks.get(i);
                if (b.trackNumber == audio.trackNo) {
                    audio.blocks.add(IndexedBlock.make(audio.framesCount, b));
                    audio.framesCount += b.frameSizes.length;
                }
            }
        }
    }

    public static MKVDemuxer getDemuxer(SeekableByteChannel channel) throws IOException {
        MKVParser parser = new MKVParser(channel);
        return new MKVDemuxer(parser.parse(), channel);
    }

    public DemuxerTrack getVideoTrack() {
        return vTrack;
    }

    private static final TapeTimecode ZERO_TAPE_TIMECODE = new TapeTimecode((short) 0, (byte) 0, (byte) 0, (byte) 0, false);

    public class VideoTrack implements DemuxerTrack {
        private ByteBuffer state;
        public final int trackNo;
        private int frameIdx = 0;
        List<MkvBlock> blocks = new ArrayList<MkvBlock>();

        public VideoTrack(int trackNo, ByteBuffer state) {
            this.trackNo = trackNo;
            this.state = state;

        }

        @Override
        public Packet nextFrame() throws IOException {
            if (frameIdx >= blocks.size())
                return null;

            MkvBlock b = blocks.get(frameIdx);
            if (b == null)
                throw new RuntimeException("Something somewhere went wrong.");
            frameIdx++;
            /**
             * This part could be moved withing yet-another inner class, say MKVPacket to that channel is actually read only when Packet.getData() is executed.
             */
            channel.position(b.dataOffset);
            ByteBuffer data = ByteBuffer.allocate(b.dataLen);
            channel.read(data);
            data.flip();
            b.readFrames(data.duplicate());
            long duration = 1;
            if (frameIdx < blocks.size())
                duration = blocks.get(frameIdx).absoluteTimecode - b.absoluteTimecode;

            return new Packet(b.frames[0].duplicate(), b.absoluteTimecode, timescale, duration, frameIdx - 1, b.keyFrame, ZERO_TAPE_TIMECODE);
        }

        @Override
        public boolean gotoFrame(long i) {
            if (i > Integer.MAX_VALUE)
                return false;
            if (i > blocks.size())
                return false;

            frameIdx = (int) i;
            return true;
        }

        @Override
        public long getCurFrame() {
            return frameIdx;
        }

        @Override
        public void seek(double second) {
            throw new RuntimeException("Not implemented yet");

        }

        public int getFrameCount() {
            return blocks.size();
        }

        public ByteBuffer getCodecState() {
            return state;
        }

    }

    public static class IndexedBlock {
        public int firstFrameNo;
        public MkvBlock block;

        public static IndexedBlock make(int no, MkvBlock b) {
            IndexedBlock ib = new IndexedBlock();
            ib.firstFrameNo = no;
            ib.block = b;
            return ib;
        }
    }

    public class AudioTrack implements DemuxerTrack {
        public double samplingFrequency;
        public final int trackNo;
        List<IndexedBlock> blocks = new ArrayList<IndexedBlock>();
        private int framesCount = 0;
        private int frameIdx = 0;
        private int blockIdx = 0;
        private int frameInBlockIdx = 0;

        public AudioTrack(int trackNo) {
            this.trackNo = trackNo;
        }

        @Override
        public Packet nextFrame() throws IOException {
            if (frameIdx > blocks.size())
                return null;

            MkvBlock b = blocks.get(blockIdx).block;
            if (b == null)
                throw new RuntimeException("Something somewhere went wrong.");

            if (b.frames == null || b.frames.length == 0) {
                /**
                 * This part could be moved withing yet-another inner class, say MKVPacket to that channel is actually rean only when Packet.getData() is executed.
                 */
                channel.position(b.dataOffset);
                ByteBuffer data = ByteBuffer.allocate(b.dataLen);
                channel.read(data);
                b.readFrames(data);
            }
            ByteBuffer data = b.frames[frameInBlockIdx].duplicate();
            frameInBlockIdx++;
            frameIdx++;
            if (frameInBlockIdx >= b.frames.length) {
                blockIdx++;
                frameInBlockIdx = 0;
            }

            return new Packet(data, b.absoluteTimecode, Math.round(samplingFrequency), 1, 0, false, ZERO_TAPE_TIMECODE);
        }

        @Override
        public boolean gotoFrame(long i) {
            if (i > Integer.MAX_VALUE)
                return false;
            if (i > this.framesCount)
                return false;

            int frameBlockIdx = findBlockIndex(i);
            if (frameBlockIdx == -1)
                return false;

            frameIdx = (int) i;
            blockIdx = frameBlockIdx;
            frameInBlockIdx = (int) i - blocks.get(blockIdx).firstFrameNo;

            return true;
        }

        private int findBlockIndex(long i) {
            for (int blockIndex = 0; blockIndex < blocks.size(); blockIndex++) {
                if (i < blocks.get(blockIndex).block.frameSizes.length)
                    return blockIndex;

                i -= blocks.get(blockIndex).block.frameSizes.length;
            }

            return -1;
        }

        @Override
        public long getCurFrame() {
            return frameIdx;
        }

        @Override
        public void seek(double second) {
            throw new RuntimeException("Not implemented yet");
        }

        /**
         * Get multiple frames
         * 
         * @param count
         * @return
         */
        public Packet getFrames(int count) {
            if (count + frameIdx >= framesCount)
                return null;
            List<ByteBuffer> packetFrames = new ArrayList<ByteBuffer>();
            MkvBlock firstBlockInAPacket = blocks.get(blockIdx).block;
            while (count > 0) {
                MkvBlock b = blocks.get(blockIdx).block;
                if (b.frames == null || b.frames.length == 0) {
                    /**
                     * This part could be moved withing yet-another inner class, say MKVPacket to that channel is actually rean only when Packet.getData() is executed.
                     */
                    try {
                        channel.position(b.dataOffset);
                        ByteBuffer data = ByteBuffer.allocate(b.dataLen);
                        channel.read(data);
                        b.readFrames(data);
                    } catch (IOException ioe) {
                        throw new RuntimeException("while reading frames of a Block at offset 0x" + Long.toHexString(b.dataOffset).toUpperCase() + ")", ioe);
                    }
                }
                packetFrames.add(b.frames[frameInBlockIdx].duplicate());
                frameIdx++;
                frameInBlockIdx++;
                if (frameInBlockIdx >= b.frames.length) {
                    frameInBlockIdx = 0;
                    blockIdx++;
                }
                count--;
            }
            
            int size = 0;
            for (ByteBuffer aFrame : packetFrames)
                size += aFrame.limit();
            
            ByteBuffer data = ByteBuffer.allocate(size);
            for (ByteBuffer aFrame : packetFrames)
                data.put(aFrame);
            
            return new Packet(data, firstBlockInAPacket.absoluteTimecode,  Math.round(samplingFrequency),  packetFrames.size(), 0, false, ZERO_TAPE_TIMECODE);
        }

    }

    public int getPictureWidth() {
        return pictureWidth;
    }

    public int getPictureHeight() {
        return pictureHeight;
    }

    public List<DemuxerTrack> getAudioTracks() {
        return aTracks;
    }

}
