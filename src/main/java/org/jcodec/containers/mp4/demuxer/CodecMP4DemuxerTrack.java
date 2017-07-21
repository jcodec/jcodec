package org.jcodec.containers.mp4.demuxer;

import java.nio.ByteBuffer;

import org.jcodec.codecs.aac.AACUtils;
import org.jcodec.codecs.aac.ADTSParser;
import org.jcodec.codecs.aac.ADTSParser.Header;
import org.jcodec.codecs.h264.H264Utils;
import org.jcodec.codecs.h264.mp4.AvcCBox;
import org.jcodec.common.Codec;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.containers.mp4.boxes.MovieBox;
import org.jcodec.containers.mp4.boxes.TrakBox;
import org.jcodec.containers.mp4.boxes.VideoSampleEntry;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Regular MP4 track containing frames
 * 
 * @author The JCodec project
 * 
 */
public class CodecMP4DemuxerTrack extends MP4DemuxerTrack {

    private ByteBuffer codecPrivate;

    private AvcCBox avcC;

    public CodecMP4DemuxerTrack(MovieBox mov, TrakBox trak, SeekableByteChannel input) {
        super(mov, trak, input);
        if (Codec.codecByFourcc(getFourcc()) == Codec.H264) {
            avcC = H264Utils.parseAVCC((VideoSampleEntry) getSampleEntries()[0]);
        }
        codecPrivate = MP4DemuxerTrackMeta.getCodecPrivate(this);
    }

    @Override
    public ByteBuffer convertPacket(ByteBuffer result) {
        if (codecPrivate != null) {
            if (Codec.codecByFourcc(getFourcc()) == Codec.H264) {
                ByteBuffer annexbCoded = H264Utils.decodeMOVPacket(result, avcC);
                if (H264Utils.isByteBufferIDRSlice(annexbCoded)) {
                    return NIOUtils.combine(codecPrivate, annexbCoded);
                }
                return annexbCoded;
            } else if (Codec.codecByFourcc(getFourcc()) == Codec.AAC) {
                // !!! crcAbsent, numAACFrames
                Header adts = AACUtils.streamInfoToADTS(codecPrivate, true, 1, result.remaining());
                ByteBuffer adtsRaw = ByteBuffer.allocate(7);
                ADTSParser.write(adts, adtsRaw);
                return NIOUtils.combine(adtsRaw, result);
            }
        }
        return result;
    }
}
