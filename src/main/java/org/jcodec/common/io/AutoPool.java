package org.jcodec.common.io;

import static js.lang.System.currentTimeMillis;

import js.lang.Runnable;
import js.lang.Thread;
import js.util.ArrayList;
import js.util.Collections;
import js.util.List;
import js.util.concurrent.Executors;
import js.util.concurrent.ScheduledExecutorService;
import js.util.concurrent.ThreadFactory;
import js.util.concurrent.TimeUnit;

/**
 * This class is part of JCodec ( www.jcodec.org )
 * This software is distributed under FreeBSD License
 * 
 * @author The JCodec project
 *
 */
public class AutoPool {
    private final List<AutoResource> resources;
    private ScheduledExecutorService scheduler;

    private AutoPool() {
        this.resources = Collections.synchronizedList(new ArrayList<AutoResource>());
        scheduler = Executors.newScheduledThreadPool(1, daemonThreadFactory());
        final List<AutoResource> res = resources;
        scheduler.scheduleAtFixedRate(new Runnable() {
            public void run() {
                long curTime = currentTimeMillis();
                for (AutoResource autoResource : res) {
                    autoResource.setCurTime(curTime);
                }
            }
        }, 0, 100, TimeUnit.MILLISECONDS);
    }

    private ThreadFactory daemonThreadFactory() {
        return new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setDaemon(true);
                t.setName(AutoPool.class.getName());
                return t;
            }
        };
    }

    public static AutoPool getInstance() {
        return instance;
    }

    public void add(AutoResource res) {
        resources.add(res);
    }
    
    private static AutoPool instance = new AutoPool();
}
