package org.jcodec.containers.mp4.boxes;

public class MetaDataSampleEntry extends SampleEntry {
    protected short drefInd;

    public MetaDataSampleEntry(Header header) {
        super(header);
    }
}
