package org.jcodec.containers.mp4.boxes;

import java.io.DataOutput;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.jcodec.common.io.ReaderBE;

/**
 * This class is part of JCodec ( www.jcodec.org )
 * This software is distributed under FreeBSD License

 * @author Jay Codec
 *
 */
public class SampleDescriptionBox extends NodeBox {

    private static final MyFactory FACTORY = new MyFactory();

    public static class MyFactory extends BoxFactory {
        private Map<String, Class<? extends Box>> handlers = new HashMap<String, Class<? extends Box>>();

        public MyFactory() {
            handlers.put("ap4h", VideoSampleEntry.class);
            handlers.put("apch", VideoSampleEntry.class);
            handlers.put("apcn", VideoSampleEntry.class);
            handlers.put("apcs", VideoSampleEntry.class);
            handlers.put("apco", VideoSampleEntry.class);
            handlers.put("avc1", VideoSampleEntry.class);
            handlers.put("cvid", VideoSampleEntry.class);
            handlers.put("jpeg", VideoSampleEntry.class);
            handlers.put("smc ", VideoSampleEntry.class);
            handlers.put("rle ", VideoSampleEntry.class);
            handlers.put("rpza", VideoSampleEntry.class);
            handlers.put("kpcd", VideoSampleEntry.class);
            handlers.put("png ", VideoSampleEntry.class);
            handlers.put("mjpa", VideoSampleEntry.class);
            handlers.put("mjpb", VideoSampleEntry.class);
            handlers.put("SVQ1", VideoSampleEntry.class);
            handlers.put("SVQ3", VideoSampleEntry.class);
            handlers.put("mp4v", VideoSampleEntry.class);
            handlers.put("dvc ", VideoSampleEntry.class);
            handlers.put("dvcp", VideoSampleEntry.class);
            handlers.put("gif ", VideoSampleEntry.class);
            handlers.put("h263", VideoSampleEntry.class);
            handlers.put("tiff", VideoSampleEntry.class);
            handlers.put("raw ", VideoSampleEntry.class);
            handlers.put("2vuY", VideoSampleEntry.class);
            handlers.put("yuv2", VideoSampleEntry.class);
            handlers.put("v308", VideoSampleEntry.class);
            handlers.put("v408", VideoSampleEntry.class);
            handlers.put("v216", VideoSampleEntry.class);
            handlers.put("v410", VideoSampleEntry.class);
            handlers.put("v210", VideoSampleEntry.class);
            handlers.put("m2v1", VideoSampleEntry.class);
            handlers.put("m1v1", VideoSampleEntry.class);

            handlers.put("ac-3", AudioSampleEntry.class);
            handlers.put("cac3", AudioSampleEntry.class);
            handlers.put("ima4", AudioSampleEntry.class);
            handlers.put("aac ", AudioSampleEntry.class);
            handlers.put("celp", AudioSampleEntry.class);
            handlers.put("hvxc", AudioSampleEntry.class);
            handlers.put("twvq", AudioSampleEntry.class);
            handlers.put(".mp1", AudioSampleEntry.class);
            handlers.put(".mp2", AudioSampleEntry.class);
            handlers.put("midi", AudioSampleEntry.class);
            handlers.put("apvs", AudioSampleEntry.class);
            handlers.put("alac", AudioSampleEntry.class);
            handlers.put("aach", AudioSampleEntry.class);
            handlers.put("aacl", AudioSampleEntry.class);
            handlers.put("aace", AudioSampleEntry.class);
            handlers.put("aacf", AudioSampleEntry.class);
            handlers.put("aacp", AudioSampleEntry.class);
            handlers.put("aacs", AudioSampleEntry.class);
            handlers.put("samr", AudioSampleEntry.class);
            handlers.put("AUDB", AudioSampleEntry.class);
            handlers.put("ilbc", AudioSampleEntry.class);
            handlers.put(new String(new byte[] {0x6D, 0x73, 0x00, 0x11}), AudioSampleEntry.class);
            handlers.put(new String(new byte[] {0x6D, 0x73, 0x00, 0x31}), AudioSampleEntry.class);
            handlers.put("aes3", AudioSampleEntry.class);
            handlers.put("NONE", AudioSampleEntry.class);
            handlers.put("raw ", AudioSampleEntry.class);
            handlers.put("twos", AudioSampleEntry.class);
            handlers.put("sowt", AudioSampleEntry.class);
            handlers.put("MAC3 ", AudioSampleEntry.class);
            handlers.put("MAC6 ", AudioSampleEntry.class);
            handlers.put("ima4", AudioSampleEntry.class);
            handlers.put("fl32", AudioSampleEntry.class);
            handlers.put("fl64", AudioSampleEntry.class);
            handlers.put("in24", AudioSampleEntry.class);
            handlers.put("in32", AudioSampleEntry.class);
            handlers.put("ulaw", AudioSampleEntry.class);
            handlers.put("alaw", AudioSampleEntry.class);
            handlers.put("dvca", AudioSampleEntry.class);
            handlers.put("QDMC", AudioSampleEntry.class);
            handlers.put("QDM2", AudioSampleEntry.class);
            handlers.put("Qclp", AudioSampleEntry.class);
            handlers.put(".mp3", AudioSampleEntry.class);
            handlers.put("mp4a", AudioSampleEntry.class);
            handlers.put("lpcm", AudioSampleEntry.class);

            handlers.put("tmcd", TimecodeSampleEntry.class);
            handlers.put("time", TimecodeSampleEntry.class);

            handlers.put("c608", SampleEntry.class);
            handlers.put("c708", SampleEntry.class);
        }

        public Class<? extends Box> toClass(String fourcc) {
            return handlers.get(fourcc);
        }
    }

    public static String fourcc() {
        return "stsd";
    }

    public SampleDescriptionBox(Header header) {
        super(header);
        factory = FACTORY;
    }

    public SampleDescriptionBox() {
        this(new Header(fourcc()));
    }
    
    public SampleDescriptionBox(SampleEntry...entries) {
        this();
        for (SampleEntry e : entries) {
            boxes.add(e);
        }
    }

    public void parse(InputStream input) throws IOException {
        ReaderBE.readInt32(input);
        ReaderBE.readInt32(input);
        super.parse(input);
    }

    @Override
    public void doWrite(DataOutput out) throws IOException {
        out.writeInt(0);
        out.writeInt(boxes.size());
        super.doWrite(out);
    }
}