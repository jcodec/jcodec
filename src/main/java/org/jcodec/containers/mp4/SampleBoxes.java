package org.jcodec.containers.mp4;

import static org.jcodec.platform.Platform.stringFromBytes;

import org.jcodec.containers.mp4.boxes.AudioSampleEntry;
import org.jcodec.containers.mp4.boxes.SampleEntry;
import org.jcodec.containers.mp4.boxes.TextMetaDataSampleEntry;
import org.jcodec.containers.mp4.boxes.TimecodeSampleEntry;
import org.jcodec.containers.mp4.boxes.URIMetaSampleEntry;
import org.jcodec.containers.mp4.boxes.VideoSampleEntry;
import org.jcodec.containers.mp4.boxes.XMLMetaDataSampleEntry;
import org.jcodec.platform.Platform;

public class SampleBoxes extends Boxes {

    public SampleBoxes() {
        clear();
        
        override("ap4h", VideoSampleEntry.class);
        override("apch", VideoSampleEntry.class);
        override("apcn", VideoSampleEntry.class);
        override("apcs", VideoSampleEntry.class);
        override("apco", VideoSampleEntry.class);
        override("avc1", VideoSampleEntry.class);
        override("cvid", VideoSampleEntry.class);
        override("jpeg", VideoSampleEntry.class);
        override("smc ", VideoSampleEntry.class);
        override("rle ", VideoSampleEntry.class);
        override("rpza", VideoSampleEntry.class);
        override("kpcd", VideoSampleEntry.class);
        override("png ", VideoSampleEntry.class);
        override("mjpa", VideoSampleEntry.class);
        override("mjpb", VideoSampleEntry.class);
        override("SVQ1", VideoSampleEntry.class);
        override("SVQ3", VideoSampleEntry.class);
        override("mp4v", VideoSampleEntry.class);
        override("dvc ", VideoSampleEntry.class);
        override("dvcp", VideoSampleEntry.class);
        override("gif ", VideoSampleEntry.class);
        override("h263", VideoSampleEntry.class);
        override("tiff", VideoSampleEntry.class);
        override("raw ", VideoSampleEntry.class);
        override("2vuY", VideoSampleEntry.class);
        override("yuv2", VideoSampleEntry.class);
        override("v308", VideoSampleEntry.class);
        override("v408", VideoSampleEntry.class);
        override("v216", VideoSampleEntry.class);
        override("v410", VideoSampleEntry.class);
        override("v210", VideoSampleEntry.class);
        override("m2v1", VideoSampleEntry.class);
        override("m1v1", VideoSampleEntry.class);
        override("xd5b", VideoSampleEntry.class);
        override("dv5n", VideoSampleEntry.class);
        override("jp2h", VideoSampleEntry.class);
        override("mjp2", VideoSampleEntry.class);

        override("ac-3", AudioSampleEntry.class);
        override("cac3", AudioSampleEntry.class);
        override("ima4", AudioSampleEntry.class);
        override("aac ", AudioSampleEntry.class);
        override("celp", AudioSampleEntry.class);
        override("hvxc", AudioSampleEntry.class);
        override("twvq", AudioSampleEntry.class);
        override(".mp1", AudioSampleEntry.class);
        override(".mp2", AudioSampleEntry.class);
        override("midi", AudioSampleEntry.class);
        override("apvs", AudioSampleEntry.class);
        override("alac", AudioSampleEntry.class);
        override("aach", AudioSampleEntry.class);
        override("aacl", AudioSampleEntry.class);
        override("aace", AudioSampleEntry.class);
        override("aacf", AudioSampleEntry.class);
        override("aacp", AudioSampleEntry.class);
        override("aacs", AudioSampleEntry.class);
        override("samr", AudioSampleEntry.class);
        override("AUDB", AudioSampleEntry.class);
        override("ilbc", AudioSampleEntry.class);
        override(stringFromBytes(new byte[] {0x6D, 0x73, 0x00, 0x11}), AudioSampleEntry.class);
        override(stringFromBytes(new byte[] {0x6D, 0x73, 0x00, 0x31}), AudioSampleEntry.class);
        override("aes3", AudioSampleEntry.class);
        override("NONE", AudioSampleEntry.class);
        override("raw ", AudioSampleEntry.class);
        override("twos", AudioSampleEntry.class);
        override("sowt", AudioSampleEntry.class);
        override("MAC3 ", AudioSampleEntry.class);
        override("MAC6 ", AudioSampleEntry.class);
        override("ima4", AudioSampleEntry.class);
        override("fl32", AudioSampleEntry.class);
        override("fl64", AudioSampleEntry.class);
        override("in24", AudioSampleEntry.class);
        override("in32", AudioSampleEntry.class);
        override("ulaw", AudioSampleEntry.class);
        override("alaw", AudioSampleEntry.class);
        override("dvca", AudioSampleEntry.class);
        override("QDMC", AudioSampleEntry.class);
        override("QDM2", AudioSampleEntry.class);
        override("Qclp", AudioSampleEntry.class);
        override(".mp3", AudioSampleEntry.class);
        override("mp4a", AudioSampleEntry.class);
        override("lpcm", AudioSampleEntry.class);

        override("tmcd", TimecodeSampleEntry.class);
        override("time", TimecodeSampleEntry.class);

        override("c608", SampleEntry.class);
        override("c708", SampleEntry.class);
        override("text", SampleEntry.class);
        
        override("metx", XMLMetaDataSampleEntry.class);
        override("mett", TextMetaDataSampleEntry.class);
        override("urim", URIMetaSampleEntry.class);
        
        //found in gopro video files
        override("fdsc", SampleEntry.class);
    }
}