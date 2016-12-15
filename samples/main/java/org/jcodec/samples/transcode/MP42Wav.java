package org.jcodec.samples.transcode;

import static org.jcodec.common.io.NIOUtils.readableChannel;
import static org.jcodec.common.io.NIOUtils.writableChannel;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import net.sourceforge.jaad.aac.Decoder;
import net.sourceforge.jaad.aac.SampleBuffer;

import org.jcodec.codecs.aac.AACUtils;
import org.jcodec.codecs.aac.AACUtils.AACMetadata;
import org.jcodec.codecs.wav.WavOutput;
import org.jcodec.common.Codec;
import org.jcodec.common.Format;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.logging.Logger;
import org.jcodec.common.model.Packet;
import org.jcodec.common.tools.MainUtils;
import org.jcodec.common.tools.MainUtils.Cmd;
import org.jcodec.containers.mp4.boxes.SampleEntry;
import org.jcodec.containers.mp4.demuxer.AbstractMP4DemuxerTrack;
import org.jcodec.containers.mp4.demuxer.MP4Demuxer;

class MP42Wav implements Transcoder {
    @Override
    public void transcode(Cmd cmd, Profile profile) throws IOException {
        SeekableByteChannel source = null;
        SeekableByteChannel sink = null;
        WavOutput wavOutput = null;
        try {
            source = readableChannel(new File(cmd.getArg(0)));
            sink = writableChannel(new File(cmd.getArg(1)));

            MP4Demuxer demuxer = new MP4Demuxer(source);

            List<AbstractMP4DemuxerTrack> tracks = demuxer.getAudioTracks();
            AbstractMP4DemuxerTrack selectedTrack = null;
            for (AbstractMP4DemuxerTrack track : tracks) {
                if (track.getCodec() == Codec.AAC) {
                    selectedTrack = track;
                    break;
                }
            }
            if (selectedTrack == null) {
                Logger.error("Could not find an AAC track");
                return;
            } else {
                Logger.info("Using the AAC track: " + selectedTrack.getNo());
            }
            SampleEntry sampleEntry = selectedTrack.getSampleEntries()[0];
            AACMetadata meta = AACUtils.getMetadata(sampleEntry);
            wavOutput = new WavOutput(sink, meta.getFormat());
            Decoder aacDecoder = new Decoder(NIOUtils.toArray(AACUtils.getCodecPrivate(sampleEntry)));
            SampleBuffer sampleBuffer = new SampleBuffer();

            Packet packet;
            while ((packet = selectedTrack.nextFrame()) != null) {
                aacDecoder.decodeFrame(NIOUtils.toArray(packet.getData()), sampleBuffer);
                if (sampleBuffer.isBigEndian())
                    toLittleEndian(sampleBuffer);
                wavOutput.write(ByteBuffer.wrap(sampleBuffer.getData()));
            }
        } finally {
            NIOUtils.closeQuietly(source);
            NIOUtils.closeQuietly(wavOutput);
        }
    }

    private void toLittleEndian(SampleBuffer sampleBuffer) {
        byte[] data = sampleBuffer.getData();
        for (int i = 0; i < data.length; i += 2) {
            byte tmp = data[i];
            data[i] = data[i + 1];
            data[i + 1] = tmp;
        }
    }

    @Override
    public void printHelp(PrintStream err) {
        MainUtils.printHelpVarArgs(new HashMap<String, String>() {
            {
            }
        }, "in file", "out file");
    }

    @Override
    public Set<Format> inputFormat() {
        return TranscodeMain.formats(Format.MOV);
    }

    @Override
    public Set<Format> outputFormat() {
        return TranscodeMain.formats(Format.WAV);
    }

    @Override
    public Set<Codec> inputVideoCodec() {
        return null;
    }

    @Override
    public Set<Codec> outputVideoCodec() {
        return null;
    }

    @Override
    public Set<Codec> inputAudioCodec() {
        return TranscodeMain.codecs(Codec.AAC);
    }

    @Override
    public Set<Codec> outputAudioCodec() {
        return TranscodeMain.codecs(Codec.PCM);
    }
}