package org.jcodec.containers.mkv;

import static org.jcodec.common.IOUtils.readFileToByteArray;
import static org.jcodec.containers.mkv.Type.Audio;
import static org.jcodec.containers.mkv.Type.Channels;
import static org.jcodec.containers.mkv.Type.CodecID;
import static org.jcodec.containers.mkv.Type.Cues;
import static org.jcodec.containers.mkv.Type.DisplayHeight;
import static org.jcodec.containers.mkv.Type.DisplayWidth;
import static org.jcodec.containers.mkv.Type.Duration;
import static org.jcodec.containers.mkv.Type.Info;
import static org.jcodec.containers.mkv.Type.Language;
import static org.jcodec.containers.mkv.Type.MuxingApp;
import static org.jcodec.containers.mkv.Type.Name;
import static org.jcodec.containers.mkv.Type.PixelHeight;
import static org.jcodec.containers.mkv.Type.PixelWidth;
import static org.jcodec.containers.mkv.Type.SamplingFrequency;
import static org.jcodec.containers.mkv.Type.SeekID;
import static org.jcodec.containers.mkv.Type.SeekPosition;
import static org.jcodec.containers.mkv.Type.Segment;
import static org.jcodec.containers.mkv.Type.Tags;
import static org.jcodec.containers.mkv.Type.TimecodeScale;
import static org.jcodec.containers.mkv.Type.TrackEntry;
import static org.jcodec.containers.mkv.Type.TrackNumber;
import static org.jcodec.containers.mkv.Type.TrackType;
import static org.jcodec.containers.mkv.Type.TrackUID;
import static org.jcodec.containers.mkv.Type.Tracks;
import static org.jcodec.containers.mkv.Type.WritingApp;
import static org.jcodec.containers.mkv.ebml.UnsignedIntegerElement.longToBytes;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import org.jcodec.common.IOUtils;
import org.jcodec.common.NIOUtils;
import org.jcodec.containers.mkv.SeekHeadIndexer.SeekMock;
import org.jcodec.containers.mkv.ebml.DateElement;
import org.jcodec.containers.mkv.ebml.Element;
import org.jcodec.containers.mkv.ebml.MasterElement;
import org.jcodec.containers.mkv.ebml.StringElement;
import org.jcodec.containers.mkv.ebml.UnsignedIntegerElement;
import org.junit.Assert;
import org.junit.Test;

public class SeekHeadIndexerTest {

    @Test
    public void testJCodecConvention() throws IOException {
        List<MasterElement> tree = fakeMkvTree();

        FileOutputStream fos = new FileOutputStream("src/test/resources/mkv/seek_head.ebml");
        try {
            FileChannel fc = fos.getChannel();
            for (MasterElement me : tree)
                me.mux(fc);
        } finally {
            IOUtils.closeQuietly(fos);
        }
    }

    private List<MasterElement> fakeMkvTree() {
        List<MasterElement> tree = new ArrayList<MasterElement>();
        MasterElement ebmlHeaderElem = (MasterElement) Type.createElementByType(Type.EBML);

        addFakeEbmlHeader(ebmlHeaderElem);
        tree.add(ebmlHeaderElem);
        // # Segment
        MasterElement segmentElem = (MasterElement) Type.createElementByType(Segment);


        addFakeInfo(segmentElem);

        // Tracks Info
        addFakeTracks(segmentElem);
        // Chapters
        // Attachments
        // Tags
        // Cues
        addFakeCues(segmentElem);
        // # Meta Seek
        muxSeeks(segmentElem);
        
        tree.add(segmentElem);
        return tree;
    }

    private void muxSeeks(MasterElement segmentElem) {
        SeekHeadIndexer shi = new SeekHeadIndexer();
        for (int i = 0; i < segmentElem.children.size() && i < 4; i++) {
            Element e = segmentElem.children.get(i);
            if (!e.type.equals(Tracks) && !e.type.equals(Info) && !e.type.equals(Tags) && !e.type.equals(Cues)) {
                throw new IllegalStateException("Unknown entry found among segment children, index " + i);
            }
            shi.add(e);
        }

        segmentElem.children.add(0, shi.indexSeekHead());
    }

    @Test
    public void testMuxingFromTestFileSeek() throws Exception {
        File file = new File("src/test/resources/mkv/seek_head.ebml");
        byte[] rawFrame = readFileToByteArray(file);

        MasterElement seekHead = Type.createElementByType(Type.SeekHead);

        MasterElement seek = Type.createElementByType(Type.Seek);
        MKVMuxer.createAndAddElement(seek, SeekID, Info.id);
        UnsignedIntegerElement se = (UnsignedIntegerElement) Type.createElementByType(SeekPosition);
        se.set(64);
        seek.addChildElement(se);
        seekHead.addChildElement(seek);

        seek = Type.createElementByType(Type.Seek);
        MKVMuxer.createAndAddElement(seek, SeekID, Tracks.id);
        se = (UnsignedIntegerElement) Type.createElementByType(SeekPosition);
        se.set(275);
        seek.addChildElement(se);
        seekHead.addChildElement(seek);

        seek = Type.createElementByType(Type.Seek);
        MKVMuxer.createAndAddElement(seek, SeekID, Tags.id);
        se = (UnsignedIntegerElement) Type.createElementByType(SeekPosition);
        se.set(440);
        seek.addChildElement(se);
        seekHead.addChildElement(seek);

        seek = Type.createElementByType(Type.Seek);
        MKVMuxer.createAndAddElement(seek, SeekID, Cues.id);
        se = (UnsignedIntegerElement) Type.createElementByType(SeekPosition);
        se.set(602);
        seek.addChildElement(se);
        seekHead.addChildElement(seek);

        ByteBuffer bb = seekHead.mux();
        Assert.assertArrayEquals(rawFrame, bb.array());
        Assert.assertArrayEquals(rawFrame, NIOUtils.toArray(bb));
    }

    @Test
    public void printSizeOptions() throws Exception {
        long s = SeekHeadIndexer.estimeteSeekSize(4, 1); // seek to info
        s += SeekHeadIndexer.estimeteSeekSize(4, 1);
        s += SeekHeadIndexer.estimeteSeekSize(4, 2);
        s += SeekHeadIndexer.estimeteSeekSize(4, 3);
        int z = Element.getEbmlSize(s);
        s += 4 + z;
        System.out.println("size:" + s + " can be encoded in " + z + " byte(s) using ebml, with seekhead size eq to:" + z);
    }
    
    @Test
    public void printVariousSeekSizes() throws Exception {
        System.out.println("seekSize(1): "+SeekHeadIndexer.estimeteSeekSize(4, 1));
        System.out.println("seekSize(2): "+SeekHeadIndexer.estimeteSeekSize(4, 2));
        System.out.println("seekSize(3): "+SeekHeadIndexer.estimeteSeekSize(4, 3));
    }

    public static int fakeZOffset = 0;
    public static SeekMock createFakeZ(byte id[], int size){
        SeekMock z = new SeekMock();
        z.id = id;
        z.size = size;
        z.dataOffset = fakeZOffset;
        z.seekPointerSize = longToBytes(z.dataOffset).length;
        fakeZOffset += z.size;
        System.out.println("Added id:"+Reader.printAsHex(z.id)+" offset:"+z.dataOffset+" seekpointer size:"+z.seekPointerSize);
        return z;
    }
    
    @Test
    public void testEdgeCasesWithFakeZ() throws Exception {
        SeekHeadIndexer a = new SeekHeadIndexer();
        a.a.add(createFakeZ(Info.id,   0xFF));
        a.a.add(createFakeZ(Tracks.id, 0xFF05));
        a.a.add(createFakeZ(Tags.id,   0xFEFFC0));
        a.a.add(createFakeZ(Cues.id,   0xFF));
        int computeSize = a.computeSeekHeadSize();
        System.out.println("SeekHeadSize: "+computeSize);
        Assert.assertEquals(a.estimateSize(), computeSize);
        
    }
    
    public static Element createFakeElement(byte[] id, int size){
        Element e = new Element(id);
        e.size = size;
        return e;
    }
    
    @Test
    public void testSeekHeadSize() throws Exception {
        SeekHeadIndexer a = new SeekHeadIndexer();
        a.add(createFakeElement(Info.id,   0xFF-4-2));
        a.add(createFakeElement(Tracks.id, 0xFF05-4-2));
        a.add(createFakeElement(Cues.id,   0xFEFFFF-4-3));
        int computeSize = a.computeSeekHeadSize();
        System.out.println("SeekHeadSize: "+computeSize);
        Assert.assertEquals(a.estimateSize(), computeSize);
        
    }
    
    @Test
    public void testSeekHeadMuxing() throws Exception {
        SeekHeadIndexer a = new SeekHeadIndexer();
        a.add(createFakeElement(Info.id,   0xFF-4-2));
        a.add(createFakeElement(Tracks.id, 0xFF00-4-3));
        a.add(createFakeElement(Cues.id,   0xFF0000-4-4));
        a.add(createFakeElement(Tags.id,   0xFF-4-4));
        int computeSize = a.computeSeekHeadSize();
        System.out.println("SeekHeadSize: "+computeSize);
        Assert.assertEquals(a.estimateSize(), computeSize);
        ByteBuffer mux = a.indexSeekHead().mux();
        Assert.assertEquals(a.estimateSize(), mux.limit());
        FileOutputStream fos = new FileOutputStream("src/test/resources/mkv/seek_head.ebml");
        try {
            fos.getChannel().write(mux);
        } finally {
            IOUtils.closeQuietly(fos);
        }
    }

    private void addFakeEbmlHeader(MasterElement ebmlHeaderElem) {
        StringElement docTypeElem = (StringElement) Type.createElementByType(Type.DocType);
        docTypeElem.set("matroska");

        UnsignedIntegerElement docTypeVersionElem = (UnsignedIntegerElement) Type.createElementByType(Type.DocTypeVersion);
        docTypeVersionElem.set(2);

        UnsignedIntegerElement docTypeReadVersionElem = (UnsignedIntegerElement) Type.createElementByType(Type.DocTypeReadVersion);
        docTypeReadVersionElem.set(2);

        ebmlHeaderElem.addChildElement(docTypeElem);
        ebmlHeaderElem.addChildElement(docTypeVersionElem);
        ebmlHeaderElem.addChildElement(docTypeReadVersionElem);
    }

    private void addFakeInfo(MasterElement segmentElem) {
        // # Segment Info
        MasterElement segmentInfoElem = (MasterElement) Type.createElementByType(Type.Info);

        DateElement dateElem = (DateElement) Type.createElementByType(Type.DateUTC);
        dateElem.setDate(new Date());
        segmentInfoElem.addChildElement(dateElem);

        // Add timecode scale
        MKVMuxer.createAndAddElement(segmentInfoElem, TimecodeScale, 100000);

        MKVMuxer.createAndAddElement(segmentInfoElem, Duration, 87336.0 * 1000.0);

        MKVMuxer.createAndAddElement(segmentInfoElem, WritingApp, "JCodec v0.1.0");

        MKVMuxer.createAndAddElement(segmentInfoElem, MuxingApp, "Matroska Muxer v0.1a");

        segmentElem.addChildElement(segmentInfoElem);
    }

    private void addFakeTracks(MasterElement segmentElem) {
        Random r = new Random();
        MasterElement tracksElem = (MasterElement) Type.createElementByType(Tracks);

        MasterElement trackEntryElem = (MasterElement) Type.createElementByType(TrackEntry);
        MKVMuxer.createAndAddElement(trackEntryElem, TrackNumber, 1);
        MKVMuxer.createAndAddElement(trackEntryElem, TrackUID, r.nextLong());

        MKVMuxer.createAndAddElement(trackEntryElem, TrackType, 1); // Video

        MKVMuxer.createAndAddElement(trackEntryElem, Name, "Video");

        MKVMuxer.createAndAddElement(trackEntryElem, Language, "en");

        MKVMuxer.createAndAddElement(trackEntryElem, CodecID, "V_UNCOMPRESSED");

        // Now we add the audio/video dependant sub-elements
        MasterElement trackVideoElem = (MasterElement) Type.createElementByType(Type.Video);

        MKVMuxer.createAndAddElement(trackVideoElem, PixelWidth, 1024);
        MKVMuxer.createAndAddElement(trackVideoElem, PixelHeight, 768);
        MKVMuxer.createAndAddElement(trackVideoElem, DisplayWidth, 1024);
        MKVMuxer.createAndAddElement(trackVideoElem, DisplayHeight, 768);

        trackEntryElem.addChildElement(trackVideoElem);
        tracksElem.addChildElement(trackEntryElem);

        trackEntryElem = (MasterElement) Type.createElementByType(TrackEntry);
        MKVMuxer.createAndAddElement(trackEntryElem, TrackNumber, 2);
        MKVMuxer.createAndAddElement(trackEntryElem, TrackUID, r.nextLong());

        MKVMuxer.createAndAddElement(trackEntryElem, TrackType, 2); // Video

        MKVMuxer.createAndAddElement(trackEntryElem, Name, "Audio");

        MKVMuxer.createAndAddElement(trackEntryElem, Language, "en");

        MKVMuxer.createAndAddElement(trackEntryElem, CodecID, "A_MPEG/L3");

        MasterElement trackAudioElem = (MasterElement) Type.createElementByType(Audio);

        MKVMuxer.createAndAddElement(trackAudioElem, Channels, 2);
        MKVMuxer.createAndAddElement(trackAudioElem, SamplingFrequency, 48000.0);

        trackEntryElem.addChildElement(trackAudioElem);
        tracksElem.addChildElement(trackEntryElem);

        segmentElem.addChildElement(tracksElem);

    }
    
    private void addFakeCues(MasterElement segmentElem) {
        MasterElement cuesElem = (MasterElement) Type.createElementByType(Cues);
        segmentElem.addChildElement(cuesElem);
    }

}
