package org.jcodec.common;

import java.util.concurrent.Callable;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public interface PriorityCallable<T> extends Callable<T> {
    
    int getPriority();

}