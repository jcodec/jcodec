package org.jcodec.samples.transcode;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Set;

import org.jcodec.common.Codec;
import org.jcodec.common.Format;
import org.jcodec.common.tools.MainUtils.Cmd;

interface Profile {
    void transcode(Cmd cmd) throws IOException;

    Set<Format> inputFormat();

    Set<Format> outputFormat();

    Set<Codec> inputVideoCodec();

    Set<Codec> outputVideoCodec();

    Set<Codec> inputAudioCodec();

    Set<Codec> outputAudioCodec();

    void printHelp(PrintStream err);
}