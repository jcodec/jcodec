package org.jcodec.containers.mp4.boxes;

import java.nio.ByteBuffer;

import org.jcodec.common.io.NIOUtils;

public class XMLMetaDataSampleEntry extends MetaDataSampleEntry {
    private String contentEncoding; // optional
    private String namespace;
    private String schemaLocation; // optional
    
    public XMLMetaDataSampleEntry(Header atom) {
        super(atom);
    }

    public void parse(ByteBuffer input) {
        super.parse(input);
        
        contentEncoding = NIOUtils.readNullTermString(input);
        namespace = NIOUtils.readNullTermString(input);
        schemaLocation = NIOUtils.readNullTermString(input);
        
        parseExtensions(input);
    }
    
    protected void doWrite(ByteBuffer out) {
       super.doWrite(out);
       
       NIOUtils.writeNullTermString(out, contentEncoding);
       NIOUtils.writeNullTermString(out, namespace);
       NIOUtils.writeNullTermString(out, schemaLocation);
       
       writeExtensions(out);
    }
}
