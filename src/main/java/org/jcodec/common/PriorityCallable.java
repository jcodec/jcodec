package org.jcodec.common;

import java.util.concurrent.Callable;

public interface PriorityCallable<T> extends Callable<T> {
    
    int getPriority();

}