package org.jcodec.codecs.aac;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.jcodec.codecs.aac.ADTSParser.Header;
import org.jcodec.common.AudioCodecMeta;
import org.jcodec.common.AudioDecoder;
import org.jcodec.common.AudioFormat;
import org.jcodec.common.UsedViaReflection;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.logging.Logger;
import org.jcodec.common.model.AudioBuffer;

import net.sourceforge.jaad.aac.AACException;
import net.sourceforge.jaad.aac.Decoder;
import net.sourceforge.jaad.aac.SampleBuffer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Wraps around the JAAD decoder and implements an AudioDecoder interface.
 * 
 * @author Stanislav Vitvitskyy
 */
public class AACDecoder implements AudioDecoder {

    private Decoder decoder;

    public AACDecoder(ByteBuffer decoderSpecific) throws AACException {
        if (decoderSpecific.remaining() >= 7) {
            Header header = ADTSParser.read(decoderSpecific);
            if (header != null) {
                decoderSpecific = ADTSParser.adtsToStreamInfo(header);
            }
            Logger.info("Creating AAC decoder from ADTS header.");
        }
        decoder = new Decoder(NIOUtils.toArray(decoderSpecific));
    }

    @Override
    public AudioBuffer decodeFrame(ByteBuffer frame, ByteBuffer dst) throws IOException {
        // Internally all AAC streams are ADTS wrapped
        ADTSParser.read(frame);
        SampleBuffer sampleBuffer = new SampleBuffer();
        decoder.decodeFrame(NIOUtils.toArray(frame), sampleBuffer);
        if (sampleBuffer.isBigEndian()) {
            // Not a simple setter! This will also swap the order of bytes inside the buffer.
            sampleBuffer.setBigEndian(false);
        }

        return new AudioBuffer(ByteBuffer.wrap(sampleBuffer.getData()), toAudioFormat(sampleBuffer), 0);
    }
    
    private AudioFormat toAudioFormat(SampleBuffer sampleBuffer) {
        return new AudioFormat(sampleBuffer.getSampleRate(), sampleBuffer.getBitsPerSample(),
                sampleBuffer.getChannels(), true, sampleBuffer.isBigEndian());
    }

    @Override
    public AudioCodecMeta getCodecMeta(ByteBuffer data) throws IOException {
        SampleBuffer sampleBuffer = new SampleBuffer();
        decoder.decodeFrame(NIOUtils.toArray(data), sampleBuffer);
        sampleBuffer.setBigEndian(false);

        return org.jcodec.common.AudioCodecMeta.fromAudioFormat(toAudioFormat(sampleBuffer));
    }

    @UsedViaReflection
    public static int probe(ByteBuffer data) {
        if (data.remaining() < 7)
            return 0;
        Header header = ADTSParser.read(data);
        if (header != null)
            return 100;
        return 0;
    }
}
