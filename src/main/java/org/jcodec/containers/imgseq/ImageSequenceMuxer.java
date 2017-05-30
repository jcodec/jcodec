package org.jcodec.containers.imgseq;

import static java.lang.String.format;
import static org.jcodec.common.tools.MainUtils.tildeExpand;

import java.io.IOException;

import org.jcodec.common.AudioCodecMeta;
import org.jcodec.common.Codec;
import org.jcodec.common.Muxer;
import org.jcodec.common.MuxerTrack;
import org.jcodec.common.VideoCodecMeta;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.logging.Logger;
import org.jcodec.common.model.Packet;

/**
 * A muxer and muxer track that simply saves each buffer as a file.
 * @author Stanislav Vitvitskiy
 */
public class ImageSequenceMuxer implements Muxer, MuxerTrack {
    private String fileNamePattern;
    private int frameNo;
    
    public ImageSequenceMuxer(String fileNamePattern) {
        this.fileNamePattern = fileNamePattern;
    }

    @Override
    public void addFrame(Packet packet) throws IOException {
        NIOUtils.writeTo(packet.getData(), tildeExpand(format(fileNamePattern, frameNo++)));
    }

    @Override
    public MuxerTrack addVideoTrack(Codec codec, VideoCodecMeta meta) {
        return this;
    }

    @Override
    public MuxerTrack addAudioTrack(Codec codec, AudioCodecMeta meta) {
        Logger.warn("Audio is not supported for image sequence muxer.");
        return null;
    }

    @Override
    public void finish() throws IOException {
        // NOP
    }
}
