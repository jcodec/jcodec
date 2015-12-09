package org.jcodec.containers.mps.psi;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.logging.Logger;
import org.jcodec.containers.mps.MPSUtils;
import org.jcodec.containers.mps.MPSUtils.MPEGMediaDescriptor;
import org.jcodec.containers.mps.MTSUtils.StreamType;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Represents PMT ( Program Map Table ) of the MPEG Transport stream
 * 
 * This section contains information about streams of an individual program, a
 * program usually contains two or more streams, such as video, audio, text,
 * etc..
 * 
 * @author The JCodec project
 * 
 */
public class PMTSection extends PSISection {

    private int pcrPid;
    private Tag[] tags;
    private PMTStream[] streams;

    public PMTSection(PSISection psi, int pcrPid, Tag[] tags, PMTStream[] streams) {
        super(psi);
        this.pcrPid = pcrPid;
        this.tags = tags;
        this.streams = streams;
    }

    public int getPcrPid() {
        return pcrPid;
    }

    public Tag[] getTags() {
        return tags;
    }

    public PMTStream[] getStreams() {
        return streams;
    }

    public static PMTSection parse(ByteBuffer data) {
        PSISection psi = PSISection.parse(data);

        int w1 = data.getShort() & 0xffff;
        int pcrPid = w1 & 0x1fff;

        int w2 = data.getShort() & 0xffff;
        int programInfoLength = w2 & 0xfff;

        List<Tag> tags = parseTags(NIOUtils.read(data, programInfoLength));
        List<PMTStream> streams = new ArrayList<PMTStream>();
        while (data.remaining() > 4) {
            int streamType = data.get() & 0xff;
            int wn = data.getShort() & 0xffff;
            int elementaryPid = wn & 0x1fff;

//            Logger.info(String.format("Elementary stream: [%d,%d]", streamType, elementaryPid));

            int wn1 = data.getShort() & 0xffff;
            int esInfoLength = wn1 & 0xfff;
            ByteBuffer read = NIOUtils.read(data, esInfoLength);
            streams.add(new PMTStream(streamType, elementaryPid, MPSUtils.parseDescriptors(read)));
        }

        return new PMTSection(psi, pcrPid, tags.toArray(new Tag[0]), streams.toArray(new PMTStream[0]));
    }

    static List<Tag> parseTags(ByteBuffer bb) {
        List<Tag> tags = new ArrayList<Tag>();
        while (bb.hasRemaining()) {
            int tag = bb.get();
            int tagLen = bb.get();
//            Logger.info(String.format("TAG: [0x%x, 0x%x]", tag, tagLen));
            tags.add(new Tag(tag, NIOUtils.read(bb, tagLen)));
        }
        return tags;
    }

    public static class Tag {
        private int tag;
        private ByteBuffer content;

        public Tag(int tag, ByteBuffer content) {
            this.tag = tag;
            this.content = content;
        }

        public int getTag() {
            return tag;
        }

        public ByteBuffer getContent() {
            return content;
        }
    }

    public static class PMTStream {
        private int streamTypeTag;
        private int pid;
        private List<MPEGMediaDescriptor> descriptors;
        private StreamType streamType;

        public PMTStream(int streamTypeTag, int pid, List<MPEGMediaDescriptor> descriptors) {
            this.streamTypeTag = streamTypeTag;
            this.pid = pid;
            this.descriptors = descriptors;
            this.streamType = StreamType.fromTag(streamTypeTag);
        }

        public int getStreamTypeTag() {
            return streamTypeTag;
        }

        public StreamType getStreamType() {
            return streamType;
        }

        public int getPid() {
            return pid;
        }

        public List<MPEGMediaDescriptor> getDesctiptors() {
            return descriptors;
        }
    }
}