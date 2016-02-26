package org.jcodec.containers.mkv;

import static java.lang.Long.toHexString;
import static org.jcodec.containers.mkv.MKVType.Attachments;
import static org.jcodec.containers.mkv.MKVType.Chapters;
import static org.jcodec.containers.mkv.MKVType.Cluster;
import static org.jcodec.containers.mkv.MKVType.Cues;
import static org.jcodec.containers.mkv.MKVType.Info;
import static org.jcodec.containers.mkv.MKVType.SeekHead;
import static org.jcodec.containers.mkv.MKVType.Tags;
import static org.jcodec.containers.mkv.MKVType.Tracks;
import static org.jcodec.containers.mkv.MKVType.createById;
import static org.jcodec.containers.mkv.util.EbmlUtil.toHexString;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.containers.mkv.boxes.EbmlBase;
import org.jcodec.containers.mkv.boxes.EbmlBin;
import org.jcodec.containers.mkv.boxes.EbmlMaster;
import org.jcodec.containers.mkv.boxes.EbmlVoid;
import org.jcodec.containers.mkv.util.EbmlUtil;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed under FreeBSD License
 * 
 * EBML IO implementation
 * 
 * @author The JCodec project
 * 
 */
public class MKVParser {

    private SeekableByteChannel channel;
    private LinkedList<EbmlMaster> trace;

    public MKVParser(SeekableByteChannel channel) {
        this.channel = channel;
        this.trace = new LinkedList<EbmlMaster>();

    }
    
    public List<EbmlMaster> parse() throws IOException {
        List<EbmlMaster> tree = new ArrayList<EbmlMaster>();
        EbmlBase e = null;

        while ((e = nextElement()) != null) {
            if (!isKnownType(e.id))
                System.err.println("Unspecified header: " + EbmlUtil.toHexString(e.id) + " at " + e.offset);

            while (!possibleChild(trace.peekFirst(), e))
                closeElem(trace.removeFirst(), tree);

            openElem(e);

            if (e instanceof EbmlMaster) {
                trace.push((EbmlMaster) e);
            } else if (e instanceof EbmlBin) {
                EbmlBin bin = (EbmlBin) e;
                EbmlMaster traceTop = trace.peekFirst();
                if ((traceTop.dataOffset + traceTop.dataLen) < (e.dataOffset + e.dataLen)) {
                    channel.setPosition((traceTop.dataOffset + traceTop.dataLen));
                } else
                    try {
                        bin.readChannel(channel);
                    } catch (OutOfMemoryError oome) {
                        throw new RuntimeException(e.type + " 0x" + toHexString(bin.id) + " size: " + toHexString(bin.dataLen) + " offset: 0x" + toHexString(e.offset), oome);
                    }
                trace.peekFirst().add(e);
            } else if (e instanceof EbmlVoid) {
                ((EbmlVoid) e).skip(channel);
            } else {
                throw new RuntimeException("Currently there are no elements that are neither Master nor Binary, should never actually get here");
            }

        }

        while (trace.peekFirst() != null)
            closeElem(trace.removeFirst(), tree);
        
        return tree;
    }

    private boolean possibleChild(EbmlMaster parent, EbmlBase child) {
        if (parent != null && Cluster.equals(parent.type) && child != null && !Cluster.equals(child.type) && !Info.equals(child.type) && !SeekHead.equals(child.type) && !Tracks.equals(child.type)
                && !Cues.equals(child.type) && !Attachments.equals(child.type) && !Tags.equals(child.type) && !Chapters.equals(child.type))
            return true;

        return MKVType.possibleChild(parent, child);
    }

    private void openElem(EbmlBase e) {
        /*
         * Whatever logging you would like to have. Here's just one example 
         */
        // System.out.println(e.type.name() + (e instanceof EbmlMaster ? " master " : "") + " id: " + printAsHex(e.id) + " off: 0x" + toHexString(e.offset).toUpperCase() + " data off: 0x" +
        // toHexString(e.dataOffset).toUpperCase() + " len: 0x" + toHexString(e.dataLen).toUpperCase());
    }

    private void closeElem(EbmlMaster e, List<EbmlMaster> tree) {
        if (trace.peekFirst() == null) {
            tree.add(e);
        } else {
            trace.peekFirst().add(e);
        }
    }

    private EbmlBase nextElement() throws IOException {
        long offset = channel.position();
        if (offset >= channel.size())
            return null;

        byte[] typeId = MKVParser.readEbmlId(channel);

        while ((typeId == null && !isKnownType(typeId)) && offset < channel.size()) {
            offset++;
            channel.setPosition(offset);
            typeId = MKVParser.readEbmlId(channel);
        }

        long dataLen = MKVParser.readEbmlInt(channel);

        EbmlBase elem = createById(typeId, offset);
        elem.offset = offset;
        elem.typeSizeLength = (int) (channel.position() - offset);
        elem.dataOffset = channel.position();

        elem.dataLen = (int) dataLen;

        return elem;
    }

    public boolean isKnownType(byte[] b) {
        if (!trace.isEmpty() && Cluster.equals(trace.peekFirst().type))
            return true;

        return MKVType.isSpecifiedHeader(b);
    }

    /**
     * Reads an EBML id from the channel. EBML ids have length encoded inside of them For instance, all one-byte ids have first byte set to '1', like 0xA3 or 0xE7, whereas the two-byte ids have first
     * byte set to '0' and second byte set to '1', thus: 0x42 0x86  or 0x42 0xF7
     * 
     * @return byte array filled with the ebml id
     * @throws IOException
     */
    static public byte[] readEbmlId(SeekableByteChannel source) throws IOException {
        if (source.position() == source.size())
            return null;
    
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.limit(1);
        source.read(buffer);
        buffer.flip();
    
        byte firstByte = buffer.get();
        int numBytes = EbmlUtil.computeLength(firstByte);
    
        if (numBytes == 0)
            return null;
    
        if (numBytes > 1) {
            buffer.limit(numBytes);
            source.read(buffer);
        }
        
        buffer.flip();
        ByteBuffer val = ByteBuffer.allocate(buffer.remaining());
        val.put(buffer);
        return val.array();
    }

    static public long readEbmlInt(SeekableByteChannel source) throws IOException {
    
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.limit(1);
        
        source.read(buffer);
        buffer.flip();
        
        // read the first byte
        byte firstByte = (byte) buffer.get();
        int length = EbmlUtil.computeLength(firstByte);
        
        if (length == 0)
            throw new RuntimeException("Invalid ebml integer size.");
    
        // read the reset
        buffer.limit(length);
        source.read(buffer);
        buffer.position(1);
        
        // use the first byte
        long value = firstByte & (0xFF >>> length); 
        length--;
        
        // use the reset
        while(length > 0){
            value = (value << 8) | (buffer.get() & 0xff);
            length--;
        }
    
        return value;
    }

}