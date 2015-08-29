package org.jcodec.common;

import java.util.Comparator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class PriorityFuture<T> implements RunnableFuture<T> {

    private RunnableFuture<T> src;
    private int priority;

    public PriorityFuture(RunnableFuture<T> other, int priority) {
        this.src = other;
        this.priority = priority;
    }

    public int getPriority() {
        return priority;
    }

    public boolean cancel(boolean mayInterruptIfRunning) {
        return src.cancel(mayInterruptIfRunning);
    }

    public boolean isCancelled() {
        return src.isCancelled();
    }

    public boolean isDone() {
        return src.isDone();
    }

    public T get() throws InterruptedException, ExecutionException {
        return src.get();
    }

    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return src.get();
    }

    public void run() {
        src.run();
    }

    public static Comparator<Runnable> COMP = new Comparator<Runnable>() {
        public int compare(Runnable o1, Runnable o2) {
            if (o1 == null && o2 == null)
                return 0;
            else if (o1 == null)
                return -1;
            else if (o2 == null)
                return 1;
            else {
                int p1 = ((PriorityFuture<?>) o1).getPriority();
                int p2 = ((PriorityFuture<?>) o2).getPriority();

                return p1 > p2 ? 1 : (p1 == p2 ? 0 : -1);
            }
        }
    };
}
