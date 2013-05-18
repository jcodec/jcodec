package org.jcodec.containers.mkv.elements;

import static org.jcodec.containers.mkv.ebml.SignedIntegerElement.convertToBytes;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;

import org.jcodec.common.ByteArrayList;
import org.jcodec.containers.mkv.Reader;
import org.jcodec.containers.mkv.Type;
import org.jcodec.containers.mkv.ebml.BinaryElement;
import org.jcodec.containers.mkv.ebml.Element;

public class BlockElement extends BinaryElement {

    private static final String XIPH = "Xiph";
    private static final String EBML = "EBML";
    private static final String FIXED = "Fixed";
    private static final int MAX_BLOCK_HEADER_SIZE = 512;
    public long[] frameOffsets;
    public long[] frameSizes;
    public long trackNumber;
    public int timecode;
    public long absoluteTimecode;
    public boolean keyFrame;
    public int headerSize;
    public String lacing;
    public boolean discardable;
    public boolean lacingPresent;
    private byte[][] frames;

    public static BlockElement copy(BlockElement old) {
        BlockElement be = new BlockElement(old.id);
        be.trackNumber = old.trackNumber;
        be.timecode = old.timecode;
        be.absoluteTimecode = old.absoluteTimecode;
        be.keyFrame = old.keyFrame;
        be.headerSize = old.headerSize;
        be.lacing = old.lacing;
        be.discardable = old.discardable;
        be.lacingPresent = old.lacingPresent;
        be.frameOffsets = new long[old.frameOffsets.length];
        be.frameSizes = new long[old.frameSizes.length];
        be.dataOffset = old.dataOffset;
        be.offset = old.offset;
        be.type = old.type;
        System.arraycopy(old.frameOffsets, 0, be.frameOffsets, 0, be.frameOffsets.length);
        System.arraycopy(old.frameSizes, 0, be.frameSizes, 0, be.frameSizes.length);
        return be;
    }

    public BlockElement(byte[] type) {
        super(type);
        if (!Arrays.equals(Type.SimpleBlock.id, type) && !Arrays.equals(Type.Block.id, type))
            throw new IllegalArgumentException("Block initiated with invalid id: " + Reader.printAsHex(type));
    }

    @Override
    public void readData(FileChannel source) throws IOException {
        // dataOffset = source.position();

        // Read enough for the header to fit in
        ByteBuffer bb = ByteBuffer.allocate((int) (MAX_BLOCK_HEADER_SIZE > size ? size : MAX_BLOCK_HEADER_SIZE));
        source.read(bb);
        // Skip the rest of the block
        source.position(dataOffset + size);
        bb.flip();

        trackNumber = Reader.getEbmlVInt(bb);
        // Read timecode
        int blockTimecode1 = bb.get() & 0xFF;
        int blockTimecode2 = bb.get() & 0xFF;
        short tc = (short) (((short) blockTimecode1 << 8) | (short) blockTimecode2);
        timecode = tc;// (blockTimecode1 << 8) | blockTimecode2;

        int flags = bb.get() & 0xFF;
        keyFrame = (flags & 0x80) > 0;
        discardable = (flags & 0x01) > 0;
        int laceFlags = flags & 0x06;

        // Increase the HeaderSize by the number of bytes we have read
        lacingPresent = laceFlags != 0x00;
        if (lacingPresent) {
            // We have lacing
            int lacesCount = bb.get() & 0xFF;
            frameSizes = new long[lacesCount + 1];
            // HeaderSize += 1;
            if (laceFlags == 0x02) { // Xiph Lacing
                lacing = XIPH;
                headerSize = readXiphLaceSizes(bb, frameSizes, (int) this.size, bb.position());

            } else if (laceFlags == 0x06) { // EBML Lacing
                lacing = EBML;
                headerSize = readEBMLLaceSizes(bb, frameSizes, (int) this.size, bb.position());

            } else if (laceFlags == 0x04) { // Fixed Size Lacing
                this.lacing = FIXED;
                this.headerSize = bb.position();
                int aLaceSize = (int) ((this.size - this.headerSize) / (lacesCount + 1));
                Arrays.fill(frameSizes, aLaceSize);
            } else {
                throw new RuntimeException("Unsupported lacing type flag.");
            }
            turnSizesToFrameOffsets(frameSizes);
        } else {

            this.lacing = "";
            long frameOffset = dataOffset + bb.position();
            frameOffsets = new long[1];
            frameOffsets[0] = frameOffset;

            headerSize = bb.position(); // (int) (frameOffset - dataOffset);

            frameSizes = new long[1];
            frameSizes[0] = this.size - headerSize;
        }
    }

    private void turnSizesToFrameOffsets(long[] sizes) {
        frameOffsets = new long[sizes.length];
        frameOffsets[0] = dataOffset + headerSize;
        for (int i = 1; i < sizes.length; i++)
            frameOffsets[i] = frameOffsets[i - 1] + sizes[i - 1];

    }

    public static int readXiphLaceSizes(ByteBuffer bb, long[] sizes, int size, int preLacingHeaderSize) {
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

    public static int readEBMLLaceSizes(ByteBuffer source, long[] sizes, int size, int preLacingHeaderSize) {

        int lastIndex = sizes.length - 1;
        sizes[lastIndex] = size;

        int startPos = source.position();
        sizes[0] = Reader.getEbmlVInt(source);

        sizes[lastIndex] -= sizes[0];

        long laceSize = sizes[0];
        long laceSizeDiff = 0;
        for (int l = 1; l < lastIndex; l++) {
            laceSizeDiff = Reader.getSignedEbmlVInt(source);

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
            sb.append(", frame[").append(i).append("]  offset ").append(frameOffsets[i]).append(" size ")
                    .append(frameSizes[i]);

        sb.append(" }");

        return sb.toString();
    }

    public byte[][] getFrames(FileChannel source) throws IOException {
        byte[][] frames = new byte[frameSizes.length][];
        for (int i = 0; i < frameSizes.length; i++) {
            ByteBuffer bb = ByteBuffer.allocate((int) frameSizes[i]);
            source.position(frameOffsets[i]);
            source.read(bb);
            bb.flip();
            frames[i] = bb.array();
        }
        return frames;
    }

    public void readFrames(FileChannel source) throws IOException {
        this.frames = getFrames(source);
    }

    @Override
    public ByteBuffer mux() {
        int dataSize = (int) getDataSize();
        ByteBuffer bb = ByteBuffer.allocate(dataSize + getEbmlSize(dataSize) + id.length);
        bb.put(id);
        bb.put(ebmlBytes(dataSize));

        bb.put(ebmlBytes(trackNumber));
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

        for (byte[] frame : frames) {
            bb.put(frame);
        }

        bb.flip();
        return bb;
    }

    public void seekAndReadContent(FileChannel source) throws IOException {
        data = ByteBuffer.allocate((int) size);
        source.position(dataOffset);
        source.read(data);
        this.data.flip();
    }

    /**
     * Get the total size of this element
     */
    @Override
    public long getSize() {
        long size = getDataSize();
        size += getEbmlSize(size);
        size += id.length;
        return size;
    }

    public long getDataSize() {
        long size = 0;
        // TODO: one can do same calculation with for(byte[] aFrame :
        // this.frames) size += aFrame.length;
        for (long fsize : frameSizes)
            size += fsize;

        if (lacingPresent) {
            size += muxLacingInfo().length;
            size += 1; // int8 laces count, a.k.a. frame_count-1
        }

        size += 3; // int8 - flags; sint16 - timecode
        size += getEbmlSize(trackNumber);
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

    public static long[] calcEbmlLacingDiffs(long[] laceSizes) {
        int lacesCount = laceSizes.length - 1;
        long[] out = new long[lacesCount];
        out[0] = (int) laceSizes[0];
        for (int i = 1; i < lacesCount; i++) {
            out[i] = laceSizes[i] - laceSizes[i - 1];
        }
        return out;
    }

    public static byte[] muxEbmlLacing(long[] laceSizes) {
        ByteArrayList bytes = new ByteArrayList();

        long[] laceSizeDiffs = calcEbmlLacingDiffs(laceSizes);
        bytes.addAll(Element.ebmlBytes(laceSizeDiffs[0]));

        for (int i = 1; i < laceSizeDiffs.length; i++) {
            bytes.addAll(convertToBytes(laceSizeDiffs[i]));
        }
        return bytes.toArray();
    }

    public static byte[] muxXiphLacing(long[] laceSizes) {
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
