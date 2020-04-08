package org.jcodec.containers.mkv;

import static org.jcodec.containers.mkv.MKVType.AlphaMode;
import static org.jcodec.containers.mkv.MKVType.AspectRatioType;
import static org.jcodec.containers.mkv.MKVType.AttachmentLink;
import static org.jcodec.containers.mkv.MKVType.Audio;
import static org.jcodec.containers.mkv.MKVType.BitDepth;
import static org.jcodec.containers.mkv.MKVType.Block;
import static org.jcodec.containers.mkv.MKVType.BlockDuration;
import static org.jcodec.containers.mkv.MKVType.BlockGroup;
import static org.jcodec.containers.mkv.MKVType.Channels;
import static org.jcodec.containers.mkv.MKVType.Cluster;
import static org.jcodec.containers.mkv.MKVType.CodecDecodeAll;
import static org.jcodec.containers.mkv.MKVType.CodecID;
import static org.jcodec.containers.mkv.MKVType.CodecName;
import static org.jcodec.containers.mkv.MKVType.CueBlockNumber;
import static org.jcodec.containers.mkv.MKVType.CueClusterPosition;
import static org.jcodec.containers.mkv.MKVType.CuePoint;
import static org.jcodec.containers.mkv.MKVType.CueTime;
import static org.jcodec.containers.mkv.MKVType.CueTrack;
import static org.jcodec.containers.mkv.MKVType.CueTrackPositions;
import static org.jcodec.containers.mkv.MKVType.Cues;
import static org.jcodec.containers.mkv.MKVType.DefaultDuration;
import static org.jcodec.containers.mkv.MKVType.DisplayHeight;
import static org.jcodec.containers.mkv.MKVType.DisplayUnit;
import static org.jcodec.containers.mkv.MKVType.DisplayWidth;
import static org.jcodec.containers.mkv.MKVType.Duration;
import static org.jcodec.containers.mkv.MKVType.FlagDefault;
import static org.jcodec.containers.mkv.MKVType.FlagEnabled;
import static org.jcodec.containers.mkv.MKVType.FlagForced;
import static org.jcodec.containers.mkv.MKVType.FlagInterlaced;
import static org.jcodec.containers.mkv.MKVType.FlagLacing;
import static org.jcodec.containers.mkv.MKVType.Info;
import static org.jcodec.containers.mkv.MKVType.Language;
import static org.jcodec.containers.mkv.MKVType.MaxCache;
import static org.jcodec.containers.mkv.MKVType.MinCache;
import static org.jcodec.containers.mkv.MKVType.Name;
import static org.jcodec.containers.mkv.MKVType.OutputSamplingFrequency;
import static org.jcodec.containers.mkv.MKVType.PixelCropBottom;
import static org.jcodec.containers.mkv.MKVType.PixelCropLeft;
import static org.jcodec.containers.mkv.MKVType.PixelCropRight;
import static org.jcodec.containers.mkv.MKVType.PixelCropTop;
import static org.jcodec.containers.mkv.MKVType.PixelHeight;
import static org.jcodec.containers.mkv.MKVType.PixelWidth;
import static org.jcodec.containers.mkv.MKVType.Position;
import static org.jcodec.containers.mkv.MKVType.ReferenceBlock;
import static org.jcodec.containers.mkv.MKVType.SamplingFrequency;
import static org.jcodec.containers.mkv.MKVType.Segment;
import static org.jcodec.containers.mkv.MKVType.StereoMode;
import static org.jcodec.containers.mkv.MKVType.Timecode;
import static org.jcodec.containers.mkv.MKVType.TimecodeScale;
import static org.jcodec.containers.mkv.MKVType.TrackEntry;
import static org.jcodec.containers.mkv.MKVType.TrackNumber;
import static org.jcodec.containers.mkv.MKVType.TrackOverlay;
import static org.jcodec.containers.mkv.MKVType.TrackType;
import static org.jcodec.containers.mkv.MKVType.Tracks;
import static org.jcodec.containers.mkv.MKVType.Video;
import static org.jcodec.containers.mkv.MKVType.findAll;
import static org.jcodec.containers.mkv.MKVType.findFirst;
import static org.jcodec.containers.mkv.MKVType.findFirstTree;

import org.jcodec.common.io.FileChannelWrapper;
import org.jcodec.containers.mkv.boxes.EbmlBase;
import org.jcodec.containers.mkv.boxes.EbmlFloat;
import org.jcodec.containers.mkv.boxes.EbmlMaster;
import org.jcodec.containers.mkv.boxes.EbmlString;
import org.jcodec.containers.mkv.boxes.EbmlUint;
import org.jcodec.containers.mkv.boxes.EbmlUlong;
import org.jcodec.containers.mkv.boxes.MkvBlock;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.StringBuilder;
import java.lang.System;

public class DisplayTimecodesTest {

    @Test
    public void test() throws IOException {
        String filename = "./src/test/resources/mkv/";
        filename += "10frames.webm";
        System.out.println("Scanning file: " + filename);
        FileInputStream iFS = new FileInputStream(new File(filename));
        MKVParser reader = new MKVParser(new FileChannelWrapper(iFS.getChannel()));
        MKVType[] path = {Segment};
        EbmlMaster s = findFirstTree(reader.parse(), path);
        printCues(s);
        printBlocks(s);
        printTracks(s);
        printInfo(s);
    }

    private void printInfo(EbmlMaster s) {
        StringBuilder sb = new StringBuilder("info ");
        MKVType[] path = {Segment, Info, TimecodeScale};
        EbmlUint scale = (EbmlUint) findFirst(s, path);
        MKVType[] path1 = {Segment, Info, Duration};
        EbmlFloat duration = (EbmlFloat) findFirst(s, path1);
        appendUint(sb, "scale", scale);
        appendFloat(sb, "duration", duration);
        sb.append("\n");
        System.out.println(sb.toString());
    }

    private void printCues(EbmlMaster s) {
        StringBuilder sb = new StringBuilder();
        MKVType[] path4 = {Segment, Cues, CuePoint};
        for (EbmlMaster aCuePoint : findAll(s, path4).toArray(new EbmlMaster[0])) {
            MKVType[] path = {CuePoint, CueTime};
            EbmlUint time = (EbmlUint) findFirst(aCuePoint, path);
            sb.append("cue time: ").append(time.getUint());
            MKVType[] path5 = {CuePoint, CueTrackPositions};
            for (EbmlMaster aCueTrackPosition : findAll(aCuePoint, path5).toArray(new EbmlMaster[0])) {
                MKVType[] path1 = {CueTrackPositions, CueTrack};
                appendUint(sb, "track", (EbmlUint) findFirst(aCueTrackPosition, path1));
                MKVType[] path2 = {CueTrackPositions, CueClusterPosition};
                EbmlUint EbmlMaster = (EbmlUint) findFirst(aCueTrackPosition, path2);
                if (EbmlMaster != null)
                    sb.append(" EbmlMaster offset ").append(EbmlMaster.getUint() + s.dataOffset);
                MKVType[] path3 = {CueTrackPositions, CueBlockNumber};
                appendUint(sb, "block", (EbmlUint) findFirst(aCueTrackPosition, path3));
            }
            sb.append("\n");
        }
        System.out.println(sb.toString());
    }

    public static void appendUint(StringBuilder b, String caption, EbmlUint e) {
        if (e != null)
            b.append(" ").append(caption).append(": ").append(e.getUint());
    }

    public static void appendUlong(StringBuilder b, String caption, EbmlUlong e) {
        if (e != null)
            b.append(" ").append(caption).append(": ").append(e.getUlong());
    }

    public static void appendString(StringBuilder b, String caption, EbmlString e) {
        if (e != null)
            b.append(" ").append(caption).append(": ").append(e.getString());
    }

    public static void appendFloat(StringBuilder b, String caption, EbmlFloat e) {
        if (e != null)
            b.append(" ").append(caption).append(": ").append(e.getDouble());
    }

    private void printBlocks(EbmlMaster s) {
        StringBuilder sb = new StringBuilder();
        MKVType[] path5 = {Segment, Cluster};
        for (EbmlMaster aEbmlMaster : findAll(s, path5).toArray(new EbmlMaster[0])) {
            MKVType[] path = {Cluster, Timecode};
            EbmlUint time = (EbmlUint) findFirst(aEbmlMaster, path);
            MKVType[] path1 = {Cluster, Position};
            EbmlUint position = (EbmlUint) findFirst(aEbmlMaster, path1);
            sb.append("EbmlMaster time: ").append(time.getUint());
            appendUint(sb, "position", position);
            sb.append(" offset: ").append(aEbmlMaster.offset).append("\n");
            for (EbmlBase aChild : aEbmlMaster.children) {
                if (aChild instanceof MkvBlock) {
                    MkvBlock block = (MkvBlock) aChild;
                    sb.append("    block tarck: ").append(block.trackNumber).append(" timecode: ").append(block.timecode).append(" offset: ").append(block.offset).append("\n");
                    sb.append("    block real timecode: " + (time.getUint() + block.timecode));
                    sb.append("\n");
                } else if (aChild instanceof EbmlMaster) {
                    MKVType[] path2 = {BlockGroup, Block};
                    MkvBlock block = (MkvBlock) findFirst((EbmlMaster) aChild, path2);
                    sb.append("    block tarck: ").append(block.trackNumber).append(" timecode: ").append(block.timecode).append(" offset: ").append(block.offset).append("\n");
                    sb.append("    block real timecode: " + (time.getUint() + block.timecode));
                    MKVType[] path3 = {BlockGroup, ReferenceBlock};

                    appendUint(sb, "reference", (EbmlUint) findFirst(aEbmlMaster, path3));
                    MKVType[] path4 = {Cluster, BlockDuration};
                    appendUint(sb, "duration", (EbmlUint) findFirst(aEbmlMaster, path4));
                    sb.append("\n");
                }

            }
            sb.append("\n");
        }
        System.out.println(sb.toString());
    }

    private void printTracks(EbmlMaster s) {
        StringBuilder sb = new StringBuilder();
        MKVType[] path31 = {Segment, Tracks, TrackEntry};
        for (EbmlMaster anEntry : findAll(s, path31).toArray(new EbmlMaster[0])) {
            sb.append("track ");
            MKVType[] path = {TrackEntry, Name};
            appendString(sb, "name", (EbmlString) findFirst(anEntry, path));
            MKVType[] path1 = {TrackEntry, Language};
            appendString(sb, "language", (EbmlString) findFirst(anEntry, path1));
            MKVType[] path2 = {TrackEntry, TrackNumber};
            appendUint(sb, "number", (EbmlUint) findFirst(anEntry, path2));
            MKVType[] path3 = {TrackEntry, TrackType};
            appendUint(sb, "type", (EbmlUint) findFirst(anEntry, path3));
            MKVType[] path4 = {TrackEntry, FlagEnabled};
            appendUint(sb, "enabled", (EbmlUint) findFirst(anEntry, path4));
            MKVType[] path5 = {TrackEntry, FlagDefault};
            appendUint(sb, "default", (EbmlUint) findFirst(anEntry, path5));
            MKVType[] path6 = {TrackEntry, FlagForced};
            appendUint(sb, "forced", (EbmlUint) findFirst(anEntry, path6));
            MKVType[] path7 = {TrackEntry, FlagLacing};
            appendUint(sb, "lacing", (EbmlUint) findFirst(anEntry, path7));
            MKVType[] path8 = {TrackEntry, MinCache};
            appendUint(sb, "mincache", (EbmlUint) findFirst(anEntry, path8));
            MKVType[] path9 = {TrackEntry, MaxCache};
            appendUint(sb, "maccache", (EbmlUint) findFirst(anEntry, path9));
            MKVType[] path10 = {TrackEntry, DefaultDuration};
            appendUint(sb, "defaultduration", (EbmlUint) findFirst(anEntry, path10));
            MKVType[] path11 = {TrackEntry, CodecID};
            appendString(sb, "codecid", (EbmlString) findFirst(anEntry, path11));
            MKVType[] path12 = {TrackEntry, CodecName};
            appendString(sb, "codecname", (EbmlString) findFirst(anEntry, path12));
            MKVType[] path13 = {TrackEntry, AttachmentLink};
            appendString(sb, "attachmentlink", (EbmlString) findFirst(anEntry, path13));
            MKVType[] path14 = {TrackEntry, CodecDecodeAll};
            appendUint(sb, "codecdecodeall", (EbmlUint) findFirst(anEntry, path14));
            MKVType[] path15 = {TrackEntry, TrackOverlay};
            appendUint(sb, "overlay", (EbmlUint) findFirst(anEntry, path15));
            MKVType[] path16 = {TrackEntry, Video};
            EbmlMaster video = (EbmlMaster) findFirst(anEntry, path16);
            MKVType[] path17 = {TrackEntry, Audio};
            EbmlMaster audio = (EbmlMaster) findFirst(anEntry, path17);
            if (video != null) {
                sb.append("\n    video ");
                MKVType[] path18 = {Video, FlagInterlaced};
                appendUint(sb, "interlaced", (EbmlUint) findFirst(video, path18));
                MKVType[] path19 = {Video, StereoMode};
                appendUint(sb, "stereo", (EbmlUint) findFirst(video, path19));
                MKVType[] path20 = {Video, AlphaMode};
                appendUint(sb, "alpha", (EbmlUint) findFirst(video, path20));
                MKVType[] path21 = {Video, PixelWidth};
                appendUint(sb, "pixelwidth", (EbmlUint) findFirst(video, path21));
                MKVType[] path22 = {Video, PixelHeight};
                appendUint(sb, "pixelheight", (EbmlUint) findFirst(video, path22));
                MKVType[] path23 = {Video, PixelCropBottom};
                appendUint(sb, "cropbottom", (EbmlUint) findFirst(video, path23));
                MKVType[] path24 = {Video, PixelCropTop};
                appendUint(sb, "croptop", (EbmlUint) findFirst(video, path24));
                MKVType[] path25 = {Video, PixelCropLeft};
                appendUint(sb, "cropleft", (EbmlUint) findFirst(video, path25));
                MKVType[] path26 = {Video, PixelCropRight};
                appendUint(sb, "cropright", (EbmlUint) findFirst(video, path26));
                MKVType[] path27 = {Video, DisplayWidth};
                appendUint(sb, "displaywidth", (EbmlUint) findFirst(video, path27));
                MKVType[] path28 = {Video, DisplayHeight};
                appendUint(sb, "displayheight", (EbmlUint) findFirst(video, path28));
                MKVType[] path29 = {Video, DisplayUnit};
                appendUint(sb, "displayunit", (EbmlUint) findFirst(video, path29));
                MKVType[] path30 = {Video, AspectRatioType};
                appendUint(sb, "aspectratiotype", (EbmlUint) findFirst(video, path30));
            } else if (audio != null) {
                sb.append("\n    audio ");
                MKVType[] path18 = {Audio, SamplingFrequency};
                appendFloat(sb, "sampling", (EbmlFloat) findFirst(audio, path18));
                MKVType[] path19 = {Audio, OutputSamplingFrequency};
                appendFloat(sb, "outputsampling", (EbmlFloat) findFirst(audio, path19));
                MKVType[] path20 = {Audio, Channels};
                appendUint(sb, "channels", (EbmlUint) findFirst(audio, path20));
                MKVType[] path21 = {Audio, BitDepth};
                appendUint(sb, "bitdepth", (EbmlUint) findFirst(audio, path21));
            }
            sb.append("\n");
        }
        System.out.println(sb.toString());
    }

}
