package org.jcodec.player.util;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class ThreadUtil {

    public static <E> void surePut(BlockingQueue<E> q, E el) {
        while (true) {
            try {
                q.put(el);
                break;
            } catch (InterruptedException e) {
            }
        }
    }

    public static <T> T sureTake(BlockingQueue<T> audio) {
        while (true) {
            try {
                return audio.take();
            } catch (InterruptedException e) {
            }
        }
    }

    public static void sleepNoShit(long nanos) {
        if (nanos < 0)
            return;
        try {
            Thread.sleep(nanos / 1000000, (int) (nanos % 1000000));
        } catch (InterruptedException e) {
        }
    }

    public static void waitNoShit(Object o, int timeout) {
        try {
            o.wait(timeout);
        } catch (InterruptedException e) {
        }
    }

    public static void joinForSure(Thread thread) {
        while (true) {
            try {
                thread.join();
                break;
            } catch (InterruptedException e) {
            }
        }
    }

    public static void waitNoShit(Object o) {
        try {
            o.wait();
        } catch (InterruptedException e) {
        }
    }

    public static <T> T take(BlockingQueue<T> videoDrain, int ms) {
        try {
            return videoDrain.poll(ms, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            return null;
        }
    }
}
