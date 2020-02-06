package org.jcodec.containers.mp4.boxes;

import java.nio.ByteBuffer;

import org.jcodec.common.io.NIOUtils;

/**
* This class is part of JCodec ( www.jcodec.org ) This software is distributed
* under FreeBSD License
* 
* Describes video payload sample
* 
* @author The JCodec project
* 
*/
public class TextMetaDataSampleEntry extends MetaDataSampleEntry {
    private String contentEncoding; // optional
    private String mimeFormat;
    
    
    public TextMetaDataSampleEntry(String contentEncoding, String mimeFormat) {
        super(Header.createHeader("mett", 0));
        this.contentEncoding = contentEncoding;
        this.mimeFormat = mimeFormat;
    }

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

    public String getContentEncoding() {
        return contentEncoding;
    }

    public String getMimeFormat() {
        return mimeFormat;
    }
}
