package org.jcodec.containers;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;

import org.jcodec.containers.mkv.MKVTestSuite;
import org.jcodec.containers.mkv.Reader;
import org.junit.Test;

public class SimpleMappingTest {

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
        ByteBuffer bb = fetchFrom(channel);
        
        System.out.println("pysch 0x"+Reader.printAsHex(bb.array()).toUpperCase()+" "+org.jcodec.containers.mkv.Type.createElementById(bb.array()));
    }

    public static ByteBuffer fetchFrom(ReadableByteChannel ch) throws IOException {
        ByteBuffer bufferForFirstByte = ByteBuffer.allocate(1);
        bufferForFirstByte.clear();
        ch.read(bufferForFirstByte);
        bufferForFirstByte.flip();
        byte first = bufferForFirstByte.get();
        int idSize = Reader.getEbmlSizeByFirstByte(first);
        
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
