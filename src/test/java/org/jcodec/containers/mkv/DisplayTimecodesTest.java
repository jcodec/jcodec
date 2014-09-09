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

import org.jcodec.common.FileChannelWrapper;
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
        appendIfExists(sb, "scale", scale);
        appendIfExists(sb, "duration", duration);
        sb.append("\n");
        System.out.println(sb.toString());
    }

    private void printCues(EbmlMaster s) {
        StringBuilder sb = new StringBuilder();
        for(EbmlMaster aCuePoint : findAll(s, EbmlMaster.class, Segment, Cues, CuePoint)){
            EbmlUint time = (EbmlUint) findFirst(aCuePoint, CuePoint, CueTime);
            sb.append("cue time: ").append(time.get());
            for(EbmlMaster aCueTrackPosition : findAll(aCuePoint, EbmlMaster.class, CuePoint, CueTrackPositions)){
                appendIfExists(sb, "track", (EbmlUint) findFirst(aCueTrackPosition, CueTrackPositions, CueTrack));
                EbmlUint EbmlMaster = (EbmlUint) findFirst(aCueTrackPosition, CueTrackPositions, CueClusterPosition);
                if (EbmlMaster != null)
                    sb.append(" EbmlMaster offset ").append(EbmlMaster.get()+s.dataOffset);
                appendIfExists(sb, "block", (EbmlUint) findFirst(aCueTrackPosition, CueTrackPositions, CueBlockNumber));
            }
            sb.append("\n");
        }
        System.out.println(sb.toString());
    }
    
    public static void appendIfExists(StringBuilder b, String caption, EbmlUint e){
        if (e != null)
            b.append(" ").append(caption).append(": ").append(e.get());
    }
    
    public static void appendIfExists(StringBuilder b, String caption, EbmlUlong e){
        if (e != null)
            b.append(" ").append(caption).append(": ").append(e.get());
    }
    
    public static void appendIfExists(StringBuilder b, String caption, EbmlString e){
        if (e != null)
            b.append(" ").append(caption).append(": ").append(e.get());
    }
    
    public static void appendIfExists(StringBuilder b, String caption, EbmlFloat e){
        if (e != null)
            b.append(" ").append(caption).append(": ").append(e.get());
    }
    
    private void printBlocks(EbmlMaster s) {
        StringBuilder sb = new StringBuilder();
        for(EbmlMaster aEbmlMaster : findAll(s, EbmlMaster.class, Segment, Cluster)){
            EbmlUint time = (EbmlUint) findFirst(aEbmlMaster, Cluster, Timecode);
            EbmlUint position = (EbmlUint) findFirst(aEbmlMaster, Cluster, Position);
            sb.append("EbmlMaster time: ").append(time.get());
            appendIfExists(sb, "position", position);
            sb.append(" offset: ").append(aEbmlMaster.offset).append("\n");
            for(EbmlBase aChild : aEbmlMaster.children){
                if (aChild instanceof MkvBlock){
                    MkvBlock block = (MkvBlock) aChild;
                    sb.append("    block tarck: ").append(block.trackNumber).append(" timecode: ").append(block.timecode).append(" offset: ").append(block.offset).append("\n");
                    sb.append("    block real timecode: "+(time.get()+block.timecode));
                    sb.append("\n");
                } else if (aChild instanceof EbmlMaster){
                    MkvBlock block = (MkvBlock) findFirst((EbmlMaster) aChild, BlockGroup, Block);
                    sb.append("    block tarck: ").append(block.trackNumber).append(" timecode: ").append(block.timecode).append(" offset: ").append(block.offset).append("\n");
                    sb.append("    block real timecode: "+(time.get()+block.timecode));
                    
                    appendIfExists(sb, "reference", (EbmlUint) findFirst(aEbmlMaster, BlockGroup, ReferenceBlock));
                    appendIfExists(sb, "duration", (EbmlUint) findFirst(aEbmlMaster, Cluster, BlockDuration));
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
            appendIfExists(sb, "name", (EbmlString) findFirst(anEntry, TrackEntry, Name));
            appendIfExists(sb, "language", (EbmlString) findFirst(anEntry, TrackEntry, Language));
            appendIfExists(sb, "number", (EbmlUint) findFirst(anEntry, TrackEntry, TrackNumber));
            appendIfExists(sb, "type", (EbmlUint) findFirst(anEntry, TrackEntry, TrackType));
            appendIfExists(sb, "enabled", (EbmlUint) findFirst(anEntry, TrackEntry, FlagEnabled));
            appendIfExists(sb, "default", (EbmlUint) findFirst(anEntry, TrackEntry, FlagDefault));
            appendIfExists(sb, "forced", (EbmlUint) findFirst(anEntry, TrackEntry, FlagForced));
            appendIfExists(sb, "lacing", (EbmlUint) findFirst(anEntry, TrackEntry, FlagLacing));
            appendIfExists(sb, "mincache", (EbmlUint) findFirst(anEntry, TrackEntry, MinCache));
            appendIfExists(sb, "maccache", (EbmlUint) findFirst(anEntry, TrackEntry, MaxCache));
            appendIfExists(sb, "defaultduration", (EbmlUint) findFirst(anEntry, TrackEntry, DefaultDuration));
            appendIfExists(sb, "codecid", (EbmlString) findFirst(anEntry, TrackEntry, CodecID));
            appendIfExists(sb, "codecname", (EbmlString) findFirst(anEntry, TrackEntry, CodecName));
            appendIfExists(sb, "attachmentlink", (EbmlString) findFirst(anEntry, TrackEntry, AttachmentLink));
            appendIfExists(sb, "codecdecodeall", (EbmlUint) findFirst(anEntry, TrackEntry, CodecDecodeAll));
            appendIfExists(sb, "overlay", (EbmlUint) findFirst(anEntry, TrackEntry, TrackOverlay));
            EbmlMaster video = (EbmlMaster) findFirst(anEntry, TrackEntry, Video);
            EbmlMaster audio = (EbmlMaster) findFirst(anEntry, TrackEntry, Audio);
            if (video != null){
                sb.append("\n    video ");
                appendIfExists(sb, "interlaced", (EbmlUint) findFirst(video, Video, FlagInterlaced));
                appendIfExists(sb, "stereo", (EbmlUint) findFirst(video, Video, StereoMode));
                appendIfExists(sb, "alpha", (EbmlUint) findFirst(video, Video, AlphaMode));
                appendIfExists(sb, "pixelwidth", (EbmlUint) findFirst(video, Video, PixelWidth));
                appendIfExists(sb, "pixelheight", (EbmlUint) findFirst(video, Video, PixelHeight));
                appendIfExists(sb, "cropbottom", (EbmlUint) findFirst(video, Video, PixelCropBottom));
                appendIfExists(sb, "croptop", (EbmlUint) findFirst(video, Video, PixelCropTop));
                appendIfExists(sb, "cropleft", (EbmlUint) findFirst(video, Video, PixelCropLeft));
                appendIfExists(sb, "cropright", (EbmlUint) findFirst(video, Video, PixelCropRight));
                appendIfExists(sb, "displaywidth", (EbmlUint) findFirst(video, Video, DisplayWidth));
                appendIfExists(sb, "displayheight", (EbmlUint) findFirst(video, Video, DisplayHeight));
                appendIfExists(sb, "displayunit", (EbmlUint) findFirst(video, Video, DisplayUnit));
                appendIfExists(sb, "aspectratiotype", (EbmlUint) findFirst(video, Video, AspectRatioType));
            } else if (audio != null){
                sb.append("\n    audio ");
                appendIfExists(sb, "sampling", (EbmlFloat) findFirst(audio, Audio, SamplingFrequency));
                appendIfExists(sb, "outputsampling", (EbmlFloat) findFirst(audio, Audio, OutputSamplingFrequency));
                appendIfExists(sb, "channels", (EbmlUint) findFirst(audio, Audio, Channels));
                appendIfExists(sb, "bitdepth", (EbmlUint) findFirst(audio, Audio, BitDepth));
            }
            sb.append("\n");
        }
        System.out.println(sb.toString());
    }

}
