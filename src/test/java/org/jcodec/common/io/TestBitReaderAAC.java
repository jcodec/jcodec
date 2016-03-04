package org.jcodec.common.io;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.jcodec.common.StringUtils;
import org.jcodec.platform.Platform;
import org.junit.Assert;
import org.junit.Test;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.System;
import java.nio.ByteBuffer;
import net.sourceforge.jaad.aac.AACException;
import net.sourceforge.jaad.aac.syntax.BitStream;
import net.sourceforge.jaad.aac.syntax.IBitStream;
import net.sourceforge.jaad.aac.syntax.NIOBitStream;

public class TestBitReader {
    @Test
    public void testEOF() throws Exception {
        byte[] src = new byte[] { b("10011000") };
        boolean eof = false;
        try {
            BitStream bs = BitStream.createBitStream(src);
            for (int i = 0; i < 10; i++) {
                System.out.println(Integer.toBinaryString(bs.readBits(4)) + " " + bs.getBitsLeft());
            }
        } catch (AACException e) {
            eof = true;
        }
        Assert.assertTrue(eof);
    }

    @Test
    public void testEOF2() throws Exception {
        byte[] src = new byte[] { b("10011000") };
        boolean eof = false;
        try {
            IBitStream bs = new NIOBitStream(BitReader.createBitReader(ByteBuffer.wrap(src)));
            for (int i = 0; i < 10; i++) {
                System.out.println(Integer.toBinaryString(bs.readBits(4)) + " " + bs.getBitsLeft());
            }
        } catch (AACException e) {
            eof = true;
        }
        Assert.assertTrue(eof);
    }
}