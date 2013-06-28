package org.jcodec.movtool.streaming.tracks;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.jcodec.common.NIOUtils;
import org.jcodec.common.SeekableByteChannel;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * A pool of open file references used to read data
 * 
 * @author The JCodec project
 * 
 */
public class FilePool {
    private BlockingQueue<SeekableByteChannel> channels;
    private List<SeekableByteChannel> allChannels = Collections.synchronizedList(new ArrayList<SeekableByteChannel>());
    private File file;
    private int max;

    public FilePool(File file, int max) {
        this.file = file;
        this.max = max;
        this.channels = new LinkedBlockingQueue<SeekableByteChannel>();
    }

    public SeekableByteChannel getChannel() throws IOException {

        SeekableByteChannel channel = channels.poll();
        if (channel == null) {
            System.out.println("NO CHANNEL");
            if (allChannels.size() < max) {
                channel = newChannel(file);
                allChannels.add(channel);
                System.out.println("CHANNELS: " + allChannels.size() + "(" + max + ")");
            } else {
                while (true) {
                    try {
                        channel = channels.take();
                        break;
                    } catch (InterruptedException e) {
                    }
                }
            }
        }
        return new PoolChannel(channel);
    }

    protected SeekableByteChannel newChannel(File file) throws FileNotFoundException {
        return NIOUtils.readableFileChannel(file);
    }

    public class PoolChannel extends SeekableByteChannelWrapper {
        public PoolChannel(SeekableByteChannel src) throws IOException {
            super(src);
            src.position(0);
        }

        public boolean isOpen() {
            return src != null;
        }

        public void close() throws IOException {
            SeekableByteChannel ret = src;
            src = null;
            while (true) {
                try {
                    channels.put(ret);
                    break;
                } catch (InterruptedException e) {
                }
            }
        }
    }

    public void close() {
        while (!allChannels.isEmpty()) {
            SeekableByteChannel channel = allChannels.remove(0);
            if (channel != null) {
                try {
                    channel.close();
                } catch (IOException e) {
                }
                System.out.println("CLOSED FILE");
            }
        }
        System.out.println("FILE POOL CLOSED!!!");
    }
}
