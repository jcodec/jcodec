package org.jcodec.containers.mp4.demuxer;

import static org.jcodec.common.TrackType.AUDIO;
import static org.jcodec.common.TrackType.OTHER;
import static org.jcodec.common.TrackType.VIDEO;
import static org.jcodec.common.VideoCodecMeta.createSimpleVideoCodecMeta;

import java.nio.ByteBuffer;
import java.util.List;

import org.jcodec.codecs.aac.AACUtils;
import org.jcodec.codecs.h264.H264Utils;
import org.jcodec.codecs.h264.io.model.SeqParameterSet;
import org.jcodec.codecs.h264.mp4.AvcCBox;
import org.jcodec.common.AudioCodecMeta;
import org.jcodec.common.Codec;
import org.jcodec.common.CodecMeta;
import org.jcodec.common.DemuxerTrackMeta;
import org.jcodec.common.Ints;
import org.jcodec.common.TrackType;
import org.jcodec.common.VideoCodecMeta;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.RationalLarge;
import org.jcodec.containers.mp4.MP4TrackType;
import org.jcodec.containers.mp4.boxes.AudioSampleEntry;
import org.jcodec.containers.mp4.boxes.Box;
import org.jcodec.containers.mp4.boxes.NodeBox;
import org.jcodec.containers.mp4.boxes.PixelAspectExt;
import org.jcodec.containers.mp4.boxes.SyncSamplesBox;
import org.jcodec.containers.mp4.boxes.TrackHeaderBox;
import org.jcodec.containers.mp4.boxes.TrakBox;
import org.jcodec.containers.mp4.boxes.VideoSampleEntry;
import org.jcodec.platform.Platform;

public class MP4DemuxerTrackMeta {
    public static DemuxerTrackMeta fromTrack(AbstractMP4DemuxerTrack track) {
        TrakBox trak = track.getBox();
        SyncSamplesBox stss = NodeBox.findFirstPath(trak, SyncSamplesBox.class, Box.path("mdia.minf.stbl.stss"));
        int[] syncSamples = stss == null ? null : stss.getSyncSamples();
        
        int[] seekFrames;
        if (syncSamples == null) {
            // all frames are I-frames
            seekFrames = new int[(int) track.getFrameCount()];
            for (int i = 0; i < seekFrames.length; i++) {
                seekFrames[i] = i;
            }
        } else {
            seekFrames = Platform.copyOfInt(syncSamples, syncSamples.length);
            for (int i = 0; i < seekFrames.length; i++)
                seekFrames[i]--;
        }

        MP4TrackType type = track.getType();
        TrackType t = type == MP4TrackType.VIDEO ? VIDEO : (type == MP4TrackType.SOUND ? AUDIO : OTHER);
        CodecMeta codecMeta = null;
        Codec codec = Codec.codecByFourcc(track.getFourcc());
        ByteBuffer codecPrivate = getCodecPrivate(track);
        if (type == MP4TrackType.VIDEO) {
            VideoCodecMeta videoCodecMeta = createSimpleVideoCodecMeta(codec, codecPrivate, trak.getCodedSize(), getColorInfo(track));
            PixelAspectExt pasp = NodeBox.findFirst(track.getSampleEntries()[0], PixelAspectExt.class, "pasp");
            if (pasp != null)
                videoCodecMeta.setPixelAspectRatio(pasp.getRational());
            codecMeta = videoCodecMeta;
        } else if (type == MP4TrackType.SOUND) {
            AudioSampleEntry ase = (AudioSampleEntry) track.getSampleEntries()[0];
            codecMeta = AudioCodecMeta.fromAudioFormat(codec, codecPrivate, ase.getFormat());
        }
        RationalLarge duration = track.getDuration();
        double sec = (double) duration.getNum() / duration.getDen();
        int frameCount = Ints.checkedCast(track.getFrameCount());
        DemuxerTrackMeta meta = new DemuxerTrackMeta(t, sec, seekFrames, frameCount, codecMeta);

        if (type == MP4TrackType.VIDEO) {
            TrackHeaderBox tkhd = NodeBox.findFirstPath(trak, TrackHeaderBox.class, Box.path("tkhd"));

            DemuxerTrackMeta.Orientation orientation;
            if (tkhd.isOrientation90())
                orientation = DemuxerTrackMeta.Orientation.D_90;
            else if (tkhd.isOrientation180())
                orientation = DemuxerTrackMeta.Orientation.D_180;
            else if (tkhd.isOrientation270())
                orientation = DemuxerTrackMeta.Orientation.D_270;
            else
                orientation = DemuxerTrackMeta.Orientation.D_0;

            meta.setOrientation(orientation);
        }

        return meta;
    }

    protected static ColorSpace getColorInfo(AbstractMP4DemuxerTrack track) {
        Codec codec = Codec.codecByFourcc(track.getFourcc());
        if (codec == Codec.H264) {
            AvcCBox avcC = H264Utils.parseAVCC((VideoSampleEntry) track.getSampleEntries()[0]);
            List<ByteBuffer> spsList = avcC.getSpsList();
            if (spsList.size() > 0) {
                SeqParameterSet sps = SeqParameterSet.read(spsList.get(0).duplicate());
                return sps.getChromaFormatIdc();
            }
        }
        return null;
    }

    public static ByteBuffer getCodecPrivate(AbstractMP4DemuxerTrack track) {
        Codec codec = Codec.codecByFourcc(track.getFourcc());
        if (codec == Codec.H264) {
            AvcCBox avcC = H264Utils.parseAVCC((VideoSampleEntry) track.getSampleEntries()[0]);
            return H264Utils.avcCToAnnexB(avcC);

        } else if (codec == Codec.AAC) {
            return AACUtils.getCodecPrivate(track.getSampleEntries()[0]);
        }
        // This codec does not have private section
        return null;
    }
}
