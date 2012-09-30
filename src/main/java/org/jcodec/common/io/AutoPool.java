package org.jcodec.common.io;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * This class is part of JCodec ( www.jcodec.org )
 * This software is distributed under FreeBSD License
 * 
 * @author The JCodec project
 *
 */
public class AutoPool {
    private static AutoPool instance = new AutoPool();
    private List<AutoResource> resources = Collections.synchronizedList(new ArrayList<AutoResource>());
    private ScheduledExecutorService scheduler;

    private AutoPool() {
        scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(new Runnable() {
            public void run() {
                long curTime = System.currentTimeMillis();
                List<AutoResource> res = resources;
                for (AutoResource autoResource : res) {
                    autoResource.setCurTime(curTime);
                }
            }
        }, 0, 100, TimeUnit.MILLISECONDS);
    }

    public static AutoPool getInstance() {
        return instance;
    }

    public void add(AutoResource res) {
        resources.add(res);
    }
}
