package org.jcodec.codecs.pcm;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.jcodec.common.AudioCodecMeta;
import org.jcodec.common.AudioDecoder;
import org.jcodec.common.AudioFormat;
import org.jcodec.common.io.NIOUtils;
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

    public AudioBuffer decodeFrame(ByteBuffer frame, ByteBuffer dst) {
        ByteBuffer dup = dst.duplicate();
        NIOUtils.write(dst, frame);

        dup.flip();
        AudioFormat format = audioInfo.getFormat();
        int nFrames = dup.remaining() / format.getFrameSize();
        return new AudioBuffer(dup, format, nFrames);
    }

    @Override
    public AudioCodecMeta getCodecMeta(ByteBuffer data) throws IOException {
        return null;
    }
}
