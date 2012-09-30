package org.jcodec.codecs.h264;

import java.io.IOException;

import org.jcodec.codecs.h264.decode.ErrorResilence;
import org.jcodec.codecs.h264.decode.SequenceDecoder;
import org.jcodec.codecs.h264.decode.SequenceDecoder.Sequence;
import org.jcodec.codecs.h264.decode.resilence.SimpleErrorResilence;
import org.jcodec.common.model.Picture;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Decodes frames from H264 bit stream
 * 
 * Provides frame-by-frame reading interface
 * 
 * @author Jay Codec
 * 
 */
public class H264Decoder {

    private SequenceDecoder streamDecoder;
    private Sequence curSequence;

    public H264Decoder(H264Demuxer auSource) throws IOException {
        ErrorResilence resilence = new SimpleErrorResilence();

        this.streamDecoder = new SequenceDecoder(auSource, resilence);
    }

    public Picture nextPicture() throws IOException {
        if (curSequence == null) {
            curSequence = streamDecoder.nextSequence();

            if (curSequence == null) {
                // end of stream, no more frames
                return null;
            }
        }

        Picture picture = curSequence.nextPicture();

        if (picture == null) {
            curSequence = null;
            return nextPicture();
        }

        return picture;
    }
}