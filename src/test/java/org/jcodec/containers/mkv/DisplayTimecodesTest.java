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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.jcodec.common.io.FileChannelWrapper;
import org.jcodec.containers.mkv.boxes.EbmlBase;
import org.jcodec.containers.mkv.boxes.EbmlFloat;
import org.jcodec.containers.mkv.boxes.EbmlMaster;
import org.jcodec.containers.mkv.boxes.EbmlString;
import org.jcodec.containers.mkv.boxes.EbmlUint;
import org.jcodec.containers.mkv.boxes.EbmlUlong;
import org.jcodec.containers.mkv.boxes.MkvBlock;
import org.junit.Test;

public class DisplayTimecodesTest {

    @Test
    public void test() throws IOException {
        String filename = "./src/test/resources/mkv/";
        filename += "10frames.webm";
        System.out.println("Scanning file: " + filename);
        FileInputStream iFS = new FileInputStream(new File(filename));
        MKVParser reader = new MKVParser(new FileChannelWrapper(iFS.getChannel()));
        EbmlMaster s = (EbmlMaster) findFirst(reader.parse(), Segment);
        printCues(s);
        printBlocks(s);
        printTracks(s);
        printInfo(s);
    }

    private void printInfo(EbmlMaster s) {
        StringBuilder sb = new StringBuilder("info ");
        EbmlUint scale = (EbmlUint) findFirst(s, Segment, Info, TimecodeScale);
        EbmlFloat duration = (EbmlFloat) findFirst(s, Segment, Info, Duration);
        appendUint(sb, "scale", scale);
        appendFloat(sb, "duration", duration);
        sb.append("\n");
        System.out.println(sb.toString());
    }

    private void printCues(EbmlMaster s) {
        StringBuilder sb = new StringBuilder();
        for(EbmlMaster aCuePoint : findAll(s, EbmlMaster.class, Segment, Cues, CuePoint)){
            EbmlUint time = (EbmlUint) findFirst(aCuePoint, CuePoint, CueTime);
            sb.append("cue time: ").append(time.getUint());
            for(EbmlMaster aCueTrackPosition : findAll(aCuePoint, EbmlMaster.class, CuePoint, CueTrackPositions)){
                appendUint(sb, "track", (EbmlUint) findFirst(aCueTrackPosition, CueTrackPositions, CueTrack));
                EbmlUint EbmlMaster = (EbmlUint) findFirst(aCueTrackPosition, CueTrackPositions, CueClusterPosition);
                if (EbmlMaster != null)
                    sb.append(" EbmlMaster offset ").append(EbmlMaster.getUint()+s.dataOffset);
                appendUint(sb, "block", (EbmlUint) findFirst(aCueTrackPosition, CueTrackPositions, CueBlockNumber));
            }
            sb.append("\n");
        }
        System.out.println(sb.toString());
    }
    
    public static void appendUint(StringBuilder b, String caption, EbmlUint e){
        if (e != null)
            b.append(" ").append(caption).append(": ").append(e.getUint());
    }
    
    public static void appendUlong(StringBuilder b, String caption, EbmlUlong e){
        if (e != null)
            b.append(" ").append(caption).append(": ").append(e.getUlong());
    }
    
    public static void appendString(StringBuilder b, String caption, EbmlString e){
        if (e != null)
            b.append(" ").append(caption).append(": ").append(e.getString());
    }
    
    public static void appendFloat(StringBuilder b, String caption, EbmlFloat e){
        if (e != null)
            b.append(" ").append(caption).append(": ").append(e.getDouble());
    }
    
    private void printBlocks(EbmlMaster s) {
        StringBuilder sb = new StringBuilder();
        for(EbmlMaster aEbmlMaster : findAll(s, EbmlMaster.class, Segment, Cluster)){
            EbmlUint time = (EbmlUint) findFirst(aEbmlMaster, Cluster, Timecode);
            EbmlUint position = (EbmlUint) findFirst(aEbmlMaster, Cluster, Position);
            sb.append("EbmlMaster time: ").append(time.getUint());
            appendUint(sb, "position", position);
            sb.append(" offset: ").append(aEbmlMaster.offset).append("\n");
            for(EbmlBase aChild : aEbmlMaster.children){
                if (aChild instanceof MkvBlock){
                    MkvBlock block = (MkvBlock) aChild;
                    sb.append("    block tarck: ").append(block.trackNumber).append(" timecode: ").append(block.timecode).append(" offset: ").append(block.offset).append("\n");
                    sb.append("    block real timecode: "+(time.getUint()+block.timecode));
                    sb.append("\n");
                } else if (aChild instanceof EbmlMaster){
                    MkvBlock block = (MkvBlock) findFirst((EbmlMaster) aChild, BlockGroup, Block);
                    sb.append("    block tarck: ").append(block.trackNumber).append(" timecode: ").append(block.timecode).append(" offset: ").append(block.offset).append("\n");
                    sb.append("    block real timecode: "+(time.getUint()+block.timecode));
                    
                    appendUint(sb, "reference", (EbmlUint) findFirst(aEbmlMaster, BlockGroup, ReferenceBlock));
                    appendUint(sb, "duration", (EbmlUint) findFirst(aEbmlMaster, Cluster, BlockDuration));
                    sb.append("\n");    
                }
                
            }
            sb.append("\n");
        }
        System.out.println(sb.toString());
    }
    
    private void printTracks(EbmlMaster s){
        StringBuilder sb = new StringBuilder();
        for(EbmlMaster anEntry : findAll(s, EbmlMaster.class, Segment, Tracks, TrackEntry)){
            sb.append("track ");
            appendString(sb, "name", (EbmlString) findFirst(anEntry, TrackEntry, Name));
            appendString(sb, "language", (EbmlString) findFirst(anEntry, TrackEntry, Language));
            appendUint(sb, "number", (EbmlUint) findFirst(anEntry, TrackEntry, TrackNumber));
            appendUint(sb, "type", (EbmlUint) findFirst(anEntry, TrackEntry, TrackType));
            appendUint(sb, "enabled", (EbmlUint) findFirst(anEntry, TrackEntry, FlagEnabled));
            appendUint(sb, "default", (EbmlUint) findFirst(anEntry, TrackEntry, FlagDefault));
            appendUint(sb, "forced", (EbmlUint) findFirst(anEntry, TrackEntry, FlagForced));
            appendUint(sb, "lacing", (EbmlUint) findFirst(anEntry, TrackEntry, FlagLacing));
            appendUint(sb, "mincache", (EbmlUint) findFirst(anEntry, TrackEntry, MinCache));
            appendUint(sb, "maccache", (EbmlUint) findFirst(anEntry, TrackEntry, MaxCache));
            appendUint(sb, "defaultduration", (EbmlUint) findFirst(anEntry, TrackEntry, DefaultDuration));
            appendString(sb, "codecid", (EbmlString) findFirst(anEntry, TrackEntry, CodecID));
            appendString(sb, "codecname", (EbmlString) findFirst(anEntry, TrackEntry, CodecName));
            appendString(sb, "attachmentlink", (EbmlString) findFirst(anEntry, TrackEntry, AttachmentLink));
            appendUint(sb, "codecdecodeall", (EbmlUint) findFirst(anEntry, TrackEntry, CodecDecodeAll));
            appendUint(sb, "overlay", (EbmlUint) findFirst(anEntry, TrackEntry, TrackOverlay));
            EbmlMaster video = (EbmlMaster) findFirst(anEntry, TrackEntry, Video);
            EbmlMaster audio = (EbmlMaster) findFirst(anEntry, TrackEntry, Audio);
            if (video != null){
                sb.append("\n    video ");
                appendUint(sb, "interlaced", (EbmlUint) findFirst(video, Video, FlagInterlaced));
                appendUint(sb, "stereo", (EbmlUint) findFirst(video, Video, StereoMode));
                appendUint(sb, "alpha", (EbmlUint) findFirst(video, Video, AlphaMode));
                appendUint(sb, "pixelwidth", (EbmlUint) findFirst(video, Video, PixelWidth));
                appendUint(sb, "pixelheight", (EbmlUint) findFirst(video, Video, PixelHeight));
                appendUint(sb, "cropbottom", (EbmlUint) findFirst(video, Video, PixelCropBottom));
                appendUint(sb, "croptop", (EbmlUint) findFirst(video, Video, PixelCropTop));
                appendUint(sb, "cropleft", (EbmlUint) findFirst(video, Video, PixelCropLeft));
                appendUint(sb, "cropright", (EbmlUint) findFirst(video, Video, PixelCropRight));
                appendUint(sb, "displaywidth", (EbmlUint) findFirst(video, Video, DisplayWidth));
                appendUint(sb, "displayheight", (EbmlUint) findFirst(video, Video, DisplayHeight));
                appendUint(sb, "displayunit", (EbmlUint) findFirst(video, Video, DisplayUnit));
                appendUint(sb, "aspectratiotype", (EbmlUint) findFirst(video, Video, AspectRatioType));
            } else if (audio != null){
                sb.append("\n    audio ");
                appendFloat(sb, "sampling", (EbmlFloat) findFirst(audio, Audio, SamplingFrequency));
                appendFloat(sb, "outputsampling", (EbmlFloat) findFirst(audio, Audio, OutputSamplingFrequency));
                appendUint(sb, "channels", (EbmlUint) findFirst(audio, Audio, Channels));
                appendUint(sb, "bitdepth", (EbmlUint) findFirst(audio, Audio, BitDepth));
            }
            sb.append("\n");
        }
        System.out.println(sb.toString());
    }

}
