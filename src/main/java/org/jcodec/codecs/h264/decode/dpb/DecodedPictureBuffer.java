package org.jcodec.codecs.h264.decode.dpb;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author Jay Codec
 * 
 */
public class DecodedPictureBuffer implements Iterable<DecodedPicture> {
    private DecodedPicture[] buf;
    private int size;

    private static Map<Integer, Integer> bufferSize;
    static {
        bufferSize = new HashMap<Integer, Integer>();

        bufferSize.put(10, 297);
        bufferSize.put(11, 675);
        bufferSize.put(12, 1783);
        bufferSize.put(13, 1783);
        bufferSize.put(20, 1783);
        bufferSize.put(21, 3564);
        bufferSize.put(22, 6075);
        bufferSize.put(30, 6075);
        bufferSize.put(31, 13500);
        bufferSize.put(32, 15360);
        bufferSize.put(40, 24576);
        bufferSize.put(41, 24576);
        bufferSize.put(42, 26112);
        bufferSize.put(50, 82800);
        bufferSize.put(51, 138240);
    };

    public static DecodedPictureBuffer getForProfileAndDimension(int level, int picSizeInMbs) {
        int size = 512 * bufferSize.get(level) / (picSizeInMbs * 384);
        if (size < 16)
            size = 16;
        return new DecodedPictureBuffer(size);
    }

    public DecodedPictureBuffer(int refListSize) {
        this.size = 0;
        this.buf = new DecodedPicture[refListSize];
    }

    public void bumpPictures() {
        int iW, iR;

        for (iR = 0, iW = 0; iR < size; ++iR) {
            boolean leave = (buf[iR] != null) && (buf[iR].isDisplay() || buf[iR].isRef());
            if (leave) {
                buf[iW++] = buf[iR];
            }
        }
        size = iW;

        for (; iW < buf.length; iW++) {
            buf[iW] = null;
        }
    }

    public void add(DecodedPicture picture) {

        buf[size++] = picture;
    }

    public Iterator<DecodedPicture> iterator() {
        return new DPBIterator(buf, size);
    }

    public class DPBIterator implements Iterator<DecodedPicture> {
        private DecodedPicture[] copy;
        private int size;
        private int pointer = -1;

        private DPBIterator(DecodedPicture[] copy, int size) {
            this.copy = copy;
            this.size = size;
        }

        public boolean hasNext() {
            return pointer < size - 1;
        }

        public DecodedPicture next() {
            if (!hasNext())
                throw new IllegalStateException("End of collection");
            return copy[++pointer];
        }

        public void remove() {
            throw new UnsupportedOperationException("Remove has no sematics with this type of iterator");
        }
    }

    public boolean isFull() {
        return size >= buf.length;
    }
}