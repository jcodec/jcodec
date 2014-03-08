package org.jcodec.movtool.streaming.tracks;

import static org.jcodec.containers.mps.MPSUtils.readPESHeader;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.jcodec.common.Assert;
import org.jcodec.common.FileChannelWrapper;
import org.jcodec.common.NIOUtils;
import org.jcodec.common.SeekableByteChannel;
import org.jcodec.containers.mps.MPSDemuxer;
import org.jcodec.containers.mps.MPSUtils;
import org.jcodec.movtool.streaming.VirtualPacket;
import org.jcodec.movtool.streaming.tracks.MPSTrackFactory.Stream;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * A factory for MPEG TS virtual tracks coming out of streams of MPEG PS
 * 
 * @author The JCodec project
 * 
 */
public class MTSTrackFactory {
    private List<MTSProgram> programs = new ArrayList<MTSProgram>();

    public MTSTrackFactory(ByteBuffer index, FilePool fp) throws IOException {
        while (index.remaining() >= 6) {
            int len = index.getInt() - 4;
            ByteBuffer sub = NIOUtils.read(index, len);
            programs.add(new MTSProgram(sub, fp));
        }
    }

    public class MTSProgram extends MPSTrackFactory {
        private int targetGuid;

        public MTSProgram(ByteBuffer index, FilePool fp) throws IOException {
            super(index, fp);
        }

        @Override
        protected void readIndex(ByteBuffer index) throws IOException {
            targetGuid = index.getShort() & 0xffff;
            super.readIndex(index);
        }

        @Override
        protected Stream createStream(int streamId) {
            return new MTSStream(streamId);
        }

        public class MTSStream extends Stream {

            public MTSStream(int streamId) {
                super(streamId);
            }

            @Override
            protected ByteBuffer readPes(SeekableByteChannel ch, long pesPosition, int pesSize, int payloadSize,
                    int pesAbsIdx) throws IOException {
                ch.position(pesPosition * 188);
                ByteBuffer buf = NIOUtils.fetchFrom(ch, pesSize * 188);

                // NOW REMOVE THE TS CRAP
                ByteBuffer dst = buf.duplicate();
                while (buf.hasRemaining()) {
                    ByteBuffer tsBuf = NIOUtils.read(buf, 188);
                    Assert.assertEquals(0x47, tsBuf.get() & 0xff);
                    int guidFlags = ((tsBuf.get() & 0xff) << 8) | (tsBuf.get() & 0xff);
                    int guid = (int) guidFlags & 0x1fff;
                    if (guid == targetGuid) {
                        int b0 = tsBuf.get() & 0xff;
                        int counter = b0 & 0xf;
                        if ((b0 & 0x20) != 0) {
                            NIOUtils.skip(tsBuf, tsBuf.get() & 0xff);
                        }
                        dst.put(tsBuf);
                    }
                }
                dst.flip();
                readPESHeader(dst, 0);
                dst.limit(dst.position() + payloadSize);
                return dst;
            }
        }
    }

    public List<Stream> getVideoStreams() {
        List<Stream> ret = new ArrayList<Stream>();
        for (MTSProgram mtsProgram : programs) {
            ret.addAll(mtsProgram.getVideoStreams());
        }
        return ret;
    }

    public List<Stream> getAudioStreams() {
        List<Stream> ret = new ArrayList<Stream>();
        for (MTSProgram mtsProgram : programs) {
            ret.addAll(mtsProgram.getAudioStreams());
        }
        return ret;
    }

    public List<Stream> getStreams() {
        List<Stream> ret = new ArrayList<Stream>();
        for (MTSProgram mtsProgram : programs) {
            ret.addAll(mtsProgram.getStreams());
        }
        return ret;
    }

    public static void main(String[] args) throws IOException {
        FilePool fp = new FilePool(new File(args[0]), 10);
        MTSTrackFactory factory = new MTSTrackFactory(NIOUtils.fetchFrom(new File(args[1])), fp);
        Stream stream = factory.getVideoStreams().get(0);
        FileChannelWrapper ch = NIOUtils.writableFileChannel(new File(args[2]));

        List<VirtualPacket> pkt = new ArrayList<VirtualPacket>();
        for (int i = 0; i < 2000; i++) {
            pkt.add(stream.nextPacket());
        }

        for (VirtualPacket virtualPacket : pkt) {
            ch.write(virtualPacket.getData());
        }

        ch.close();
    }
}