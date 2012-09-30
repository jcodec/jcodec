package org.jcodec.common.io;

import static java.util.Arrays.copyOfRange;
import static org.junit.Assert.assertArrayEquals;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;

import junit.framework.TestCase;

import org.junit.Assert;
import org.junit.Test;

public class TestRandomAccessFileInputStream extends TestCase {

    private File tmp;
    private byte[] exps;

    protected void setUp() throws Exception {
        tmp = File.createTempFile("jay", "cool");
        FileOutputStream out = new FileOutputStream(tmp);
        exps = new byte[0x80000];
        for (int i = 0; i < exps.length; i++) {
            exps[i] = (byte) (Math.random() * 255);
        }
        out.write(exps);
        out.close();
    }

    @Test
    public void testStream() throws Exception {
        RandomAccessFileInputStream is = new RandomAccessFileInputStream(tmp);
        byte[] buf = new byte[10];
        assertEquals(exps[(int)is.getPos()] & 0xff, is.read());
        is.read(buf);
        assertArrayEquals(copyOfRange(exps, (int)is.getPos() - 10, (int)is.getPos()), buf);
        is.seek(69494);
        assertEquals(exps[(int)is.getPos()] & 0xff, is.read());
        is.read(buf);
        assertArrayEquals(copyOfRange(exps, (int)is.getPos() - 10, (int)is.getPos()), buf);
        is.seek(12342);
        assertEquals(exps[(int)is.getPos()] & 0xff, is.read());
        is.read(buf);
        assertArrayEquals(copyOfRange(exps, (int)is.getPos() - 10, (int)is.getPos()), buf);
        is.seek(161232);
        assertEquals(exps[(int)is.getPos()] & 0xff, is.read());
        is.read(buf);
        assertArrayEquals(copyOfRange(exps, (int)is.getPos() - 10, (int)is.getPos()), buf);
        is.seek(367302);
        assertEquals(exps[(int)is.getPos()] & 0xff, is.read());
        is.read(buf);
        assertArrayEquals(copyOfRange(exps, (int)is.getPos() - 10, (int)is.getPos()), buf);
        is.seek(524286);
        assertEquals(exps[(int)is.getPos()] & 0xff, is.read());
        int pos = (int)is.getPos();
        Arrays.fill(buf, (byte)0);
        assertEquals(1, is.read(buf));
        Assert.assertArrayEquals(copyOfRange(exps, pos, pos + 10), buf);
        assertEquals(-1, is.read());
        assertEquals(-1, is.read(buf));
    }
    
    @Test
    public void testAutoStream() throws Exception {
        RandomAccessInputStream is = new AutoRandomAccessFileInputStream(tmp);
        byte[] buf = new byte[10];
        assertEquals(exps[(int)is.getPos()] & 0xff, is.read());
        is.close();
        is.read(buf);
        assertArrayEquals(copyOfRange(exps, (int)is.getPos() - 10, (int)is.getPos()), buf);
        is.seek(69494);
        assertEquals(exps[(int)is.getPos()] & 0xff, is.read());
        is.close();
        is.read(buf);
        assertArrayEquals(copyOfRange(exps, (int)is.getPos() - 10, (int)is.getPos()), buf);
        is.seek(12342);
        assertEquals(exps[(int)is.getPos()] & 0xff, is.read());
        is.close();
        is.read(buf);
        assertArrayEquals(copyOfRange(exps, (int)is.getPos() - 10, (int)is.getPos()), buf);
        is.seek(161232);
        assertEquals(exps[(int)is.getPos()] & 0xff, is.read());
        is.close();
        is.read(buf);
        assertArrayEquals(copyOfRange(exps, (int)is.getPos() - 10, (int)is.getPos()), buf);
        is.seek(367302);
        assertEquals(exps[(int)is.getPos()] & 0xff, is.read());
        is.close();
        is.read(buf);
        assertArrayEquals(copyOfRange(exps, (int)is.getPos() - 10, (int)is.getPos()), buf);
        is.seek(524286);
        assertEquals(exps[(int)is.getPos()] & 0xff, is.read());
        is.close();
        int pos = (int)is.getPos();
        Arrays.fill(buf, (byte)0);
        assertEquals(1, is.read(buf));
        Assert.assertArrayEquals(copyOfRange(exps, pos, pos + 10), buf);
        assertEquals(-1, is.read());
        assertEquals(-1, is.read(buf));
    }
}
