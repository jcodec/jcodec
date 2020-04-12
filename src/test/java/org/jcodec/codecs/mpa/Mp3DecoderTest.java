package org.jcodec.codecs.mpa;

import java.io.File;
import java.nio.ByteBuffer;

import org.jcodec.Asserts;
import org.jcodec.common.io.FileChannelWrapper;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.AudioBuffer;
import org.jcodec.common.model.Packet;
import org.jcodec.containers.mp3.MPEGAudioDemuxer;
import org.junit.Test;

public class Mp3DecoderTest {
    @Test
    public void testOneFile() throws Exception {
        File f = new File("src/test/resources/drip.mp3");
        File f1 = new File("src/test/resources/drip.wav");
        Mp3Decoder decoder = new Mp3Decoder();

        ByteBuffer bb = NIOUtils.fetchFromFile(f1);
        NIOUtils.skip(bb, 44);

        FileChannelWrapper ch = NIOUtils.readableChannel(f);
        MPEGAudioDemuxer audioDemuxer = new MPEGAudioDemuxer(ch);
        for (int i = 0; i < 31; i++) {
            Packet pkt = audioDemuxer.nextFrame();
            AudioBuffer audioBuffer = decoder.decodeFrame(pkt.getData(), ByteBuffer.allocate(65536));

            ByteBuffer frame = NIOUtils.read(bb, audioBuffer.getData().remaining());

            Asserts.assertEpsilonEquals(NIOUtils.toArray(frame), NIOUtils.toArray(audioBuffer.getData()), 1);
        }

        ch.close();
    }
}
