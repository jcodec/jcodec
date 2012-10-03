package org.jcodec.codecs.pcm;

import java.io.IOException;

import org.jcodec.common.AudioDecoder;
import org.jcodec.common.io.Buffer;
import org.jcodec.common.model.AudioBuffer;
import org.jcodec.player.filters.MediaInfo;
import org.jcodec.player.filters.MediaInfo.AudioInfo;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class PCMDecoder implements AudioDecoder {

    private MediaInfo.AudioInfo audioInfo;

    public PCMDecoder(AudioInfo audioInfo) {
        this.audioInfo = audioInfo;
    }

    public AudioBuffer decodeFrame(Buffer frame, byte[] dst) throws IOException {
        int tgt = Math.min(dst.length, frame.remaining());
        frame.toArray(dst, 0, tgt);

        return new AudioBuffer(new Buffer(dst, 0, tgt), audioInfo.getFormat(), audioInfo.getFramesPerPacket());
    }
}
