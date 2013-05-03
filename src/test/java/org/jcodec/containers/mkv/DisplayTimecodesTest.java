package org.jcodec.containers.mkv;

import static org.jcodec.containers.mkv.Type.AttachmentLink;
import static org.jcodec.containers.mkv.Type.Audio;
import static org.jcodec.containers.mkv.Type.BitDepth;
import static org.jcodec.containers.mkv.Type.Block;
import static org.jcodec.containers.mkv.Type.BlockDuration;
import static org.jcodec.containers.mkv.Type.BlockGroup;
import static org.jcodec.containers.mkv.Type.Channels;
import static org.jcodec.containers.mkv.Type.Cluster;
import static org.jcodec.containers.mkv.Type.CodecDecodeAll;
import static org.jcodec.containers.mkv.Type.CodecID;
import static org.jcodec.containers.mkv.Type.CodecName;
import static org.jcodec.containers.mkv.Type.CuePoint;
import static org.jcodec.containers.mkv.Type.CueTime;
import static org.jcodec.containers.mkv.Type.CueTrackPositions;
import static org.jcodec.containers.mkv.Type.Cues;
import static org.jcodec.containers.mkv.Type.DefaultDuration;
import static org.jcodec.containers.mkv.Type.Duration;
import static org.jcodec.containers.mkv.Type.FlagDefault;
import static org.jcodec.containers.mkv.Type.FlagEnabled;
import static org.jcodec.containers.mkv.Type.FlagForced;
import static org.jcodec.containers.mkv.Type.FlagInterlaced;
import static org.jcodec.containers.mkv.Type.FlagLacing;
import static org.jcodec.containers.mkv.Type.Info;
import static org.jcodec.containers.mkv.Type.Language;
import static org.jcodec.containers.mkv.Type.MaxCache;
import static org.jcodec.containers.mkv.Type.MinCache;
import static org.jcodec.containers.mkv.Type.Name;
import static org.jcodec.containers.mkv.Type.OutputSamplingFrequency;
import static org.jcodec.containers.mkv.Type.Position;
import static org.jcodec.containers.mkv.Type.ReferenceBlock;
import static org.jcodec.containers.mkv.Type.SamplingFrequency;
import static org.jcodec.containers.mkv.Type.Segment;
import static org.jcodec.containers.mkv.Type.Timecode;
import static org.jcodec.containers.mkv.Type.TimecodeScale;
import static org.jcodec.containers.mkv.Type.TrackEntry;
import static org.jcodec.containers.mkv.Type.TrackNumber;
import static org.jcodec.containers.mkv.Type.TrackOverlay;
import static org.jcodec.containers.mkv.Type.TrackType;
import static org.jcodec.containers.mkv.Type.Tracks;
import static org.jcodec.containers.mkv.Type.Video;
import static org.jcodec.containers.mkv.Type.findAll;
import static org.jcodec.containers.mkv.Type.findFirst;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.jcodec.containers.mkv.ebml.Element;
import org.jcodec.containers.mkv.ebml.FloatElement;
import org.jcodec.containers.mkv.ebml.MasterElement;
import org.jcodec.containers.mkv.ebml.StringElement;
import org.jcodec.containers.mkv.ebml.UnsignedIntegerElement;
import org.jcodec.containers.mkv.elements.BlockElement;
import org.jcodec.containers.mkv.elements.Cluster;
import org.jcodec.containers.mkv.elements.CuePoint;
import org.jcodec.containers.mkv.elements.TrackEntryElement;
import org.junit.Test;

public class DisplayTimecodesTest {

    @Test
    public void test() throws IOException {
        String filename = "./src/test/resources/mkv/";
        filename += "10frames.webm";
        System.out.println("Scanning file: " + filename);
        FileInputStream iFS = new FileInputStream(new File(filename));
        SimpleEBMLParser reader = new SimpleEBMLParser(iFS.getChannel());
        reader.parse();
        MasterElement s = (MasterElement) findFirst(reader.getTree(), Segment);
        printCues(s);
        printBlocks(s);
        printTracks(s);
        printInfo(s);
    }

    private void printInfo(MasterElement s) {
        StringBuilder sb = new StringBuilder("info ");
        UnsignedIntegerElement scale = (UnsignedIntegerElement) findFirst(s, Segment, Info, TimecodeScale);
        FloatElement duration = (FloatElement) findFirst(s, Segment, Info, Duration);
        appendIfExists(sb, "scale", scale);
        appendIfExists(sb, "duration", duration);
        sb.append("\n");
        System.out.println(sb.toString());
    }

    private void printCues(MasterElement s) {
        StringBuilder sb = new StringBuilder();
        for(CuePoint aCuePoint : findAll(s, CuePoint.class, Segment, Cues, CuePoint)){
            UnsignedIntegerElement time = (UnsignedIntegerElement) findFirst(aCuePoint, CuePoint, CueTime);
            sb.append("cue time: ").append(time.get());
            for(MasterElement aCueTrackPosition : findAll(aCuePoint, MasterElement.class, CuePoint, CueTrackPositions)){
                appendIfExists(sb, "track", (UnsignedIntegerElement) findFirst(aCueTrackPosition, CueTrackPositions, Type.CueTrack));
                UnsignedIntegerElement cluster = (UnsignedIntegerElement) findFirst(aCueTrackPosition, CueTrackPositions, Type.CueClusterPosition);
                if (cluster != null)
                    sb.append(" cluster offset ").append(cluster.get()+s.dataOffset);
                appendIfExists(sb, "block", (UnsignedIntegerElement) findFirst(aCueTrackPosition, CueTrackPositions, Type.CueBlockNumber));
            }
            sb.append("\n");
        }
        System.out.println(sb.toString());
    }
    
    public static void appendIfExists(StringBuilder b, String caption, UnsignedIntegerElement e){
        if (e != null)
            b.append(" ").append(caption).append(": ").append(e.get());
    }
    
    public static void appendIfExists(StringBuilder b, String caption, StringElement e){
        if (e != null)
            b.append(" ").append(caption).append(": ").append(e.get());
    }
    
    public static void appendIfExists(StringBuilder b, String caption, FloatElement e){
        if (e != null)
            b.append(" ").append(caption).append(": ").append(e.get());
    }
    
    private void printBlocks(MasterElement s) {
        StringBuilder sb = new StringBuilder();
        for(Cluster aCluster : findAll(s, Cluster.class, Segment, Cluster)){
            UnsignedIntegerElement time = (UnsignedIntegerElement) findFirst(aCluster, Cluster, Timecode);
            UnsignedIntegerElement position = (UnsignedIntegerElement) findFirst(aCluster, Cluster, Position);
            sb.append("cluster time: ").append(time.get());
            appendIfExists(sb, "position", position);
            sb.append(" offset: ").append(aCluster.offset).append("\n");
            for(Element aChild : aCluster.children){
                if (aChild instanceof BlockElement){
                    BlockElement block = (BlockElement) aChild;
                    sb.append("    block tarck: ").append(block.trackNumber).append(" timecode: ").append(block.timecode).append(" offset: ").append(block.offset).append("\n");
                    sb.append("    block real timecode: "+(time.get()+block.timecode));
                    sb.append("\n");
                } else if (aChild instanceof MasterElement){
                    BlockElement block = (BlockElement) findFirst((MasterElement) aChild, BlockGroup, Block);
                    sb.append("    block tarck: ").append(block.trackNumber).append(" timecode: ").append(block.timecode).append(" offset: ").append(block.offset).append("\n");
                    sb.append("    block real timecode: "+(time.get()+block.timecode));
                    
                    appendIfExists(sb, "reference", (UnsignedIntegerElement) findFirst(aCluster, BlockGroup, ReferenceBlock));
                    appendIfExists(sb, "duration", (UnsignedIntegerElement) findFirst(aCluster, Cluster, BlockDuration));
                    sb.append("\n");    
                }
                
            }
            sb.append("\n");
        }
        System.out.println(sb.toString());
    }
    
    private void printTracks(MasterElement s){
        StringBuilder sb = new StringBuilder();
        for(TrackEntryElement anEntry : findAll(s, TrackEntryElement.class, Segment, Tracks, TrackEntry)){
            sb.append("track ");
            appendIfExists(sb, "name", (StringElement) findFirst(anEntry, TrackEntry, Name));
            appendIfExists(sb, "language", (StringElement) findFirst(anEntry, TrackEntry, Language));
            appendIfExists(sb, "number", (UnsignedIntegerElement) findFirst(anEntry, TrackEntry, TrackNumber));
            appendIfExists(sb, "type", (UnsignedIntegerElement) findFirst(anEntry, TrackEntry, TrackType));
            appendIfExists(sb, "enabled", (UnsignedIntegerElement) findFirst(anEntry, TrackEntry, FlagEnabled));
            appendIfExists(sb, "default", (UnsignedIntegerElement) findFirst(anEntry, TrackEntry, FlagDefault));
            appendIfExists(sb, "forced", (UnsignedIntegerElement) findFirst(anEntry, TrackEntry, FlagForced));
            appendIfExists(sb, "lacing", (UnsignedIntegerElement) findFirst(anEntry, TrackEntry, FlagLacing));
            appendIfExists(sb, "mincache", (UnsignedIntegerElement) findFirst(anEntry, TrackEntry, MinCache));
            appendIfExists(sb, "maccache", (UnsignedIntegerElement) findFirst(anEntry, TrackEntry, MaxCache));
            appendIfExists(sb, "defaultduration", (UnsignedIntegerElement) findFirst(anEntry, TrackEntry, DefaultDuration));
            appendIfExists(sb, "codecid", (StringElement) findFirst(anEntry, TrackEntry, CodecID));
            appendIfExists(sb, "codecname", (StringElement) findFirst(anEntry, TrackEntry, CodecName));
            appendIfExists(sb, "attachmentlink", (StringElement) findFirst(anEntry, TrackEntry, AttachmentLink));
            appendIfExists(sb, "codecdecodeall", (UnsignedIntegerElement) findFirst(anEntry, TrackEntry, CodecDecodeAll));
            appendIfExists(sb, "overlay", (UnsignedIntegerElement) findFirst(anEntry, TrackEntry, TrackOverlay));
            MasterElement video = (MasterElement) findFirst(anEntry, TrackEntry, Video);
            MasterElement audio = (MasterElement) findFirst(anEntry, TrackEntry, Audio);
            if (video != null){
                sb.append("\n    video ");
                appendIfExists(sb, "interlaced", (UnsignedIntegerElement) findFirst(video, Video, FlagInterlaced));
                appendIfExists(sb, "stereo", (UnsignedIntegerElement) findFirst(video, Video, Type.StereoMode));
                appendIfExists(sb, "alpha", (UnsignedIntegerElement) findFirst(video, Video, Type.AlphaMode));
                appendIfExists(sb, "pixelwidth", (UnsignedIntegerElement) findFirst(video, Video, Type.PixelWidth));
                appendIfExists(sb, "pixelheight", (UnsignedIntegerElement) findFirst(video, Video, Type.PixelHeight));
                appendIfExists(sb, "cropbottom", (UnsignedIntegerElement) findFirst(video, Video, Type.PixelCropBottom));
                appendIfExists(sb, "croptop", (UnsignedIntegerElement) findFirst(video, Video, Type.PixelCropTop));
                appendIfExists(sb, "cropleft", (UnsignedIntegerElement) findFirst(video, Video, Type.PixelCropLeft));
                appendIfExists(sb, "cropright", (UnsignedIntegerElement) findFirst(video, Video, Type.PixelCropRight));
                appendIfExists(sb, "displaywidth", (UnsignedIntegerElement) findFirst(video, Video, Type.DisplayWidth));
                appendIfExists(sb, "displayheight", (UnsignedIntegerElement) findFirst(video, Video, Type.DisplayHeight));
                appendIfExists(sb, "displayunit", (UnsignedIntegerElement) findFirst(video, Video, Type.DisplayUnit));
                appendIfExists(sb, "aspectratiotype", (UnsignedIntegerElement) findFirst(video, Video, Type.AspectRatioType));
            } else if (audio != null){
                sb.append("\n    audio ");
                appendIfExists(sb, "sampling", (FloatElement) findFirst(audio, Audio, SamplingFrequency));
                appendIfExists(sb, "outputsampling", (FloatElement) findFirst(audio, Audio, OutputSamplingFrequency));
                appendIfExists(sb, "channels", (UnsignedIntegerElement) findFirst(audio, Audio, Channels));
                appendIfExists(sb, "bitdepth", (UnsignedIntegerElement) findFirst(audio, Audio, BitDepth));
            }
            sb.append("\n");
        }
        System.out.println(sb.toString());
    }

}
