package org.jcodec.containers.mkv.boxes;

import static org.jcodec.containers.mkv.util.EbmlUtil.ebmlLength;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import org.jcodec.containers.mkv.util.EbmlUtil;


/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * EBML IO implementation
 * 
 * @author The JCodec project
 * 
 */
public class EbmlMaster extends EbmlBase {
    
    protected long usedSize;
    public final ArrayList<EbmlBase> children = new ArrayList<EbmlBase>();
    
    public EbmlMaster(byte[] id) {
        this.id = id;
    }

    public void add(EbmlBase elem) {
        if (elem == null)
            return;
        
        elem.parent = this;
        children.add(elem);
//        dataLen += elem.size();
    }
    
    @Override
    public ByteBuffer getData() {
        long size = getDataLen();
        
        if (size > Integer.MAX_VALUE)
            System.out.println("EbmlMaster.getData: id.length "+id.length+"  EbmlUtil.ebmlLength("+size+"): "+ebmlLength(size)+" size: "+size);
        ByteBuffer bb = ByteBuffer.allocate((int)(id.length + ebmlLength(size)+size));

        bb.put(id);
        bb.put(EbmlUtil.ebmlEncode(size));

        for (int i = 0; i < children.size(); i++) 
            bb.put(children.get(i).getData());
        
        
        bb.flip();
        
        return bb;
    }
    
    protected long getDataLen() {
        if (children == null || children.isEmpty())
            return dataLen;

        long dataLength = 0;
        for (EbmlBase e : children)
            dataLength += e.size();
        return dataLength;
    }

    @Override
    public long size() {
        long size = getDataLen();
        
        size += ebmlLength(size);
        
        size += id.length;
        return size;
    }

}
