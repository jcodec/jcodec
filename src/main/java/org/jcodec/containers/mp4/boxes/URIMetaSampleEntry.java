package org.jcodec.containers.mp4.boxes;

import java.nio.ByteBuffer;

public class URIMetaSampleEntry extends MetaDataSampleEntry {
    public URIMetaSampleEntry(Header atom) {
        super(atom);
    }

    public void parse(ByteBuffer input) {
        super.parse(input);
        parseExtensions(input);
    }

    protected void doWrite(ByteBuffer out) {
        super.doWrite(out);
        writeExtensions(out);
    }
}
