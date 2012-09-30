package org.jcodec.codecs.h264;

import java.io.IOException;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * An interface that H264 compliant demuxer shell provide
 * 
 * @author Jay Codec
 * 
 */
public interface H264Demuxer extends StreamParams {

    AccessUnit nextAcceessUnit() throws IOException;

}
