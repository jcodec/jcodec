package org.jcodec.containers.mkv;

import java.util.Arrays;

import org.junit.Ignore;
import org.junit.Test;

public class BitwiseMagicTest {

    @Test
    @Ignore
    public void test() {
        byte b127 = 127;
        byte bm128 = -128;
        System.out.println(Integer.toBinaryString(b127));
        System.out.println(Integer.toBinaryString(bm128));
        
        System.out.println(Integer.toBinaryString(b127 & 0x00FF));
        System.out.println(Integer.toBinaryString(bm128 & 0x00FF));
        
        System.out.println(Long.toBinaryString(((long)b127 << 56) >>> 56));
        System.out.println(Long.toBinaryString(((long)bm128 << 56) >>> 56));
        
        System.out.println(Long.toBinaryString((long)(b127 & 0x00FF)));
        System.out.println(Long.toBinaryString((long)(bm128 & 0x00FF)));
        
        System.out.println(Arrays.equals(new byte[]{0x0F, 0x08}, new byte[]{0x0F}));
    }

}
