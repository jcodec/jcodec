/**
 * JEBML - Java library to read/write EBML/Matroska elements.
 * Copyright (C) 2004 Jory Stone <jebml@jory.info>
 * Based on Javatroska (C) 2002 John Cannon <spyder@matroska.org>
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jcodec.containers.mkv.ebml;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;

import org.jcodec.containers.mkv.Reader;

public class MasterElement extends Element {
    protected long usedSize;
    public final ArrayList<Element> children = new ArrayList<Element>();

    public MasterElement(byte[] type) {
        super(type);
        usedSize = 0;
    }

    public Element readNextChild(Reader reader) throws IOException {
        if (usedSize >= this.size)
            return null;
        
        long start = reader.getPos();
        Element elem = reader.readNextElement();
        // elem == 'null', only if 0x00 was read as first byte of id
        if (elem == null) {
            while (elem == null && reader.getPos() < reader.getAvailable()) {
                long pos = reader.getPos();
                usedSize += (pos - start); // since elem == null, 0x00 was read instead of
                               // element ID, one byte should be accounted for
                start = pos;
                elem = reader.readNextElement();
            }

            if (elem == null || usedSize >= size)
                return null;
        }

        elem.setParent(this);

        usedSize += elem.getSize();

        return elem;
    }

    /* Skip the element data */
    @Override
    public void skipData(FileChannel source) throws IOException {
        // Skip the child elements or seek back if too much was read
        long l = size + dataOffset;
        source.position(l);
        usedSize = size;
    }

    public void addChildElement(Element elem) {
        if (elem == null)
            return;
        
        elem.setParent(this);
        children.add(elem);
    }

    public long mux(WritableByteChannel os) throws IOException {
        long size = getDataSize();

        byte[] ebmledSize = ebmlBytes(size);
        ByteBuffer bb = ByteBuffer.allocate(id.length + ebmledSize.length);
        bb.put(id);
        bb.put(ebmledSize);
        bb.flip();
        
        long bytesMuxed = os.write(bb);

        for (int i = 0; i < children.size(); i++) {
            bytesMuxed += os.write(children.get(i).mux());
        }
        return bytesMuxed;
    }
    
    
    public ByteBuffer mux() {
        long size = getDataSize();
        
        if (size > Integer.MAX_VALUE)
            System.out.println("MasterElement.mux: id.length "+id.length+"  Element.getEbmlSize("+size+"): "+Element.getEbmlSize(size)+" size: "+size);
        ByteBuffer bb = ByteBuffer.allocate((int)(id.length + Element.getEbmlSize(size)+size));

        bb.put(id);
        bb.put(ebmlBytes(size));

        for (int i = 0; i < children.size(); i++) 
            bb.put(children.get(i).mux());
        
        bb.flip();
        
        return bb;
    }
    
    private long getDataSize(){
        long returnValue = 0;
        if (children != null && !children.isEmpty()){
            // Either account for all the children
            for(Element e : children)
                returnValue += e.getSize(); 
        } else {
            // Or just rely on size attribute if no children are present
            //    this happens while reading the file
            returnValue += size;
        }
        return returnValue;
    }

    @Override
    public long getSize() {
        // 1. Start counting from the bottom
        long returnValue = getDataSize();
        
        // 2. Account for size of all the children in EBML format
        returnValue += Element.getEbmlSize(returnValue);
        
        // 3. Finally add size of this element's header
        returnValue += id.length;
        return returnValue;
    }
    
}