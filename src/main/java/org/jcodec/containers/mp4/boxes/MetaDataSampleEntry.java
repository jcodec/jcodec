package org.jcodec.containers.mp4.boxes;

import java.nio.ByteBuffer;

import org.jcodec.common.io.NIOUtils;

public class MetaDataSampleEntry extends SampleEntry {
    protected short drefInd;

    public MetaDataSampleEntry(Header header) {
        super(header);
    }
}
