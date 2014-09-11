package org.jcodec.containers.mps.index;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.jcodec.common.Assert;
import org.jcodec.common.NIOUtils;
import org.jcodec.common.SeekableByteChannel;
import org.jcodec.containers.mps.index.MPSIndex.MPSStreamIndex;
import org.jcodec.containers.mps.index.MTSIndex.MTSProgram;

public class MTSRandomAccessDemuxer {

    private MTSProgram[] programs;
    private SeekableByteChannel ch;

    public MTSRandomAccessDemuxer(SeekableByteChannel ch, MTSIndex index) {
        programs = index.getPrograms();
        this.ch = ch;
    }

    public int[] getGuids() {
        int[] guids = new int[programs.length];
        for (int i = 0; i < programs.length; i++)
            guids[i] = programs[i].getTargetGuid();
        return guids;
    }

    public MPSRandomAccessDemuxer getProgramDemuxer(final int tgtGuid) throws IOException {
        MPSIndex index = getProgram(tgtGuid);
        return new MPSRandomAccessDemuxer(ch, index) {
            @Override
            protected Stream newStream(SeekableByteChannel ch, MPSStreamIndex streamIndex) throws IOException {
                return new Stream(streamIndex, ch) {
                    @Override
                    protected ByteBuffer fetch(int pesLen) throws IOException {
                        ByteBuffer bb = ByteBuffer.allocate(pesLen * 188);
                        
                        for(int i = 0; i < pesLen; i++) {
                            ByteBuffer tsBuf = NIOUtils.fetchFrom(source, 188);
                            Assert.assertEquals(0x47, tsBuf.get() & 0xff);
                            int guidFlags = ((tsBuf.get() & 0xff) << 8) | (tsBuf.get() & 0xff);
                            int guid = (int) guidFlags & 0x1fff;
                            if(guid != tgtGuid)
                                continue;
                            int payloadStart = (guidFlags >> 14) & 0x1;
                            int b0 = tsBuf.get() & 0xff;
                            int counter = b0 & 0xf;
                            if ((b0 & 0x20) != 0) {
                                NIOUtils.skip(tsBuf, tsBuf.get() & 0xff);
                            }
                            bb.put(tsBuf);
                        }
                        bb.flip();
                        return bb;
                    }

                    @Override
                    protected void skip(long leadingSize) throws IOException {
                        source.position(source.position() + leadingSize * 188);
                    }

                    @Override
                    protected void reset() throws IOException {
                        source.position(0);
                    }

                };
            }
        };
    }

    private MPSIndex getProgram(int guid) {
        for (MTSProgram mtsProgram : programs) {
            if (mtsProgram.getTargetGuid() == guid)
                return mtsProgram;
        }
        return null;
    }
}