package org.jcodec.common.io;

import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Implements a tiled read. Designed to be wrapped around HttpChannel for the
 * optimal media container access over HTTP.
 * 
 * @author The JCodec project
 */
public class TiledChannel implements SeekableByteChannel {
    private static final String LOG_TAG_TILED_CHANNEL = "TiledChannel";

    private static final int TILE_CAPACITY = 512 << 10; // 512K

    private SeekableByteChannel ch;
    private Tile cur;
    private long pos;

    private class Tile {
        private byte[] data;
        private long tileStart;
        private int tileLength;

        public Tile() {
            this.data = new byte[TILE_CAPACITY];
        }

        public boolean in(long newPosition) {
            return newPosition >= tileStart && newPosition < tileStart + tileLength;
        }

        public void reset(long newStart) {
            tileLength = 0;
            tileStart = newStart;
        }

        public int readTo(ByteBuffer buffer) {
            int tilePos = (int) (pos - tileStart);
            int tileRemaining = Math.max(0, tileLength - tilePos);
            int toRead = 0;
            if (tileRemaining > 0) {
                toRead = Math.min(buffer.remaining(), tileRemaining);
                buffer.put(data, tilePos, toRead);
            }
            return toRead;
        }

        public boolean eof() {
            return tileLength == -1;
        }

        public void fetch(ReadableByteChannel ch) throws IOException {
            ByteBuffer wrap = ByteBuffer.wrap(data);
            int r = 0;
            while (wrap.hasRemaining()) {
                r = ch.read(wrap);
                if (r == -1) {
                    break;
                }
            }
            tileStart += tileLength;
            int read = data.length - wrap.remaining();
            tileLength = read == 0 && r == -1 ? -1 : read;
            Log.d(LOG_TAG_TILED_CHANNEL, "Tile " + tileStart + " - " + (tileStart + tileLength));
        }
    }

    public TiledChannel(SeekableByteChannel ch) {
        this.ch = ch;
        this.cur = new Tile();
    }

    @Override
    public long position() throws IOException {
        return pos;
    }

    @Override
    public SeekableByteChannel setPosition(long newPosition) throws IOException {
        if (newPosition > size()) {
            newPosition = size();
        }
        if (newPosition < 0) {
            newPosition = 0;
        }
        pos = newPosition;
        if (cur.in(newPosition)) {
            return this;
        }
        long tileStart = newPosition - (newPosition % TILE_CAPACITY);
        cur.reset(tileStart);
        ch.setPosition(tileStart);
        Log.d(LOG_TAG_TILED_CHANNEL, "Seeking to: " + newPosition + ", tile @" + cur.tileStart);
        return this;
    }

    @Override
    public long size() throws IOException {
        return ch.size();
    }

    @Override
    public SeekableByteChannel truncate(long size) throws IOException {
        throw new IOException("Truncate on HTTP is not supported.");
    }

    @Override
    public int read(ByteBuffer buffer) throws IOException {
        if (cur.eof())
            return -1;
        int startRem = buffer.remaining();
        do {
            int read = cur.readTo(buffer);
            if (buffer.hasRemaining()) {
                cur.fetch(ch);
                if (cur.eof())
                    break;
            }
            pos += read;
        } while (buffer.hasRemaining());

        int read = startRem - buffer.remaining();
        Log.d(LOG_TAG_TILED_CHANNEL, "Read: " + read);
        return read == 0 && cur.eof() ? -1 : read;
    }

    @Override
    public int write(ByteBuffer buffer) throws IOException {
        throw new IOException("Write to HTTP is not supported.");
    }

    @Override
    public boolean isOpen() {
        return ch.isOpen();
    }

    @Override
    public void close() throws IOException {
        ch.close();
    }
}