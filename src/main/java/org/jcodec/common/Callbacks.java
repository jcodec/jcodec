package org.jcodec.common;

/**
 * Defines standard callbacl types
 * 
 * @author Stanislav Vitvitskyy
 */
public class Callbacks {
    public static interface Callback0Into0 {
        public void call();
    }

    public static interface Callback0Into1<R0> {
        public R0 call();
    }

    public static interface Callback1Into0<A0> {
        public void call(A0 arg);
    }

    public static interface Callback1Into1<R0, A0> {
        public R0 call(A0 arg);
    }

}
