package org.jcodec.containers;
import org.junit.Test;

import java.lang.System;
import java.nio.ByteBuffer;

public class ByteBufferManipulationTest {

    @Test
    public void test() {
        int size = 64;
        ByteBuffer b1 = ByteBuffer.allocate(128);
        b1.position(2);
        printBuffer(b1, "ori");
        
        ByteBuffer b2 = b1.duplicate();
        b2.limit(64);
        printBuffer(b2, "dup");
        printBuffer(b1, "ori");

        ByteBuffer b3 = b1.slice();
        b3.limit(64);
        printBuffer(b3, "sli");
        printBuffer(b1, "ori");
    }
    
    public static void printBuffer(ByteBuffer bb, String name){
        System.out.println(name+"  pos: "+bb.position()+" lim: "+bb.limit()+" rem: "+bb.remaining()+" cap: "+bb.capacity());
    }

}
