package org.jcodec.codecs.png;

import java.nio.ByteBuffer;
import java.util.zip.CRC32;
import java.util.zip.Deflater;

import org.jcodec.common.VideoEncoder;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.common.tools.MainUtils;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Simplistic PNG encoder, doesn't support anything.
 * 
 * @author Stanislav Vitvitskyy
 * 
 */
public class PNGEncoder extends VideoEncoder {
    private static final long PNGSIG = 0x89504e470d0a1a0aL;
    private static final int TAG_IHDR = 0x49484452;
    private static final int TAG_IDAT = 0x49444154;
    private static final int TAG_IEND = 0x49454e44;

    private static class IHDR {
        private int width;
        private int height;
        private byte bitDepth;
        private byte colorType;
        private byte compressionType;
        private byte filterType;
        private byte interlaceType;

        public void write(ByteBuffer data) {
            data.putInt(width);
            data.putInt(height);
            data.put(bitDepth);
            data.put(colorType);
            data.put(compressionType);
            data.put(filterType);
            data.put(interlaceType);
        }
    }

    private static int crc32(ByteBuffer from, ByteBuffer to) {
        from.limit(to.position());
        
        CRC32 crc32 = new CRC32();
        crc32.update(NIOUtils.toArray(from));
        return (int) crc32.getValue();
    }

    private static final int PNG_COLOR_MASK_COLOR = 2;

    @Override
    public EncodedFrame encodeFrame(Picture pic, ByteBuffer out) {
        ByteBuffer _out = out.duplicate();
        _out.putLong(PNGSIG);
        IHDR ihdr = new IHDR();
        ihdr.width = pic.getCroppedWidth();
        ihdr.height = pic.getCroppedHeight();
        ihdr.bitDepth = 8;
        ihdr.colorType = PNG_COLOR_MASK_COLOR;
        _out.putInt(13);
        
        ByteBuffer crcFrom = _out.duplicate();
        _out.putInt(TAG_IHDR);
        ihdr.write(_out);
        _out.putInt(crc32(crcFrom, _out));
        
        Deflater deflater = new Deflater();
        byte[] rowData = new byte[pic.getCroppedWidth() * 3 + 1];
        byte[] pix = pic.getPlaneData(0);
        byte[] buffer = new byte[1 << 15];
        int ptr = 0, len = buffer.length;

        // We do one extra iteration here to flush the deflator
        int lineStep = (pic.getWidth() - pic.getCroppedWidth()) * 3;
        for (int row = 0, bptr = 0; row < pic.getCroppedHeight() + 1; row++) {
            int count;
            while ((count = deflater.deflate(buffer, ptr, len)) > 0) {
                ptr += count;
                len -= count;

                if (len == 0) {
                    _out.putInt(ptr);
                    crcFrom = _out.duplicate();
                    _out.putInt(TAG_IDAT);
                    _out.put(buffer, 0, ptr);
                    _out.putInt(crc32(crcFrom, _out));
                    ptr = 0;
                    len = buffer.length;
                }
            }

            if (row >= pic.getCroppedHeight())
                break;

            rowData[0] = 0; // no filter
            for (int i = 1; i <= pic.getCroppedWidth() * 3; i += 3, bptr += 3) {
                rowData[i] = (byte) (pix[bptr] + 128);
                rowData[i + 1] = (byte) (pix[bptr + 1] + 128);
                rowData[i + 2] = (byte) (pix[bptr + 2] + 128);
            }
            bptr += lineStep;
            deflater.setInput(rowData);
            if (row >= pic.getCroppedHeight() - 1)
                deflater.finish();
        }
        if (ptr > 0) {
            _out.putInt(ptr);
            crcFrom = _out.duplicate();
            _out.putInt(TAG_IDAT);
            _out.put(buffer, 0, ptr);
            _out.putInt(crc32(crcFrom, _out));
        }
        _out.putInt(0);
        _out.putInt(TAG_IEND);
        _out.putInt(0xae426082);
        _out.flip();

        return new EncodedFrame(_out, true);
    }

    @Override
    public ColorSpace[] getSupportedColorSpaces() {
        return new ColorSpace[] { ColorSpace.RGB };
    }

    @Override
    public int estimateBufferSize(Picture frame) {
        return frame.getCroppedWidth() * frame.getCroppedHeight() * 4;
    }
}
