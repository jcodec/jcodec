package org.jcodec.codecs.png;

import static org.jcodec.common.tools.MathUtil.abs;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.zip.CRC32;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import org.jcodec.common.VideoCodecMeta;
import org.jcodec.common.VideoDecoder;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.logging.Logger;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture8Bit;
import org.jcodec.common.model.Size;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * PNG image decoder.
 * 
 * @author Stanislav Vitvitskyy
 * 
 */
public class PNGDecoder extends VideoDecoder {
    private static final long PNGSIG = 0x89504e470d0a1a0aL;
    private static final long MNGSIG = 0x8a4d4e470d0a1a0aL;
    private static final int TAG_IHDR = 0x49484452;
    private static final int TAG_IDAT = 0x49444154;
    private static final int TAG_IEND = 0x49454e44;

    private static final int FILTER_TYPE_LOCO = 64;
    private static final int FILTER_VALUE_NONE = 0;
    private static final int FILTER_VALUE_SUB = 1;
    private static final int FILTER_VALUE_UP = 2;
    private static final int FILTER_VALUE_AVG = 3;
    private static final int FILTER_VALUE_PAETH = 4;
    private static final int FILTER_VALUE_MIXED = 5;

    private static final int PNG_COLOR_MASK_PALETTE = 1;
    private static final int PNG_COLOR_MASK_COLOR = 2;
    private static final int PNG_COLOR_MASK_ALPHA = 4;

    private static final int PNG_COLOR_TYPE_GRAY = 0;
    private static final int PNG_COLOR_TYPE_PALETTE = (PNG_COLOR_MASK_COLOR | PNG_COLOR_MASK_PALETTE);
    private static final int PNG_COLOR_TYPE_RGB = (PNG_COLOR_MASK_COLOR);
    private static final int PNG_COLOR_TYPE_RGB_ALPHA = (PNG_COLOR_MASK_COLOR | PNG_COLOR_MASK_ALPHA);
    private static final int PNG_COLOR_TYPE_GRAY_ALPHA = (PNG_COLOR_MASK_ALPHA);

    @Override
    public Picture8Bit decodeFrame8Bit(ByteBuffer data, byte[][] buffer) {
        long sig = data.getLong();
        if (sig != PNGSIG && sig != MNGSIG)
            throw new RuntimeException("Not a PNG file.");

        IHDR ihdr = null;
        List<ByteBuffer> list = new ArrayList<ByteBuffer>();
        while (data.remaining() >= 8) {
            int length = data.getInt();
            int tag = data.getInt();

            if (data.remaining() < length)
                break;

            switch (tag) {
            case TAG_IHDR:
                ihdr = new IHDR();
                ihdr.parse(data);
                break;
            case TAG_IDAT:
                list.add(NIOUtils.read(data, length));
                NIOUtils.skip(data, 4); // CRC
                break;
            case TAG_IEND:
                NIOUtils.skip(data, 4); // CRC
                break;
            default:
                data.position(data.position() + length + 4);
            }
        }
        try {
            decodeData(ihdr, list, buffer);
        } catch (DataFormatException e) {
            return null;
        }
        return Picture8Bit.createPicture8Bit(ihdr.width, ihdr.height, buffer, ihdr.colorSpace());
    }

    private void decodeData(IHDR ihdr, List<ByteBuffer> list, byte[][] buffer) throws DataFormatException {
        int rowSize = ihdr.rowSize() + 1;
        int bpp = (ihdr.getBitsPerPixel() + 7) >> 3;
        Inflater inflater = new Inflater();
        Iterator<ByteBuffer> it = list.iterator();
        byte[] lastRow = new byte[ihdr.rowSize()];
        byte[] uncompressed = new byte[ihdr.rowSize() + 1];

        for (int row = 0, bptr = 0; row < ihdr.height; row++) {
            int count = inflater.inflate(uncompressed);
            if (count < uncompressed.length && inflater.needsInput()) {
                if (!it.hasNext()) {
                    Logger.warn(String.format("Data truncation at row %d", row));
                    break;
                }
                ByteBuffer next = it.next();
                inflater.setInput(NIOUtils.toArray(next));
                int toRead = uncompressed.length - count;
                count = inflater.inflate(uncompressed, count, toRead);
                if (count != toRead) {
                    Logger.warn(String.format("Data truncation at row %d", row));
                    break;
                }
            }
            int filter = uncompressed[0];
            switch (filter) {
            case FILTER_VALUE_NONE:
                for(int i = 0; i < rowSize - 1; i++) {
                    lastRow[i] = (byte)((uncompressed[i + 1] & 0xff) - 128);
                }
                break;
            case FILTER_VALUE_SUB:
                filterSub(uncompressed, 1, rowSize, lastRow, bpp);
                break;
            case FILTER_VALUE_UP:
                filterUp(uncompressed, 1, rowSize, lastRow);
                break;
            case FILTER_VALUE_AVG:
                filterAvg(uncompressed, 1, rowSize, lastRow, bpp);
                break;
            case FILTER_VALUE_PAETH:
                filterPaeth(uncompressed, 1, rowSize, lastRow, bpp);
                break;
            }

            for (int i = 0; i < rowSize - 1; i += bpp, bptr += bpp) {
                if (ihdr.filterType != FILTER_TYPE_LOCO) {
                    buffer[0][bptr] = lastRow[i];
                    buffer[0][bptr+1] = lastRow[i + 1];
                    buffer[0][bptr+2] = lastRow[i + 2];
                } else {
                    buffer[0][bptr] = (byte) (lastRow[i] + lastRow[i + 1]);
                    buffer[0][bptr+1] = lastRow[i + 1];
                    buffer[0][bptr+2] = (byte) (lastRow[i + 2] + lastRow[i + 1]);
                }
            }
        }
    }

    private void filterPaeth(byte[] uncompressed, int from, int rowSize, byte[] lastRow, int bpp) {
        for (int i = 0; i < bpp; i++) {
            uncompressed[from + i] = (byte) (uncompressed[from + i] + lastRow[i]);
        }
        for (int i = bpp; i < rowSize; i++) {
            int a = uncompressed[from + i - bpp];
            int b = lastRow[i];
            int c = lastRow[i - bpp];
            int p = b - c;
            int pc = a - c;

            int pa = abs(p);
            int pb = abs(pc);
            pc = abs(p + pc);

            if (pa <= pb && pa <= pc)
                p = a;
            else if (pb <= pc)
                p = b;
            else
                p = c;
            lastRow[i] = uncompressed[from + i] = (byte) (p + uncompressed[from + i]);
        }
    }

    private void filterSub(byte[] uncompressed, int from, int rowSize, byte[] lastRow, int bpp) {
        switch (bpp) {
        case 1:
            filterSub1(uncompressed, from, lastRow, rowSize);
            break;
        case 2:
            filterSub2(uncompressed, from, lastRow, rowSize);
            break;
        case 3:
            filterSub3(uncompressed, from, lastRow, rowSize);
            break;
        default:
            filterSub4(uncompressed, from, lastRow, rowSize);
        }
    }

    private void filterAvg(byte[] uncompressed, int from, int rowSize, byte[] lastRow, int bpp) {
        switch (bpp) {
        case 1:
            filterAvg1(uncompressed, from, lastRow, rowSize);
            break;
        case 2:
            filterAvg2(uncompressed, from, lastRow, rowSize);
            break;
        case 3:
            filterAvg3(uncompressed, from, lastRow, rowSize);
            break;
        default:
            filterAvg4(uncompressed, from, lastRow, rowSize);
        }

    }

    private void filterSub1(byte[] uncompressed, int from, byte[] lastRow, int rowSize) {
        byte p = lastRow[0] = uncompressed[from];
        for (int i = 1; i < rowSize; i++) {
            p = lastRow[i] = uncompressed[from + i] = (byte) (p + uncompressed[from + i]);
        }
    }

    private void filterUp(byte[] uncompressed, int from, int rowSize, byte[] lastRow) {
        for (int i = 0; i < rowSize; i++) {
            lastRow[i] = uncompressed[from + i] = (byte) (lastRow[i] + uncompressed[from + i]);
        }
    }

    private void filterAvg1(byte[] uncompressed, int from, byte[] lastRow, int rowSize) {
        byte p = lastRow[0] = (byte) (uncompressed[from] + (lastRow[0] >> 1));
        for (int i = 1; i < rowSize; i++) {
            p = lastRow[i] = uncompressed[from + i] = (byte) (((lastRow[i] + p) >> 1) + uncompressed[from + i]);
        }
    }

    private void filterSub2(byte[] uncompressed, int from, byte[] lastRow, int rowSize) {
        byte p0 = lastRow[0] = uncompressed[from];
        byte p1 = lastRow[1] = uncompressed[from + 1];
        for (int i = 2; i < rowSize; i += 2) {
            p0 = lastRow[i] = uncompressed[from + i] = (byte) (p0 + uncompressed[from + i]);
            p1 = lastRow[i + 1] = uncompressed[from + i + 1] = (byte) (p1 + uncompressed[from + i + 1]);
        }
    }

    private void filterAvg2(byte[] uncompressed, int from, byte[] lastRow, int rowSize) {
        byte p0 = lastRow[0] = (byte) (uncompressed[from] + (lastRow[0] >> 1));
        byte p1 = lastRow[1] = (byte) (uncompressed[from + 1] + (lastRow[1] >> 1));
        for (int i = 2; i < rowSize; i += 2) {
            p0 = lastRow[i] = uncompressed[from + i] = (byte) (((lastRow[i] + p0) >> 1) + uncompressed[from + i]);
            p1 = lastRow[i + 1] = uncompressed[from + i
                    + 1] = (byte) (((lastRow[i + 1] + p1) >> 1) + uncompressed[from + i + 1]);
        }
    }

    private void filterSub3(byte[] uncompressed, int from, byte[] lastRow, int rowSize) {
        byte p0 = lastRow[0] = uncompressed[from];
        byte p1 = lastRow[1] = uncompressed[from + 1];
        byte p2 = lastRow[2] = uncompressed[from + 2];
        for (int i = 3; i < rowSize; i += 3) {
            p0 = lastRow[i] = uncompressed[from + i] = (byte) (p0 + uncompressed[from + i]);
            p1 = lastRow[i + 1] = uncompressed[from + i + 1] = (byte) (p1 + uncompressed[from + i + 1]);
            p2 = lastRow[i + 2] = uncompressed[from + i + 2] = (byte) (p2 + uncompressed[from + i + 2]);
        }
    }

    private void filterAvg3(byte[] uncompressed, int from, byte[] lastRow, int rowSize) {
        byte p0 = lastRow[0] = (byte) (uncompressed[from] + (lastRow[0] >> 1));
        byte p1 = lastRow[1] = (byte) (uncompressed[from + 1] + (lastRow[1] >> 1));
        byte p2 = lastRow[2] = (byte) (uncompressed[from + 2] + (lastRow[2] >> 1));
        for (int i = 3; i < rowSize; i += 3) {
            p0 = lastRow[i] = uncompressed[from + i] = (byte) (((lastRow[i] + p0) >> 1) + uncompressed[from + i]);
            p1 = lastRow[i + 1] = uncompressed[from + i
                    + 1] = (byte) (((lastRow[i + 1] + p1) >> 1) + uncompressed[from + i + 1]);
            p2 = lastRow[i + 2] = uncompressed[from + i
                    + 2] = (byte) (((lastRow[i + 2] + p2) >> 1) + uncompressed[from + i + 2]);
        }
    }

    private void filterSub4(byte[] uncompressed, int from, byte[] lastRow, int rowSize) {
        byte p0 = lastRow[0] = uncompressed[from];
        byte p1 = lastRow[1] = uncompressed[from + 1];
        byte p2 = lastRow[2] = uncompressed[from + 2];
        byte p3 = lastRow[3] = uncompressed[from + 3];
        for (int i = 4; i < rowSize; i += 4) {
            p0 = lastRow[i] = uncompressed[from + i] = (byte) (p0 + uncompressed[from + i]);
            p1 = lastRow[i + 1] = uncompressed[from + i + 1] = (byte) (p1 + uncompressed[from + i + 1]);
            p2 = lastRow[i + 2] = uncompressed[from + i + 2] = (byte) (p2 + uncompressed[from + i + 2]);
            p3 = lastRow[i + 3] = uncompressed[from + i + 3] = (byte) (p3 + uncompressed[from + i + 3]);
        }
    }

    private void filterAvg4(byte[] uncompressed, int from, byte[] lastRow, int rowSize) {
        byte p0 = lastRow[0] = (byte) (uncompressed[from] + (lastRow[0] >> 1));
        byte p1 = lastRow[1] = (byte) (uncompressed[from + 1] + (lastRow[1] >> 1));
        byte p2 = lastRow[2] = (byte) (uncompressed[from + 2] + (lastRow[2] >> 1));
        byte p3 = lastRow[3] = (byte) (uncompressed[from + 3] + (lastRow[3] >> 1));
        for (int i = 4; i < rowSize; i += 4) {
            p0 = lastRow[i] = uncompressed[from + i] = (byte) (((lastRow[i] + p0) >> 1) + uncompressed[from + i]);
            p1 = lastRow[i + 1] = uncompressed[from + i
                    + 1] = (byte) (((lastRow[i + 1] + p1) >> 1) + uncompressed[from + i + 1]);
            p2 = lastRow[i + 2] = uncompressed[from + i
                    + 2] = (byte) (((lastRow[i + 2] + p2) >> 1) + uncompressed[from + i + 2]);
            p3 = lastRow[i + 3] = uncompressed[from + i
                    + 3] = (byte) (((lastRow[i + 3] + p3) >> 1) + uncompressed[from + i + 3]);
        }
    }

    private static class IHDR {
        private int width;
        private int height;
        private byte bitDepth;
        private byte colorType;
        private byte compressionType;
        private byte filterType;
        private byte interlaceType;

        public void parse(ByteBuffer data) {
            width = data.getInt();
            height = data.getInt();
            bitDepth = data.get();
            colorType = data.get();
            compressionType = data.get();
            filterType = data.get();
            interlaceType = data.get();
            data.getInt();
        }

        public int rowSize() {
            return (width * getBitsPerPixel() + 7) >> 3;
        }

        public void write(ByteBuffer data) {
            ByteBuffer from = data.duplicate();
            data.putInt(width);
            data.putInt(height);
            data.put(bitDepth);
            data.put(colorType);
            data.put(compressionType);
            data.put(filterType);
            data.put(interlaceType);
            data.putInt(crc32(data, from));
        }

        private int crc32(ByteBuffer data, ByteBuffer dup) {
            CRC32 crc32 = new CRC32();
            crc32.update(NIOUtils.toArray(NIOUtils.read(dup, data.position() - dup.position())));
            return (int) crc32.getValue();
        }

        public int getNBChannels() {
            int channels;
            channels = 1;
            if ((colorType & (PNG_COLOR_MASK_COLOR | PNG_COLOR_MASK_PALETTE)) == PNG_COLOR_MASK_COLOR)
                channels = 3;
            if ((colorType & PNG_COLOR_MASK_ALPHA) != 0)
                channels++;
            return channels;
        }

        public int getBitsPerPixel() {
            return bitDepth * getNBChannels();
        }

        public ColorSpace colorSpace() {
            return ColorSpace.RGB;
        }
    }

    private static int[] passMin = new int[] { 0, 4, 0, 2, 0, 1, 0 };

    private static int[] passShift = new int[] { 3, 3, 2, 2, 1, 1, 0 };

    int passRowSize(int pass, IHDR ihdr) {
        int shift, xmin, pass_width;

        xmin = passMin[pass];
        if (ihdr.width <= xmin)
            return 0;
        shift = passShift[pass];
        pass_width = (ihdr.width - xmin + (1 << shift) - 1) >> shift;
        return (pass_width * ihdr.bitDepth + 7) >> 3;
    }

    @Override
    public VideoCodecMeta getCodecMeta(ByteBuffer data) {
        long sig = data.getLong();
        if (sig != PNGSIG && sig != MNGSIG)
            throw new RuntimeException("Not a PNG file.");

        while (data.remaining() >= 8) {
            int length = data.getInt();
            int tag = data.getInt();

            if (data.remaining() < length)
                break;

            switch (tag) {
            case TAG_IHDR:
                IHDR ihdr = new IHDR();
                ihdr.parse(data);
                return new VideoCodecMeta(new Size(ihdr.width, ihdr.height), ColorSpace.RGB);
            default:
                data.position(data.position() + length + 4);
            }
        }
        return null;
    }

    public static int probe(ByteBuffer data) {
        long sig = data.getLong();
        if (sig == PNGSIG && sig == MNGSIG)
            return 100;
        return 0;
    }

    public static byte[] compress(byte[] data) throws IOException {
        Deflater deflater = new Deflater();
        deflater.setInput(data);
        ByteArrayOutputStream baos = new ByteArrayOutputStream(data.length);
        deflater.finish();
        byte[] buffer = new byte[1024];
        while (!deflater.finished()) {
            int count = deflater.deflate(buffer); // returns the generated
                                                  // code... index
            baos.write(buffer, 0, count);
        }
        return baos.toByteArray();
    }

    public static byte[] deflate(byte[] data, Inflater inflater) throws DataFormatException {
        inflater.setInput(data);
        ByteArrayOutputStream baos = new ByteArrayOutputStream(data.length);
        byte[] buffer = new byte[1 << 14];
        while (!inflater.needsInput()) {
            int count = inflater.inflate(buffer);
            baos.write(buffer, 0, count);
            System.out.println(baos.size());
        }
        return baos.toByteArray();
    }
}
