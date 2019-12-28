package org.jcodec.containers.mp4.boxes;

import java.nio.ByteBuffer;

import org.jcodec.common.io.NIOUtils;

public class TextMetaDataSampleEntry extends MetaDataSampleEntry {
    private String contentEncoding; // optional
    private String mimeFormat;
    
    public TextMetaDataSampleEntry(Header atom) {
        super(atom);
    }

    public void parse(ByteBuffer input) {
        String asString = new String(NIOUtils.toArray(input));
        if (asString.startsWith("application")) {
            setDrefInd((short)1);
        } else {
            super.parse(input);
        }
        
        contentEncoding = NIOUtils.readNullTermString(input);
        mimeFormat = NIOUtils.readNullTermString(input);
        
        parseExtensions(input);
    }
    
    protected void doWrite(ByteBuffer out) {
       super.doWrite(out);
       
       NIOUtils.writeNullTermString(out, contentEncoding);
       NIOUtils.writeNullTermString(out, mimeFormat);
       
       writeExtensions(out);
    }
}
