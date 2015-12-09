package org.jcodec.containers.mps.index;

import java.nio.ByteBuffer;

import org.jcodec.common.RunLength.Integer;
import org.jcodec.common.io.NIOUtils;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Represents index for MPEG TS stream, enables demuxers to do precise seek
 * 
 * Note: some values inside the MPSIndex are not expressed in bytes anymore, but
 * rather in integral MPEG TS packets.
 * 
 * @author The JCodec project
 * 
 */
public class MTSIndex {
    private MTSProgram[] programs;

    public static class MTSProgram extends MPSIndex {
        private int targetGuid;

        public MTSProgram(long[] pesTokens, Integer pesStreamIds, MPSStreamIndex[] streams, int targetGuid) {
            super(pesTokens, pesStreamIds, streams);
            this.targetGuid = targetGuid;
        }

        public MTSProgram(MPSIndex mpsIndex, int target) {
            super(mpsIndex);
            this.targetGuid = target;
        }

        public int getTargetGuid() {
            return targetGuid;
        }

        @Override
        public void serializeTo(ByteBuffer index) {
            index.putInt(targetGuid);
            super.serializeTo(index);
        }

        public static MTSProgram parse(ByteBuffer read) {
            int targetGuid = read.getInt();
            return new MTSProgram(MPSIndex.parseIndex(read), targetGuid);
        }
    }

    public MTSIndex(MTSProgram[] programs) {
        this.programs = programs;
    }

    public MTSProgram[] getPrograms() {
        return programs;
    }

    public static MTSIndex parse(ByteBuffer buf) {
        int numPrograms = buf.getInt();
        MTSProgram[] programs = new MTSProgram[numPrograms];
        for (int i = 0; i < numPrograms; i++) {
            int programDataSize = buf.getInt();
            programs[i] = MTSProgram.parse(NIOUtils.read(buf, programDataSize));
        }
        return new MTSIndex(programs);
    }

    public int estimateSize() {
        int totalSize = 64;
        for (MTSProgram mtsProgram : programs) {
            totalSize += 4 + mtsProgram.estimateSize();
        }
        return totalSize;
    }

    public void serializeTo(ByteBuffer buf) {
        buf.putInt(programs.length);
        for (MTSProgram mtsAnalyser : programs) {
            ByteBuffer dup = buf.duplicate();
            NIOUtils.skip(buf, 4);
            mtsAnalyser.serializeTo(buf);
            dup.putInt(buf.position() - dup.position() - 4);
        }
    }

    public ByteBuffer serialize() {
        ByteBuffer bb = ByteBuffer.allocate(estimateSize());
        serializeTo(bb);
        bb.flip();
        return bb;
    }
}