package org.jcodec.codecs.mjpeg;

import static org.jcodec.codecs.mjpeg.JpegConst.Type.CAC;
import static org.jcodec.codecs.mjpeg.JpegConst.Type.CDC;
import static org.jcodec.codecs.mjpeg.JpegConst.Type.YAC;
import static org.jcodec.codecs.mjpeg.JpegConst.Type.YDC;
import static org.jcodec.codecs.mjpeg.JpegUtils.zigzagDecode;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.CountingInputStream;
import org.jcodec.codecs.mjpeg.tools.Asserts;
import org.jcodec.codecs.wav.StringReader;
import org.jcodec.common.io.VLCBuilder;
import org.jcodec.common.tools.Debug;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author Jay Codec
 * 
 */
public class JpegParser {
    public CodedImage parse(InputStream is) throws IOException {
        CountingInputStream counter = new CountingInputStream(is);
        return parse(new PushbackInputStream(counter, 2), counter);
    }

    public CodedImage parse(PushbackInputStream is, CountingInputStream counter) throws IOException {
        CodedImage image = new CodedImage();
        int curQTable = 0;
        while (true) {
            int marker = is.read();
            if (marker == -1)
                return image;
            if (marker == 0)
                continue;
            if (marker != 0xFF)
                throw new RuntimeException("@" + Long.toHexString(counter.getByteCount()) + " Marker expected: 0x"
                        + Integer.toHexString(marker));

            int b = is.read();
            Debug.trace("%s", Markers.toString(b));
            switch (b) {
            case Markers.SOF0:
                image.frame = FrameHeader.read(is);
                Debug.trace("    %s", image.frame);
                break;
            case Markers.DHT:
                int len1 = readShort(is);
                CountingInputStream cis = new CountingInputStream(is);
                while (cis.getCount() < len1 - 2) {
                    readHuffmanTable(cis, image);
                }
                break;
            case Markers.DQT:
                int len4 = readShort(is);
                CountingInputStream cis1 = new CountingInputStream(is);
                while (cis1.getCount() < len4 - 2) {
                    QuantTable quantTable = readQuantTable(cis1);
                    if (curQTable == 0)
                        image.setQuantLum(quantTable);
                    else
                        image.setQuantChrom(quantTable);
                    curQTable++;
                }
                break;
            case Markers.SOS:
                if (image.scan != null) {
                    throw new IllegalStateException("unhandled - more than one scan header");
                }
                image.scan = ScanHeader.read(is);
                Debug.trace("    %s", image.scan);
                image.setData(readData(is));
                break;
            case Markers.SOI:
                break;
            case Markers.EOI:
                return image;
            case Markers.APP0:
                // int len10 = readShort(is);
                // byte[] id = new byte[4];
                // is.read(id);
                // if (!Arrays.equals(JFIF, id))
                // throw new RuntimeException("Not a JFIF file");
                // is.skip(1);
                //
                // is.skip(2);
                // int units = is.read();
                // int dx = readShort(is);
                // int dy = readShort(is);
                // int tx = is.read();
                // int ty = is.read();
                // is.skip(tx * ty * 3);
                // break;
            case Markers.APP1:
            case Markers.APP2:
            case Markers.APP3:
            case Markers.APP4:
            case Markers.APP5:
            case Markers.APP6:
            case Markers.APP7:
            case Markers.APP8:
            case Markers.APP9:
            case Markers.APPA:
            case Markers.APPB:
            case Markers.APPC:
            case Markers.APPD:
            case Markers.APPE:
            case Markers.APPF:
                int len3 = readShort(is);
                StringReader.sureSkip(is, len3 - 2);
                break;
            case Markers.DRI:
                /*
                 * Lr: Define restart interval segment length – Specifies the
                 * length of the parameters in the DRI segment shown in Figure
                 * B.9 (see B.1.1.4).
                 */
                int lr = readShort(is);
                // Ri: Restart interval – Specifies the number of MCU in the
                // restart interval.
                int ri = readShort(is);
                Debug.trace("DRI Lr: %d Ri: %d", lr, ri);
                // A DRI marker segment with Ri equal to zero shall disable
                // restart intervals for the following scans.
                Asserts.assertEquals(0, ri);
                break;
            default: {
                throw new IllegalStateException("unhandled marker " + Markers.toString(b));
            }
            }
        }
    }

    /**
     * Decodes huffman tables
     * 
     * @param image
     * 
     * @param orig
     * @return
     * @throws IOException
     */
    private void readHuffmanTable(InputStream is, CodedImage image) throws IOException {
        VLCBuilder builder = new VLCBuilder();

        int tableNo = is.read();
        byte[] levelSizes = new byte[16];
        is.read(levelSizes);

        int levelStart = 0;
        for (int i = 0; i < 16; i++) {
            int length = levelSizes[i] & 0xff;
            for (int c = 0; c < length; c++) {
                int val = is.read();
                int code = levelStart++;
                builder.set(code, i + 1, val);
            }
            levelStart <<= 1;
        }

        if (tableNo == YDC.getValue()) {
            image.setYdc(builder.getVLC());
        } else if (tableNo == CDC.getValue()) {
            image.setCdc(builder.getVLC());
        } else if (tableNo == YAC.getValue()) {
            image.setYac(builder.getVLC());
        } else if( tableNo == CAC.getValue()){
            image.setCac(builder.getVLC());
        } else {
            throw new RuntimeException("Unsupported huffman table index: " + tableNo);
        }
    }

    private QuantTable readQuantTable(InputStream is) throws IOException {
        int ind = is.read();

        int[] result = new int[64];
        for (int i = 0; i < 64; i++) {
            result[i] = is.read();
        }
        return new QuantTable(ind, zigzagDecode(result));
    }

    private byte[] readData(PushbackInputStream is) throws IOException {
        return IOUtils.toByteArray(new ByteStuffingInputStream(is));
    }

    private int readShort(InputStream is) throws IOException {
        int b1 = is.read();
        int b2 = is.read();

        return (b1 << 8) + b2;
    }

}
