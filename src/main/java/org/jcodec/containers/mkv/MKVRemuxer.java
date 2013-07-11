package org.jcodec.containers.mkv;

import static org.jcodec.containers.mkv.CuesIndexer.CuePointMock.make;
import static org.jcodec.containers.mkv.Type.Audio;
import static org.jcodec.containers.mkv.Type.BitDepth;
import static org.jcodec.containers.mkv.Type.Channels;
import static org.jcodec.containers.mkv.Type.Cluster;
import static org.jcodec.containers.mkv.Type.CodecID;
import static org.jcodec.containers.mkv.Type.CodecPrivate;
import static org.jcodec.containers.mkv.Type.Cues;
import static org.jcodec.containers.mkv.Type.DefaultDuration;
import static org.jcodec.containers.mkv.Type.DisplayHeight;
import static org.jcodec.containers.mkv.Type.DisplayWidth;
import static org.jcodec.containers.mkv.Type.Language;
import static org.jcodec.containers.mkv.Type.Name;
import static org.jcodec.containers.mkv.Type.OutputSamplingFrequency;
import static org.jcodec.containers.mkv.Type.PixelHeight;
import static org.jcodec.containers.mkv.Type.PixelWidth;
import static org.jcodec.containers.mkv.Type.PrevSize;
import static org.jcodec.containers.mkv.Type.SamplingFrequency;
import static org.jcodec.containers.mkv.Type.Segment;
import static org.jcodec.containers.mkv.Type.Timecode;
import static org.jcodec.containers.mkv.Type.TrackEntry;
import static org.jcodec.containers.mkv.Type.TrackNumber;
import static org.jcodec.containers.mkv.Type.TrackType;
import static org.jcodec.containers.mkv.Type.TrackUID;
import static org.jcodec.containers.mkv.Type.Tracks;
import static org.jcodec.containers.mkv.Type.Video;
import static org.jcodec.containers.mkv.Type.findFirst;
import static org.jcodec.containers.mkv.elements.BlockElement.copy;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.jcodec.containers.mkv.ebml.DateElement;
import org.jcodec.containers.mkv.ebml.Element;
import org.jcodec.containers.mkv.ebml.FloatElement;
import org.jcodec.containers.mkv.ebml.MasterElement;
import org.jcodec.containers.mkv.ebml.StringElement;
import org.jcodec.containers.mkv.ebml.UnsignedIntegerElement;
import org.jcodec.containers.mkv.elements.BlockElement;
import org.jcodec.containers.mkv.elements.Cluster;
import org.jcodec.containers.mkv.elements.Info;

public class MKVRemuxer {
    public final List<MasterElement> tree;
    private FileChannel source;

    public MKVRemuxer(List<MasterElement> tree, FileChannel source) {
        this.tree = tree;
        this.source = source;
    }

    /**
     * The JCodec convention to muxing MKV files is following
     * 
     * <pre>
     * +----------+
     * | SeekHead |
     * +----------+
     * | Info?    |
     * +----------+
     * | Tracks?  |
     * +----------+
     * | Tags?    |
     * +----------+
     * | Cues     |
     * +----------+
     * | Cluster  |
     * +----------+
     * | Cluster  |
     * +----------+
     * | ....     |
     * </pre>
     * 
     * Which stands for "Seek Head" element followed by at most one "Info" element, followed by at most one "Tracks" element, followed by at most one "Tags" element, followed by Cues element, followed by as many as needed Cluster elements
     * 
     * Also "Seek Head" contains pointers to Info, Tracks, Tags, and Cues (if correspodning element is present), but not to Cluster.
     * 
     * In its turn Cues contains pointers to every Cluster element. In case of big cluster, several CuePoints can point to a single Cluster element.
     * 
     * @param os
     * @throws IOException
     */
    public void mux(FileChannel os) throws IOException {
        MasterElement ebmlHeaderElem = (MasterElement) Type.createElementByType(Type.EBML);

        StringElement docTypeElem = (StringElement) Type.createElementByType(Type.DocType);
        docTypeElem.set("matroska");

        UnsignedIntegerElement docTypeVersionElem = (UnsignedIntegerElement) Type.createElementByType(Type.DocTypeVersion);
        docTypeVersionElem.set(2);

        UnsignedIntegerElement docTypeReadVersionElem = (UnsignedIntegerElement) Type.createElementByType(Type.DocTypeReadVersion);
        docTypeReadVersionElem.set(2);

        ebmlHeaderElem.addChildElement(docTypeElem);
        ebmlHeaderElem.addChildElement(docTypeVersionElem);
        ebmlHeaderElem.addChildElement(docTypeReadVersionElem);
        ebmlHeaderElem.mux(os);

        // # Segment
        MasterElement segmentElem = (MasterElement) Type.createElementByType(Segment);

        SeekHeadIndexer shi = new SeekHeadIndexer();
        // # Meta Seek
        // muxSeeks(segmentElem);
        MasterElement info = muxInfo();
        MasterElement tracks = muxTracks();
        MasterElement cues = (MasterElement) Type.createElementByType(Cues);
        shi.add(info);
        shi.add(tracks);
        shi.add(cues);
        MasterElement seekHead = shi.indexSeekHead();
        System.out.println("seekHead size: "+seekHead.getSize());
        System.out.println("info size: "+info.getSize());
        System.out.println("tracks size: "+tracks.getSize());

        // Tracks Info
        segmentElem.addChildElement(seekHead);
        segmentElem.addChildElement(info);
        segmentElem.addChildElement(tracks);

        long off = seekHead.getSize() + info.getSize() + tracks.getSize();
        CuesIndexer ci = new CuesIndexer(off, 1);
        for (Cluster aCluster : Type.findAll(tree, Cluster.class, Segment, Cluster)) {
            ci.add(make(aCluster));
        }
        MasterElement indexedCues = ci.createCues();
        for (Element aCuePoint : indexedCues.children)
            cues.addChildElement(aCuePoint);
        System.out.println("cues size: "+cues.getSize());
        segmentElem.addChildElement(cues);
        // Chapters
        // Attachments
        // Tags
        // Cues
        // segmentElem.addChildElement(cues);
        // Clusters
        muxClusters(segmentElem);

        segmentElem.mux(os);
    }

    private MasterElement muxInfo() {
        Info origInfo = Type.findFirst(tree, Segment, Type.Info);
        if (origInfo == null)
            throw new RuntimeException("No Info entry found in file");

        // # Segment Info
        MasterElement info = (MasterElement) Type.createElementByType(Type.Info);

        for (Element e : origInfo.children) {
            if (e.type.equals(Type.TimecodeScale)) {
                // Add timecode scale
                UnsignedIntegerElement timecodescaleElem = (UnsignedIntegerElement) Type.createElementByType(Type.TimecodeScale);
                timecodescaleElem.set(origInfo.getTimecodeScale());
                info.addChildElement(timecodescaleElem);
            } else if (e.type.equals(Type.Duration)) {

                FloatElement durationElem = (FloatElement) Type.createElementByType(Type.Duration);
                durationElem.set(origInfo.getDuration());
                info.addChildElement(durationElem);
            }
        }

        DateElement dateElem = (DateElement) Type.createElementByType(Type.DateUTC);
        dateElem.setDate(new Date());
        info.addChildElement(dateElem);

        StringElement writingAppElem = (StringElement) Type.createElementByType(Type.WritingApp);
        writingAppElem.set("JCodec v0.1.0");
        info.addChildElement(writingAppElem);

        StringElement muxingAppElem = (StringElement) Type.createElementByType(Type.MuxingApp);
        muxingAppElem.set("JCodec MKVRemuxer v0.1a");
        info.addChildElement(muxingAppElem);

        return info;

    }

    private void muxClusters(MasterElement segmentElem) throws IOException {
        for (MasterElement aCluster : Type.findAll(tree, MasterElement.class, Segment, Cluster)) {
            MasterElement clusterCopy = new MasterElement(Type.Cluster.id);

            clusterCopy.addChildElement(Type.findFirst(aCluster, Cluster, Timecode));
            clusterCopy.addChildElement(Type.findFirst(aCluster, Cluster, PrevSize));
            List<Element> blocks = new ArrayList<Element>();
            for (Element child : aCluster.children) {
                if (child.type.equals(Type.SimpleBlock)) {
                    BlockElement aBlock = (BlockElement) child;
                    BlockElement be = copy(aBlock);
                    be.readFrames(source);
                    blocks.add(be);
                } else if (child.type.equals(Type.BlockGroup)) {
                    MasterElement aBlockGroup = (MasterElement) child;
                    MasterElement bg = new MasterElement(Type.BlockGroup.id);
                    bg.type = Type.BlockGroup;
                    BlockElement aBlock = (BlockElement) Type.findFirst(aBlockGroup, Type.BlockGroup, Type.Block);
                    BlockElement be = BlockElement.copy(aBlock);
                    be.readFrames(source);
                    bg.addChildElement(be);
                    bg.addChildElement(Type.findFirst(aBlockGroup, Type.BlockGroup, Type.BlockDuration));
                    bg.addChildElement(Type.findFirst(aBlockGroup, Type.BlockGroup, Type.ReferenceBlock));
                    blocks.add(bg);
                }
            }

//            Collections.sort(blocks, new BlockComparator());
            for (Element e : blocks)
                clusterCopy.addChildElement(e);

            segmentElem.addChildElement(clusterCopy);
        }
    }

    private MasterElement muxTracks() throws IOException {
        MasterElement tracksElem = (MasterElement) Type.createElementByType(Tracks);

        for (MasterElement track : Type.findAll(tree, MasterElement.class, Segment, Tracks, TrackEntry)) {
            MasterElement trackEntryElem = (MasterElement) Type.createElementByType(TrackEntry);

            trackEntryElem.addChildElement(findFirst(track, TrackEntry, TrackNumber));

            trackEntryElem.addChildElement(findFirst(track, TrackEntry, TrackUID));

            trackEntryElem.addChildElement(findFirst(track, TrackEntry, TrackType));

            trackEntryElem.addChildElement(findFirst(track, TrackEntry, Name));

            trackEntryElem.addChildElement(findFirst(track, TrackEntry, Language));

            trackEntryElem.addChildElement(findFirst(track, TrackEntry, CodecID));

            trackEntryElem.addChildElement(findFirst(track, TrackEntry, CodecPrivate));

            trackEntryElem.addChildElement(findFirst(track, TrackEntry, DefaultDuration));

            Element video = findFirst(track, TrackEntry, Video);
            Element audio = findFirst(track, TrackEntry, Audio);
            // Now we add the audio/video dependant sub-elements
            if (video != null) {
                MasterElement trackVideoElem = (MasterElement) Type.createElementByType(Type.Video);

                trackVideoElem.addChildElement(findFirst(track, TrackEntry, Video, PixelWidth));
                trackVideoElem.addChildElement(findFirst(track, TrackEntry, Video, PixelHeight));
                trackVideoElem.addChildElement(findFirst(track, TrackEntry, Video, DisplayWidth));
                trackVideoElem.addChildElement(findFirst(track, TrackEntry, Video, DisplayHeight));

                trackEntryElem.addChildElement(trackVideoElem);
            } else if (audio != null) {
                MasterElement trackAudioElem = (MasterElement) Type.createElementByType(Audio);

                trackAudioElem.addChildElement(findFirst(track, TrackEntry, Audio, Channels));
                trackAudioElem.addChildElement(findFirst(track, TrackEntry, Audio, BitDepth));
                trackAudioElem.addChildElement(findFirst(track, TrackEntry, Audio, SamplingFrequency));
                trackAudioElem.addChildElement(findFirst(track, TrackEntry, Audio, OutputSamplingFrequency));

                trackEntryElem.addChildElement(trackAudioElem);
            }

            tracksElem.addChildElement(trackEntryElem);
        }

        return tracksElem;

    }

    public static class BlockComparator implements Comparator<Element> {

        @Override
        public int compare(Element o1, Element o2) {
            int timecode1, timecode2;
            if (o1 instanceof BlockElement)
                timecode1 = ((BlockElement) o1).timecode;
            else if (Type.BlockGroup.equals(o1.type))
                timecode1 = ((BlockElement) Type.findFirst(o1, Type.BlockGroup, Type.Block)).timecode;
            else
                throw new IllegalArgumentException("Block comparator works on blocks or block groups only, first argument " + o1.getClass().getName() + " of type " + o1.type + " provided instead.");

            if (o2 instanceof BlockElement)
                timecode2 = ((BlockElement) o2).timecode;
            else if (Type.BlockGroup.equals(o2.type))
                timecode2 = ((BlockElement) Type.findFirst(o2, Type.BlockGroup, Type.Block)).timecode;
            else
                throw new IllegalArgumentException("Block comparator works on blocks or block groups only, but " + o2.getClass().getName() + " of type " + o2.type + " provided instead.");

            return timecode1 - timecode2;
        }

    }
}
