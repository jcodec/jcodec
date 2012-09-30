package org.jcodec.containers.mp4.boxes;

import java.io.DataOutput;
import java.io.IOException;
import java.io.InputStream;

import org.jcodec.common.io.ReaderBE;

/**
 * This class is part of JCodec ( www.jcodec.org )
 * This software is distributed under FreeBSD License
 * 
 * @author The JCodec project
 *
 */
public class UrlBox extends FullBox {

    private String url;
    
    public static String fourcc() {
        return "url ";
    }

    public UrlBox(String url) {
        super(new Header(fourcc()));
        this.url = url;
    }
    
    public UrlBox(Header atom) {
        super(atom);
    }

    @Override
    public void parse(InputStream input) throws IOException {
        super.parse(input);
        if((flags & 0x1) != 0)
            return;
        url = ReaderBE.readNullTermString(input);
    }

    @Override
    protected void doWrite(DataOutput out) throws IOException {
        super.doWrite(out);
        
        if (url != null) {
            out.write(url.getBytes());
            out.write(0);
        }
    }

    public String getUrl() {
        return url;
    }
}
