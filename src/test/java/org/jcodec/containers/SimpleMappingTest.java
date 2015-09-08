package org.jcodec.containers;

import static org.jcodec.containers.mkv.util.EbmlUtil.computeLength;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;

import org.jcodec.containers.mkv.MKVTestSuite;
import org.jcodec.containers.mkv.MKVType;
import org.jcodec.containers.mkv.util.EbmlUtil;
import org.junit.Ignore;
import org.junit.Test;

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
