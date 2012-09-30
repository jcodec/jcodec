package org.jcodec.containers.mp4.boxes;


/**
 * This class is part of JCodec ( www.jcodec.org )
 * This software is distributed under FreeBSD License
 * 
 * @author The JCodec project
 *
 */
public class AVC1Box extends VideoSampleEntry {

    public AVC1Box() {
        super(new Header("avc1"));
    }

}
