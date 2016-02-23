package org.jcodec.codecs.mpeg12.bitstream;

import java.nio.ByteBuffer;

import org.jcodec.common.io.BitReader;
import org.jcodec.common.io.BitWriter;
import org.jcodec.common.model.TapeTimecode;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class GOPHeader implements MPEGHeader {

    private TapeTimecode timeCode;
    private boolean closedGop;
    private boolean brokenLink;

    public GOPHeader(TapeTimecode timeCode, boolean closedGop, boolean brokenLink) {
        this.timeCode = timeCode;
        this.closedGop = closedGop;
        this.brokenLink = brokenLink;
    }

    public static GOPHeader read(ByteBuffer bb) {
        BitReader _in = new BitReader(bb);
        boolean dropFrame = _in.read1Bit() == 1;
        short hours = (short) _in.readNBit(5);
        byte minutes = (byte) _in.readNBit(6);
        _in.skip(1);

        byte seconds = (byte) _in.readNBit(6);
        byte frames = (byte) _in.readNBit(6);

        boolean closedGop = _in.read1Bit() == 1;
        boolean brokenLink = _in.read1Bit() == 1;

        return new GOPHeader(new TapeTimecode(hours, minutes, seconds, frames, dropFrame), closedGop, brokenLink);
    }

    @Override
    public void write(ByteBuffer bb) {
        BitWriter bw = new BitWriter(bb);
        if (timeCode == null)
            bw.writeNBit(0, 25);
        else {
            bw.write1Bit(timeCode.isDropFrame() ? 1 : 0);
            bw.writeNBit(timeCode.getHour(), 5);
            bw.writeNBit(timeCode.getMinute(), 6);
            bw.write1Bit(1);
            bw.writeNBit(timeCode.getSecond(), 6);
            bw.writeNBit(timeCode.getFrame(), 6);
        }
        bw.write1Bit(closedGop ? 1 : 0);
        bw.write1Bit(brokenLink ? 1 : 0);
        bw.flush();
    }

    public TapeTimecode getTimeCode() {
        return timeCode;
    }

    public boolean isClosedGop() {
        return closedGop;
    }

    public boolean isBrokenLink() {
        return brokenLink;
    }
}