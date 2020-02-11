package org.jcodec.codecs.aac;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.jcodec.codecs.aac.ADTSParser.Header;
import org.jcodec.common.AudioCodecMeta;
import org.jcodec.common.AudioDecoder;
import org.jcodec.common.UsedViaReflection;
import org.jcodec.common.logging.Logger;
import org.jcodec.common.model.AudioBuffer;

import net.sourceforge.jaad.aac.AACException;
import net.sourceforge.jaad.aac.Decoder;

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
        decoder = new Decoder(decoderSpecific);
    }

    @Override
    public AudioBuffer decodeFrame(ByteBuffer frame, ByteBuffer dst) throws IOException {
        // Internally all AAC streams are ADTS wrapped
        ADTSParser.read(frame);
        dst.order(ByteOrder.LITTLE_ENDIAN);
        decoder.decodeFrame(frame, dst);

        return new AudioBuffer(dst, decoder.getMeta(), 0);
    }
    

    @Override
    public AudioCodecMeta getCodecMeta(ByteBuffer data) throws IOException {
        decoder.decodeFrame(data, ByteBuffer.allocate(1 << 16));
        return org.jcodec.common.AudioCodecMeta.fromAudioFormat(decoder.getMeta());
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
