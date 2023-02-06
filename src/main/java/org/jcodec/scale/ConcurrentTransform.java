package org.jcodec.scale;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.jcodec.common.model.Picture;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public abstract class ConcurrentTransform implements Transform {

	/** The processor count. */
	private static final int PROCESSOR_COUNT = Runtime.getRuntime().availableProcessors();

	/** The processor count. */
	private static final boolean FORCE_SINGLE_THREAD = System.getProperty("forceNonThreadedTransforms") == null ? false : true;
    
	
	/** The executor pool for this operation. */
    private final ExecutorService tp;

    /** Indicates if this is actually a concurrent operation. */
	private final boolean isThreaded;
	
	
	/**
	 * Create a transform with a name.
	 * 
	 * @param name The name that will be used to identify the transform.
	 */
    public ConcurrentTransform(final String name) {
    	
    	this.isThreaded = !FORCE_SINGLE_THREAD && PROCESSOR_COUNT > 1;
        
        if (isThreaded) {
            tp = Executors.newFixedThreadPool(PROCESSOR_COUNT, 
        		new ThreadFactory() {
            	
            		private final AtomicInteger count = new AtomicInteger(1);
            		
	                public Thread newThread(Runnable r) {
	                	Thread t = new Thread(r, "jcodec-" + name + "-transform-" + count.getAndIncrement());
	                    t.setDaemon(true);
	                    return t;
	                }
	            });
        }
        else
        {
        	tp = null;
        }
    }
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final void transform(final Picture src, final Picture dst) 
    {
//    	long start = System.nanoTime();

    	final List<Runnable> tasks = getTasks(isThreaded, src, dst);
    	if (isThreaded)
    	{
    		CountDownLatch countdown = new CountDownLatch(tasks.size());
    		for (Runnable task : tasks)
    		{
    			tp.submit(new TransformTask(countdown, task));
//    			System.out.println("************** submitted tasl");
    		}
    		
    		try {
	    		// Wait for all the tasks to execute.
	    		countdown.await();
    		}
    		catch (InterruptedException e) {
    			throw new RuntimeException(e);
    		}
    		
//    		System.out.println("************** COMPLETE");
    	}
    	else
    	{
    		// No concurrency, so just execute the tasks.
    		// JAVA 8: tasks.forEach(task -> task.run());
    		for (Runnable task : tasks)
    		{
    			task.run();
    		}
    	}
    	
//        System.out.println("Concurrent - " + isThreaded + ", Time in ns to complete - " + (System.nanoTime() - start));
    }
	
    
    
    protected abstract List<Runnable> getTasks(final boolean willBeConcurrent, final Picture src, final Picture dst);
    
    
    
    private static class TransformTask implements Runnable {
    	
    	private final CountDownLatch countdown;
    	
    	private final Runnable task;
    	
    	private TransformTask(final CountDownLatch countdown, final Runnable task) {
    		
    		this.countdown = countdown;
    		this.task = task;
    	}
    	
		@Override
		public void run() {
			try {
				task.run();
			}
			finally {
				countdown.countDown();
			}
		}
    	
    }
    
}
