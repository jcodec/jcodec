package org.jcodec.platform;
import js.lang.IllegalArgumentException;
import js.lang.IllegalStateException;
import js.lang.Comparable;
import js.lang.StringBuilder;
import js.lang.System;
import js.lang.Runtime;
import js.lang.Runnable;
import js.lang.Process;
import js.lang.ThreadLocal;
import js.lang.IndexOutOfBoundsException;
import js.lang.Thread;
import js.lang.NullPointerException;

import js.io.IOException;
import js.io.InputStream;

public abstract class BaseInputStream extends InputStream {

    protected abstract int readByte() throws IOException;

    protected abstract int readBuffer(byte[] b, int from, int len) throws IOException;

}
