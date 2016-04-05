package org.jcodec.movtool.streaming.tracks;
import js.lang.IllegalStateException;
import js.lang.System;


import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;

import js.io.File;
import js.io.FileNotFoundException;
import js.io.IOException;
import js.lang.InterruptedException;
import js.util.ArrayList;
import js.util.Collections;
import js.util.List;
import js.util.concurrent.BlockingQueue;
import js.util.concurrent.LinkedBlockingQueue;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * A pool of open file references used to read data
 * 
 * @author The JCodec project
 * 
 */
public class FilePool implements ByteChannelPool {
    private BlockingQueue<SeekableByteChannel> channels;
    private List<SeekableByteChannel> allChannels;
    private File file;
    private int max;

    public FilePool(File file, int max) {
        this.file = file;
        this.max = max;
        this.channels = new LinkedBlockingQueue<SeekableByteChannel>();
        this.allChannels = Collections.synchronizedList(new ArrayList<SeekableByteChannel>());
    }

    @Override
    public SeekableByteChannel getChannel() throws IOException {

        SeekableByteChannel channel = channels.poll();
        if (channel == null) {
//            System.out.println("NO CHANNEL");
            if (allChannels.size() < max) {
                channel = newChannel(file);
                allChannels.add(channel);
//                System.out.println("CHANNELS: " + allChannels.size() + "(" + max + ")");
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
        return new PoolChannel(this, channel);
    }

    protected SeekableByteChannel newChannel(File file) throws FileNotFoundException {
        return NIOUtils.readableChannel(file);
    }

    public static class PoolChannel extends SeekableByteChannelWrapper {
        private FilePool pool;

		public PoolChannel(FilePool pool, SeekableByteChannel src) throws IOException {
            super(src);
			this.pool = pool;
            src.setPosition(0);
        }

        public boolean isOpen() {
            return src != null;
        }

        public void close() throws IOException {
            SeekableByteChannel ret = src;
            src = null;
            while (true) {
                try {
                	pool.channels.put(ret);
                    break;
                } catch (InterruptedException e) {
                }
            }
        }
    }

    @Override
    public void close() {
        while (!allChannels.isEmpty()) {
            SeekableByteChannel channel = allChannels.remove(0);
            if (channel != null) {
                try {
                    channel.close();
                } catch (IOException e) {
                }
//                System.out.println("CLOSED FILE");
            }
        }
//        System.out.println("FILE POOL CLOSED!!!");
    }
}
