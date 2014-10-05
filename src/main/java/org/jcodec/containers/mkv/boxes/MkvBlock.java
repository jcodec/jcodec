package org.jcodec.containers.mkv.boxes;

import static org.jcodec.containers.mkv.MKVType.Block;
import static org.jcodec.containers.mkv.MKVType.SimpleBlock;
import static org.jcodec.containers.mkv.boxes.EbmlSint.convertToBytes;
import static org.jcodec.containers.mkv.boxes.EbmlSint.signedComplement;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;

import org.jcodec.common.ByteArrayList;
import org.jcodec.common.SeekableByteChannel;
import org.jcodec.containers.mkv.util.EbmlUtil;

public class MkvBlock extends EbmlBin {
    private static final String XIPH = "Xiph";
    private static final String EBML = "EBML";
    private static final String FIXED = "Fixed";
    private static final int MAX_BLOCK_HEADER_SIZE = 512;
    public int[] frameOffsets;
    public int[] frameSizes;
    public long trackNumber;
    public int timecode;
    public long absoluteTimecode;
    public boolean keyFrame;
    public int headerSize;
    public String lacing;
    public boolean discardable;
    public boolean lacingPresent;
    public ByteBuffer[] frames;

    public static MkvBlock copy(MkvBlock old) {
        MkvBlock be = new MkvBlock(old.id);
        be.trackNumber = old.trackNumber;
        be.timecode = old.timecode;
        be.absoluteTimecode = old.absoluteTimecode;
        be.keyFrame = old.keyFrame;
        be.headerSize = old.headerSize;
        be.lacing = old.lacing;
        be.discardable = old.discardable;
        be.lacingPresent = old.lacingPresent;
        be.frameOffsets = new int[old.frameOffsets.length];
        be.frameSizes = new int[old.frameSizes.length];
        be.dataOffset = old.dataOffset;
        be.offset = old.offset;
        be.type = old.type;
        System.arraycopy(old.frameOffsets, 0, be.frameOffsets, 0, be.frameOffsets.length);
        System.arraycopy(old.frameSizes, 0, be.frameSizes, 0, be.frameSizes.length);
        return be;
    }

    public static MkvBlock keyFrame(long trackNumber, int timecode, ByteBuffer frame) {
        MkvBlock be = new MkvBlock(SimpleBlock.id);
        be.frames = new ByteBuffer[] { frame };
        be.frameSizes = new int[] { frame.limit() };
        be.keyFrame = true;
        be.trackNumber = trackNumber;
        be.timecode = timecode;
        return be;
    }

    public MkvBlock(byte[] type) {
        super(type);
        if (!Arrays.equals(SimpleBlock.id, type) && !Arrays.equals(Block.id, type))
            throw new IllegalArgumentException("Block initiated with invalid id: " + EbmlUtil.toHexString(type));
    }
    
    @Override
    public void read(SeekableByteChannel is) throws IOException {
        ByteBuffer bb = ByteBuffer.allocate((int) 100);
        is.read(bb);
        bb.flip();
        this.read(bb);
        is.position(this.dataOffset+this.dataLen);
    }

    @Override
    public void read(ByteBuffer source) {
        ByteBuffer bb = source.slice();

        trackNumber = MkvBlock.ebmlDecode(bb);
        int tcPart1 = bb.get() & 0xFF;
        int tcPart2 = bb.get() & 0xFF;
        timecode = (short) (((short) tcPart1 << 8) | (short) tcPart2);

        int flags = bb.get() & 0xFF;
        keyFrame = (flags & 0x80) > 0;
        discardable = (flags & 0x01) > 0;
        int laceFlags = flags & 0x06;

        lacingPresent = laceFlags != 0x00;
        if (lacingPresent) {
            int lacesCount = bb.get() & 0xFF;
            frameSizes = new int[lacesCount + 1];
            if (laceFlags == 0x02) {
                /* Xiph */
                lacing = XIPH;
                headerSize = readXiphLaceSizes(bb, frameSizes, (int) this.dataLen, bb.position());

            } else if (laceFlags == 0x06) {
                /* EBML */
                lacing = EBML;
                headerSize = readEBMLLaceSizes(bb, frameSizes, (int) this.dataLen, bb.position());

            } else if (laceFlags == 0x04) {
                /* Fixed Size Lacing */
                this.lacing = FIXED;
                this.headerSize = bb.position();
                int aLaceSize = (int) ((this.dataLen - this.headerSize) / (lacesCount + 1));
                Arrays.fill(frameSizes, aLaceSize);
                
            } else {
                throw new RuntimeException("Unsupported lacing type flag.");
            }
            turnSizesToFrameOffsets(frameSizes);
        } else {

            this.lacing = "";
            int frameOffset = bb.position();
            frameOffsets = new int[1];
            frameOffsets[0] = frameOffset;

            headerSize = bb.position();

            frameSizes = new int[1];
            frameSizes[0] = (int) (this.dataLen - headerSize);
        }
    }

    private void turnSizesToFrameOffsets(int[] sizes) {
        frameOffsets = new int[sizes.length];
        frameOffsets[0] = headerSize;
        for (int i = 1; i < sizes.length; i++)
            frameOffsets[i] = frameOffsets[i - 1] + sizes[i - 1];

    }

    public static int readXiphLaceSizes(ByteBuffer bb, int[] sizes, int size, int preLacingHeaderSize) {
        int startPos = bb.position();
        int lastIndex = sizes.length - 1;
        sizes[lastIndex] = size;

        for (int l = 0; l < lastIndex; l++) {
            int laceSize = 255;
            while (laceSize == 255) {
                laceSize = bb.get() & 0xFF;
                sizes[l] += laceSize;
            }
            // Update the size of the last block
            sizes[lastIndex] -= sizes[l];
        }

        int headerSize = (bb.position() - startPos) + preLacingHeaderSize;
        sizes[lastIndex] -= headerSize;

        return headerSize;
    }

    public static int readEBMLLaceSizes(ByteBuffer source, int[] sizes, int size, int preLacingHeaderSize) {

        int lastIndex = sizes.length - 1;
        sizes[lastIndex] = size;

        int startPos = source.position();
        sizes[0] = (int) MkvBlock.ebmlDecode(source);

        sizes[lastIndex] -= sizes[0];

        int laceSize = sizes[0];
        long laceSizeDiff = 0;
        for (int l = 1; l < lastIndex; l++) {
            laceSizeDiff = MkvBlock.ebmlDecodeSigned(source);

            laceSize += laceSizeDiff;
            sizes[l] = laceSize;

            // Update the size of the last block
            sizes[lastIndex] -= sizes[l];
        }

        int headerSize = (source.position() - startPos) + preLacingHeaderSize;
        sizes[lastIndex] -= headerSize;
        return headerSize;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{dataOffset: ").append(dataOffset);
        sb.append(", trackNumber: ").append(trackNumber);
        sb.append(", timecode: ").append(timecode);
        sb.append(", keyFrame: ").append(keyFrame);
        sb.append(", headerSize: ").append(headerSize);
        sb.append(", lacing: ").append(lacing);
        for (int i = 0; i < frameSizes.length; i++)
            sb.append(", frame[").append(i).append("]  offset ").append(frameOffsets[i]).append(" size ").append(frameSizes[i]);

        sb.append(" }");

        return sb.toString();
    }

    public ByteBuffer[] getFrames(ByteBuffer source) throws IOException {
        ByteBuffer[] frames = new ByteBuffer[frameSizes.length];
        for (int i = 0; i < frameSizes.length; i++) {
            if (frameOffsets[i] > source.limit())
                System.err.println("frame offset: " + frameOffsets[i] + " limit: " + source.limit());
            source.position(frameOffsets[i]);
            ByteBuffer bb = source.slice();
            bb.limit(frameSizes[i]);
            frames[i] = bb;
        }
        return frames;
    }

    public void readFrames(ByteBuffer source) throws IOException {
        this.frames = getFrames(source);
    }

    // @Override
    public ByteBuffer getData() {
        int dataSize = (int) getDataSize();
        ByteBuffer bb = ByteBuffer.allocate(dataSize + EbmlUtil.ebmlLength(dataSize) + id.length);
        bb.put(id);
        bb.put(EbmlUtil.ebmlEncode(dataSize));

        bb.put(EbmlUtil.ebmlEncode(trackNumber));
        bb.put((byte) ((timecode >>> 8) & 0xFF));
        bb.put((byte) (timecode & 0xFF));

        byte flags = 0x00;
        if (XIPH.equals(lacing)) {
            flags = 0x02;
        } else if (EBML.equals(lacing)) {
            flags = 0x06;
        } else if (FIXED.equals(lacing)) {
            flags = 0x04;
        }

        if (discardable)
            flags |= 0x01;
        if (keyFrame)
            flags |= 0x80;

        bb.put(flags);

        if ((flags & 0x06) != 0) {
            bb.put((byte) ((frames.length - 1) & 0xFF));
            bb.put(muxLacingInfo());
        }

        for (ByteBuffer frame : frames)
            bb.put(frame);

        bb.flip();
        return bb;
    }

    public void seekAndReadContent(FileChannel source) throws IOException {
        data = ByteBuffer.allocate((int) dataLen);
        source.position(dataOffset);
        source.read(data);
        this.data.flip();
    }

    /**
     * Get the total size of this element
     */
    @Override
    public long size() {
        long size = getDataSize();
        size += EbmlUtil.ebmlLength(size);
        size += id.length;
        return size;
    }

    public int getDataSize() {
        int size = 0;
        // TODO: one can do same calculation with for(byte[] aFrame : this.frames) size += aFrame.length;
        for (long fsize : frameSizes)
            size += fsize;

        if (lacingPresent) {
            size += muxLacingInfo().length;
            size += 1; // int8 laces count, a.k.a. frame_count-1
        }

        size += 3; // int8 - flags; sint16 - timecode
        size += EbmlUtil.ebmlLength(trackNumber);
        return size;
    }

    private byte[] muxLacingInfo() {
        if (EBML.equals(lacing))
            return muxEbmlLacing(frameSizes);

        if (XIPH.equals(lacing))
            return muxXiphLacing(frameSizes);

        if (FIXED.equals(lacing))
            return new byte[0];

        return null;
    }

    static public long ebmlDecode(ByteBuffer bb) {
        byte firstByte = bb.get();
        int length = EbmlUtil.computeLength(firstByte);
        if (length == 0)
            throw new RuntimeException("Invalid ebml integer size.");
    
        long value = firstByte & (0xFF >>> length);
    
        length--;
        while (length > 0) {
            value = (value << 8) | (bb.get() & 0xff);
            length--;
        }
    
        return value;
    }

    static public long ebmlDecodeSigned(ByteBuffer source) {
        byte firstByte = source.get();
        int size = EbmlUtil.computeLength(firstByte);
    
        if (size == 0)
            throw new RuntimeException("Invalid ebml integer size.");
    
        long value = firstByte & (0xFF >>> size);
        int remaining = size-1;
        while (remaining > 0){
            value = (value << 8) | (source.get() & 0xff);
            remaining--;
        }
    
        return value - signedComplement[size];
    }

    public static long[] calcEbmlLacingDiffs(int[] laceSizes) {
        int lacesCount = laceSizes.length - 1;
        long[] out = new long[lacesCount];
        out[0] = (int) laceSizes[0];
        for (int i = 1; i < lacesCount; i++) {
            out[i] = laceSizes[i] - laceSizes[i - 1];
        }
        return out;
    }

    public static byte[] muxEbmlLacing(int[] laceSizes) {
        ByteArrayList bytes = new ByteArrayList();

        long[] laceSizeDiffs = calcEbmlLacingDiffs(laceSizes);
        bytes.addAll(EbmlUtil.ebmlEncode(laceSizeDiffs[0]));

        for (int i = 1; i < laceSizeDiffs.length; i++) {
            bytes.addAll(convertToBytes(laceSizeDiffs[i]));
        }
        return bytes.toArray();
    }

    public static byte[] muxXiphLacing(int[] laceSizes) {
        ByteArrayList bytes = new ByteArrayList();
        for (int i = 0; i < laceSizes.length - 1; i++) {
            long laceSize = laceSizes[i];
            while (laceSize >= 255) {
                bytes.add((byte) 255);
                laceSize -= 255;
            }
            bytes.add((byte) laceSize);
        }
        return bytes.toArray();
    }

}
