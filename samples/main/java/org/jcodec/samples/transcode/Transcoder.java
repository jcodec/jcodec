package org.jcodec.samples.transcode;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Set;

import org.jcodec.common.Codec;
import org.jcodec.common.Format;
import org.jcodec.common.tools.MainUtils.Cmd;

public interface Transcoder {
    void transcode(Cmd cmd, Profile profile) throws IOException;

    void printHelp(PrintStream err);

    public static class Profile {
        private Format inputFormat;
        private Format outputFormat;
        private Codec intputVideoCodec;
        private Codec outputVideoCodec;
        private Codec inputAudioCode;
        private Codec outputAudioCodec;

        public Profile(Format inputFormat, Format outputFormat, Codec intputVideoCodec, Codec outputVideoCodec,
                Codec inputAudioCode, Codec outputAudioCodec) {
            this.inputFormat = inputFormat;
            this.outputFormat = outputFormat;
            this.intputVideoCodec = intputVideoCodec;
            this.outputVideoCodec = outputVideoCodec;
            this.inputAudioCode = inputAudioCode;
            this.outputAudioCodec = outputAudioCodec;
        }

        public Format getInputFormat() {
            return inputFormat;
        }

        public Format getOutputFormat() {
            return outputFormat;
        }

        public Codec getIntputVideoCodec() {
            return intputVideoCodec;
        }

        public Codec getOutputVideoCodec() {
            return outputVideoCodec;
        }

        public Codec getInputAudioCode() {
            return inputAudioCode;
        }

        public Codec getOutputAudioCodec() {
            return outputAudioCodec;
        }
    }

    Set<Format> inputFormat();

    Set<Format> outputFormat();

    Set<Codec> inputVideoCodec();

    Set<Codec> outputVideoCodec();

    Set<Codec> inputAudioCodec();

    Set<Codec> outputAudioCodec();
}