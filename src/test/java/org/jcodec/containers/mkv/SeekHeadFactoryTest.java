package org.jcodec.containers.mkv;
import static org.jcodec.common.io.IOUtils.readFileToByteArray;
import static org.jcodec.containers.mkv.MKVType.Audio;
import static org.jcodec.containers.mkv.MKVType.Channels;
import static org.jcodec.containers.mkv.MKVType.CodecID;
import static org.jcodec.containers.mkv.MKVType.Cues;
import static org.jcodec.containers.mkv.MKVType.DisplayHeight;
import static org.jcodec.containers.mkv.MKVType.DisplayWidth;
import static org.jcodec.containers.mkv.MKVType.Duration;
import static org.jcodec.containers.mkv.MKVType.Info;
import static org.jcodec.containers.mkv.MKVType.Language;
import static org.jcodec.containers.mkv.MKVType.MuxingApp;
import static org.jcodec.containers.mkv.MKVType.Name;
import static org.jcodec.containers.mkv.MKVType.PixelHeight;
import static org.jcodec.containers.mkv.MKVType.PixelWidth;
import static org.jcodec.containers.mkv.MKVType.SamplingFrequency;
import static org.jcodec.containers.mkv.MKVType.SeekID;
import static org.jcodec.containers.mkv.MKVType.SeekPosition;
import static org.jcodec.containers.mkv.MKVType.Segment;
import static org.jcodec.containers.mkv.MKVType.Tags;
import static org.jcodec.containers.mkv.MKVType.TimecodeScale;
import static org.jcodec.containers.mkv.MKVType.TrackEntry;
import static org.jcodec.containers.mkv.MKVType.TrackNumber;
import static org.jcodec.containers.mkv.MKVType.TrackType;
import static org.jcodec.containers.mkv.MKVType.TrackUID;
import static org.jcodec.containers.mkv.MKVType.Tracks;
import static org.jcodec.containers.mkv.MKVType.WritingApp;
import static org.jcodec.containers.mkv.SeekHeadFactory.estimeteSeekSize;
import static org.jcodec.containers.mkv.boxes.EbmlUint.longToBytes;
import static org.jcodec.containers.mkv.muxer.MKVMuxer.createBuffer;
import static org.jcodec.containers.mkv.muxer.MKVMuxer.createDouble;
import static org.jcodec.containers.mkv.muxer.MKVMuxer.createLong;
import static org.jcodec.containers.mkv.muxer.MKVMuxer.createString;
import static org.junit.Assert.assertEquals;

import org.jcodec.api.UnhandledStateException;
import org.jcodec.common.io.FileChannelWrapper;
import org.jcodec.common.io.IOUtils;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.containers.mkv.SeekHeadFactory.SeekMock;
import org.jcodec.containers.mkv.boxes.EbmlBase;
import org.jcodec.containers.mkv.boxes.EbmlBin;
import org.jcodec.containers.mkv.boxes.EbmlDate;
import org.jcodec.containers.mkv.boxes.EbmlMaster;
import org.jcodec.containers.mkv.boxes.EbmlString;
import org.jcodec.containers.mkv.boxes.EbmlUint;
import org.jcodec.containers.mkv.muxer.MKVMuxer;
import org.jcodec.containers.mkv.util.EbmlUtil;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.System;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

public class SeekHeadFactoryTest {

    @Test
    public void testJCodecConvention() throws IOException {
        List<EbmlMaster> tree = fakeMkvTree();

        FileOutputStream fos = new FileOutputStream("src/test/resources/mkv/seek_head.ebml");
        try {
            SeekableByteChannel fc = new FileChannelWrapper(fos.getChannel());
            for (EbmlMaster me : tree)
                me.mux(fc);
        } finally {
            IOUtils.closeQuietly(fos);
        }
    }

    private List<EbmlMaster> fakeMkvTree() {
        List<EbmlMaster> tree = new ArrayList<EbmlMaster>();
        EbmlMaster ebmlHeaderElem = (EbmlMaster) MKVType.createByType(MKVType.EBML);

        addFakeEbmlHeader(ebmlHeaderElem);
        tree.add(ebmlHeaderElem);
        // # Segment
        EbmlMaster segmentElem = (EbmlMaster) MKVType.createByType(Segment);


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

    private void muxSeeks(EbmlMaster segmentElem) {
        SeekHeadFactory shi = new SeekHeadFactory();
        for (int i = 0; i < segmentElem.children.size() && i < 4; i++) {
            EbmlBase e = segmentElem.children.get(i);
            if (!e.type.equals(Tracks) && !e.type.equals(Info) && !e.type.equals(Tags) && !e.type.equals(Cues)) {
                throw new UnhandledStateException("Unknown entry found among segment children, index " + i);
            }
            shi.add(e);
        }

        segmentElem.children.add(0, shi.indexSeekHead());
    }

    public void _testMuxingFromTestFileSeek() throws Exception {
        File file = new File("src/test/resources/mkv/seek_head.ebml");
        byte[] rawFrame = readFileToByteArray(file);

        EbmlMaster seekHead = MKVType.createByType(MKVType.SeekHead);

        EbmlMaster seek = MKVType.createByType(MKVType.Seek);
        createBuffer(seek, SeekID, ByteBuffer.wrap(Info.id));
        EbmlUint se = (EbmlUint) MKVType.createByType(SeekPosition);
        se.setUint(64);
        seek.add(se);
        seekHead.add(seek);

        seek = MKVType.createByType(MKVType.Seek);
        createBuffer(seek, SeekID, ByteBuffer.wrap(Tracks.id));
        se = (EbmlUint) MKVType.createByType(SeekPosition);
        se.setUint(275);
        seek.add(se);
        seekHead.add(seek);

        seek = MKVType.createByType(MKVType.Seek);
        createBuffer(seek, SeekID, ByteBuffer.wrap(Tags.id));
        se = (EbmlUint) MKVType.createByType(SeekPosition);
        se.setUint(440);
        seek.add(se);
        seekHead.add(seek);

        seek = MKVType.createByType(MKVType.Seek);
        createBuffer(seek, SeekID, ByteBuffer.wrap(Cues.id));
        se = (EbmlUint) MKVType.createByType(SeekPosition);
        se.setUint(602);
        seek.add(se);
        seekHead.add(seek);

        ByteBuffer bb = seekHead.getData();
        Assert.assertArrayEquals(rawFrame, bb.array());
        Assert.assertArrayEquals(rawFrame, NIOUtils.toArray(bb));
    }

    @Test
    public void testPrintSizeOptions() throws Exception {
        long s = estimeteSeekSize(4, 1); // seek to info
        s += estimeteSeekSize(4, 1);
        s += estimeteSeekSize(4, 2);
        s += estimeteSeekSize(4, 3);
        int z = EbmlUtil.ebmlLength(s);
        s += 4 + z;
        System.out.println("size:" + s + " can be encoded in " + z + " byte(s) using ebml, with seekhead size eq to:" + z);
    }
    
    @Test
    public void testEstimeteSeekSize() throws Exception {
        assertEquals(14, estimeteSeekSize(4, 1));
        assertEquals(15, estimeteSeekSize(4, 2));
        assertEquals(16, estimeteSeekSize(4, 3));
    }

    public static int fakeZOffset = 0;
    public static SeekMock createFakeZ(byte id[], int size){
        SeekMock z = new SeekMock();
        z.id = id;
        z.size = size;
        z.dataOffset = fakeZOffset;
        z.seekPointerSize = longToBytes(z.dataOffset).length;
        fakeZOffset += z.size;
        System.out.println("Added id:"+EbmlUtil.toHexString(z.id)+" offset:"+z.dataOffset+" seekpointer size:"+z.seekPointerSize);
        return z;
    }
    
    @Test
    public void testEdgeCasesWithFakeZ() throws Exception {
        SeekHeadFactory a = new SeekHeadFactory();
        a.a.add(createFakeZ(Info.id,   0xFF));
        a.a.add(createFakeZ(Tracks.id, 0xFF05));
        a.a.add(createFakeZ(Tags.id,   0xFEFFC0));
        a.a.add(createFakeZ(Cues.id,   0xFF));
        int computeSize = a.computeSeekHeadSize();
        System.out.println("SeekHeadSize: "+computeSize);
        assertEquals(a.estimateSize(), computeSize);
        
    }
    
    public static EbmlBase createFakeElement(byte[] id, int size){
        EbmlBase e = new EbmlBin(id);
        e.dataLen = size;
        return e;
    }
    
    @Test
    public void testSeekHeadSize() throws Exception {
        SeekHeadFactory a = new SeekHeadFactory();
        a.add(createFakeElement(Info.id,   0xFF-4-2));
        a.add(createFakeElement(Tracks.id, 0xFF05-4-2));
        a.add(createFakeElement(Cues.id,   0xFEFFFF-4-3));
        int computeSize = a.computeSeekHeadSize();
        System.out.println("SeekHeadSize: "+computeSize);
        assertEquals(a.estimateSize(), computeSize);
        
    }
    
    @Test
    public void testSeekHeadMuxing() throws Exception {
        SeekHeadFactory a = new SeekHeadFactory();
        a.add(createFakeElement(Info.id,   0xFF-4-2));
        a.add(createFakeElement(Tracks.id, 0xFF00-4-3));
        a.add(createFakeElement(Cues.id,   0xFF0000-4-4));
        a.add(createFakeElement(Tags.id,   0xFF-4-4));
        int computeSize = a.computeSeekHeadSize();
        System.out.println("SeekHeadSize: "+computeSize);
        assertEquals(a.estimateSize(), computeSize);
        ByteBuffer mux = a.indexSeekHead().getData();
        assertEquals(a.estimateSize(), mux.limit());
        FileOutputStream fos = new FileOutputStream("src/test/resources/mkv/seek_head.ebml");
        try {
            fos.getChannel().write(mux);
        } finally {
            IOUtils.closeQuietly(fos);
        }
    }

    private void addFakeEbmlHeader(EbmlMaster ebmlHeaderElem) {
        EbmlString docTypeElem = (EbmlString) MKVType.createByType(MKVType.DocType);
        docTypeElem.setString("matroska");

        EbmlUint docTypeVersionElem = (EbmlUint) MKVType.createByType(MKVType.DocTypeVersion);
        docTypeVersionElem.setUint(2);

        EbmlUint docTypeReadVersionElem = (EbmlUint) MKVType.createByType(MKVType.DocTypeReadVersion);
        docTypeReadVersionElem.setUint(2);

        ebmlHeaderElem.add(docTypeElem);
        ebmlHeaderElem.add(docTypeVersionElem);
        ebmlHeaderElem.add(docTypeReadVersionElem);
    }

    private void addFakeInfo(EbmlMaster segmentElem) {
        // # Segment Info
        EbmlMaster segmentInfoElem = (EbmlMaster) MKVType.createByType(MKVType.Info);

        EbmlDate dateElem = (EbmlDate) MKVType.createByType(MKVType.DateUTC);
        dateElem.setDate(new Date());
        segmentInfoElem.add(dateElem);

        // Add timecode scale
        createLong(segmentInfoElem, TimecodeScale, 100000);

        createDouble(segmentInfoElem, Duration, 87336.0 * 1000.0);

        createString(segmentInfoElem, WritingApp, "JCodec v0.1.0");

        createString(segmentInfoElem, MuxingApp, "Matroska Muxer v0.1a");

        segmentElem.add(segmentInfoElem);
    }

    private void addFakeTracks(EbmlMaster segmentElem) {
        Random r = new Random();
        EbmlMaster tracksElem = (EbmlMaster) MKVType.createByType(Tracks);

        EbmlMaster trackEntryElem = (EbmlMaster) MKVType.createByType(TrackEntry);
        createLong(trackEntryElem, TrackNumber, 1);
        createLong(trackEntryElem, TrackUID, r.nextLong());

        createLong(trackEntryElem, TrackType, 1); // Video

        createString(trackEntryElem, Name, "Video");

        createString(trackEntryElem, Language, "en");

        createString(trackEntryElem, CodecID, "V_UNCOMPRESSED");

        // Now we add the audio/video dependant sub-elements
        EbmlMaster trackVideoElem = (EbmlMaster) MKVType.createByType(MKVType.Video);

        createLong(trackVideoElem, PixelWidth, 1024);
        createLong(trackVideoElem, PixelHeight, 768);
        createLong(trackVideoElem, DisplayWidth, 1024);
        createLong(trackVideoElem, DisplayHeight, 768);

        trackEntryElem.add(trackVideoElem);
        tracksElem.add(trackEntryElem);

        trackEntryElem = (EbmlMaster) MKVType.createByType(TrackEntry);
        createLong(trackEntryElem, TrackNumber, 2);
        createLong(trackEntryElem, TrackUID, r.nextLong());

        createLong(trackEntryElem, TrackType, 2); // Video

        createString(trackEntryElem, Name, "Audio");

        createString(trackEntryElem, Language, "en");

        createString(trackEntryElem, CodecID, "A_MPEG/L3");

        EbmlMaster trackAudioElem = (EbmlMaster) MKVType.createByType(Audio);

        createLong(trackAudioElem, Channels, 2);
        MKVMuxer.createDouble(trackAudioElem, SamplingFrequency, 48000.0);

        trackEntryElem.add(trackAudioElem);
        tracksElem.add(trackEntryElem);

        segmentElem.add(tracksElem);

    }
    
    private void addFakeCues(EbmlMaster segmentElem) {
        EbmlMaster cuesElem = (EbmlMaster) MKVType.createByType(Cues);
        segmentElem.add(cuesElem);
    }

}
