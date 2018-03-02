package org.jcodec.containers.mp4.muxer;

import static org.jcodec.common.Preconditions.checkState;
import static org.jcodec.common.VideoCodecMeta.createSimpleVideoCodecMeta;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jcodec.codecs.aac.ADTSParser;
import org.jcodec.codecs.h264.H264Utils;
import org.jcodec.codecs.h264.io.model.SeqParameterSet;
import org.jcodec.codecs.mpeg4.mp4.EsdsBox;
import org.jcodec.common.AudioFormat;
import org.jcodec.common.Codec;
import org.jcodec.common.VideoCodecMeta;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.logging.Logger;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Packet.FrameType;
import org.jcodec.common.model.Size;
import org.jcodec.containers.mp4.MP4TrackType;
import org.jcodec.containers.mp4.boxes.AudioSampleEntry;
import org.jcodec.containers.mp4.boxes.Box;
import org.jcodec.containers.mp4.boxes.MovieHeaderBox;
import org.jcodec.containers.mp4.boxes.PixelAspectExt;
import org.jcodec.containers.mp4.boxes.SampleEntry;
import org.jcodec.containers.mp4.boxes.VideoSampleEntry;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class CodecMP4MuxerTrack extends MP4MuxerTrack {

    private static Map<Codec, String> codec2fourcc = new HashMap<Codec, String>();

    static {
        codec2fourcc.put(Codec.MP1, ".mp1");
        codec2fourcc.put(Codec.MP2, ".mp2");
        codec2fourcc.put(Codec.MP3, ".mp3");
        codec2fourcc.put(Codec.H264, "avc1");
        codec2fourcc.put(Codec.AAC, "mp4a");
        codec2fourcc.put(Codec.PRORES, "apch");
        codec2fourcc.put(Codec.JPEG, "mjpg");
        codec2fourcc.put(Codec.PNG, "png ");
        codec2fourcc.put(Codec.V210, "v210");
    }

    private Codec codec;

    // SPS/PPS lists when h.264 is stored, otherwise these lists are not used.
    private List<ByteBuffer> spsList = new ArrayList<ByteBuffer>();
    private List<ByteBuffer> ppsList = new ArrayList<ByteBuffer>();

    // ADTS header used to construct audio sample entry for AAC
    private ADTSParser.Header adtsHeader;

    public CodecMP4MuxerTrack(int trackId, MP4TrackType type, Codec codec) {
        super(trackId, type);
        this.codec = codec;
    }

    @Override
    public void addFrame(Packet pkt) throws IOException {
        if (codec == Codec.H264) {
            ByteBuffer result = pkt.getData();
            
            if (pkt.frameType == FrameType.UNKNOWN) {
                pkt.setFrameType(H264Utils.isByteBufferIDRSlice(result) ? FrameType.KEY : FrameType.INTER);
            }
            
            H264Utils.wipePSinplace(result, spsList, ppsList);
            result = H264Utils.encodeMOVPacket(result);
            pkt = Packet.createPacketWithData(pkt, result);
        } else if (codec == Codec.AAC) {
            ByteBuffer result = pkt.getData();
            adtsHeader = ADTSParser.read(result);
//            System.out.println(String.format("crc_absent: %d, num_aac_frames: %d, size: %d, remaining: %d, %d, %d, %d",
//                    adtsHeader.getCrcAbsent(), adtsHeader.getNumAACFrames(), adtsHeader.getSize(), result.remaining(),
//                    adtsHeader.getObjectType(), adtsHeader.getSamplingIndex(), adtsHeader.getChanConfig()));
            pkt = Packet.createPacketWithData(pkt, result);
        }
        super.addFrame(pkt);
    }

    @Override
    public void addFrameInternal(Packet pkt, int entryNo) throws IOException {
        checkState(!finished, "The muxer track has finished muxing");

        if (_timescale == NO_TIMESCALE_SET) {
            if (adtsHeader != null) {
                _timescale = adtsHeader.getSampleRate();
            } else {
                _timescale = pkt.getTimescale();
            }
        }
        
        if (_timescale != pkt.getTimescale()) {
            pkt.setPts((pkt.getPts() * _timescale) / pkt.getTimescale());
            pkt.setDuration((pkt.getPts() * _timescale) / pkt.getDuration());
        }
        
        if (adtsHeader != null) {
            pkt.setDuration(1024);
        }

        super.addFrameInternal(pkt, entryNo);
    }

    @Override
    protected Box finish(MovieHeaderBox mvhd) throws IOException {
        checkState(!finished, "The muxer track has finished muxing");
        if (getEntries().isEmpty()) {
            if (codec == Codec.H264 && !spsList.isEmpty()) {
                SeqParameterSet sps = SeqParameterSet.read(spsList.get(0).duplicate());
                Size size = H264Utils.getPicSize(sps);
                VideoCodecMeta meta = createSimpleVideoCodecMeta(size, ColorSpace.YUV420);
                addVideoSampleEntry(meta);
            } else {
                Logger.warn("CodecMP4MuxerTrack: Creating a track without sample entry");
            }
        }
        setCodecPrivateIfNeeded();

        return super.finish(mvhd);
    }

    void addVideoSampleEntry(VideoCodecMeta meta) {
        SampleEntry se = VideoSampleEntry.videoSampleEntry(codec2fourcc.get(codec), meta.getSize(), "JCodec");
        if (meta.getPixelAspectRatio() != null)
            se.add(PixelAspectExt.createPixelAspectExt(meta.getPixelAspectRatio()));
        addSampleEntry(se);
    }

    private static List<ByteBuffer> selectUnique(List<ByteBuffer> bblist) {
        Set<ByteArrayWrapper> all = new HashSet<ByteArrayWrapper>();
        for (ByteBuffer byteBuffer : bblist) {
            all.add(new ByteArrayWrapper(byteBuffer));
        }
        List<ByteBuffer> result = new ArrayList<ByteBuffer>();
        for (ByteArrayWrapper bs : all) {
            result.add(bs.get());
        }
        return result;
    }

    public void setCodecPrivateIfNeeded() {
        if (codec == Codec.H264) {
            List<ByteBuffer> sps = selectUnique(spsList);
            List<ByteBuffer> pps = selectUnique(ppsList);
            if (!sps.isEmpty() && !pps.isEmpty()) {
                getEntries().get(0).add(H264Utils.createAvcCFromPS(sps, pps, 4));
            } else {
                Logger.warn("CodecMP4MuxerTrack: Not adding a sample entry for h.264 track, missing any SPS/PPS NAL units");
            }
        } else if (codec == Codec.AAC) {
            if (adtsHeader != null) {
                getEntries().get(0).add(EsdsBox.fromADTS(adtsHeader));
            } else {
                Logger.warn("CodecMP4MuxerTrack: Not adding a sample entry for AAC track, missing any ADTS headers.");
            }
        }
    }

    private static class ByteArrayWrapper {
        private byte[] bytes;

        public ByteArrayWrapper(ByteBuffer bytes) {
            this.bytes = NIOUtils.toArray(bytes);
        }

        public ByteBuffer get() {
            return ByteBuffer.wrap(bytes);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof ByteArrayWrapper))
                return false;
            return Arrays.equals(bytes, ((ByteArrayWrapper) obj).bytes);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(bytes);
        }
    }

    void addAudioSampleEntry(AudioFormat format) {
        AudioSampleEntry ase = AudioSampleEntry.compressedAudioSampleEntry(codec2fourcc.get(codec), (short) 1, (short) 16,
                format.getChannels(), format.getSampleRate(), 0, 0, 0);

        addSampleEntry(ase);
    }
}