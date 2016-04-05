package org.jcodec.containers;
import static org.jcodec.containers.mkv.util.EbmlUtil.computeLength;

import org.jcodec.containers.mkv.MKVTestSuite;
import org.jcodec.containers.mkv.MKVType;
import org.jcodec.containers.mkv.util.EbmlUtil;
import org.junit.Ignore;
import org.junit.Test;

import js.io.FileInputStream;
import js.io.IOException;
import js.lang.System;
import js.nio.ByteBuffer;
import js.nio.channels.FileChannel;
import js.nio.channels.ReadableByteChannel;

public class SimpleMappingTest {

    @Ignore @Test
    public void test() throws IOException {
        MKVTestSuite suite = MKVTestSuite.read();
        System.out.println("Scanning file: " + suite.test2.getAbsolutePath());

        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(suite.test2);
            readEBMLElements(fileInputStream.getChannel());
        } finally {
            fileInputStream.close();
        }

    }

    private void readEBMLElements(FileChannel channel) throws IOException {
        long offset = channel.position();
        ByteBuffer bb = fetchFrom(channel);
        
        System.out.println("pysch 0x"+EbmlUtil.toHexString(bb.array()).toUpperCase()+" "+MKVType.createById(bb.array(), offset));
    }

    public static ByteBuffer fetchFrom(ReadableByteChannel ch) throws IOException {
        ByteBuffer bufferForFirstByte = ByteBuffer.allocate(1);
        bufferForFirstByte.clear();
        ch.read(bufferForFirstByte);
        bufferForFirstByte.flip();
        byte first = bufferForFirstByte.get();
        int idSize = computeLength(first);
        
        ByteBuffer bufferForId = ByteBuffer.allocate(idSize);
        bufferForId.put(first);
        ch.read(bufferForId);
        bufferForId.flip();
        return bufferForId;
    }

    public static int read(ReadableByteChannel channel, ByteBuffer buffer) throws IOException {
        int rem = buffer.position();
        while (channel.read(buffer) != -1 && buffer.hasRemaining())
            ;
        return buffer.position() - rem;
    }

}
