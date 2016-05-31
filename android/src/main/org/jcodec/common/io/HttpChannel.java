package org.jcodec.common.io;

import android.util.Log;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * This class implements a read-only SeekableBytChannel for downloading a HTTP
 * resource.
 *
 * Basically it wraps a URLConnection. It provides random access functionality
 * by closing the connection and re-establishing a new one with HTTP 1.1 Range
 * header to download a stream from a certain offset.
 *
 * This class is the dummiest possible implementation and lacks some important
 * features: - caching; - re-connecting vs reading ahead for forward seeks; -
 * readahead and grouping of the small reads.
 * 
 * @author The JCodec project
 */
public class HttpChannel implements SeekableByteChannel {
    private static final String LOG_TAG_HTTP_CHANNEL = "HttpChannel";

    private URL url;
    private ReadableByteChannel ch;
    private long pos;
    private long length;

    public HttpChannel(URL url) {
        this.url = url;
    }

    @Override
    public long position() throws IOException {
        return pos;
    }

    @Override
    public SeekableByteChannel setPosition(long newPosition) throws IOException {
        if (newPosition == pos)
            return this;
        if (ch != null) {
            ch.close();
            ch = null;
        }
        pos = newPosition;
        Log.d(LOG_TAG_HTTP_CHANNEL, "Seeking to: " + newPosition);
        return this;
    }

    @Override
    public long size() throws IOException {
        return length;
    }

    @Override
    public SeekableByteChannel truncate(long size) throws IOException {
        throw new IOException("Truncate on HTTP is not supported.");
    }

    @Override
    public int read(ByteBuffer buffer) throws IOException {
        ensureOpen();
        int read = ch.read(buffer);
        if (read != -1)
            pos += read;
        return read;
    }

    @Override
    public int write(ByteBuffer buffer) throws IOException {
        throw new IOException("Write to HTTP is not supported.");
    }

    @Override
    public boolean isOpen() {
        return ch != null && ch.isOpen();
    }

    @Override
    public void close() throws IOException {
        ch.close();
    }

    private void ensureOpen() throws IOException {
        if (ch == null) {
            URLConnection connection = url.openConnection();
            if (pos > 0)
                connection.addRequestProperty("Range", "bytes=" + pos + "-");
            ch = Channels.newChannel(connection.getInputStream());
            String resp = connection.getHeaderField("Content-Range");
            if (resp != null) {
                Log.d(LOG_TAG_HTTP_CHANNEL, resp);
                length = Long.parseLong(resp.split("/")[1]);
            } else {
                resp = connection.getHeaderField("Content-Length");
                Log.d(LOG_TAG_HTTP_CHANNEL, resp);
                length = Long.parseLong(resp);
            }
        }
    }
}
