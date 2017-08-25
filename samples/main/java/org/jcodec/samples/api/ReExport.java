package org.jcodec.samples.api;

import java.io.IOException;

import org.jcodec.api.transcode.Sink;
import org.jcodec.api.transcode.SinkImpl;
import org.jcodec.api.transcode.Source;
import org.jcodec.api.transcode.SourceImpl;
import org.jcodec.api.transcode.Transcoder;
import org.jcodec.api.transcode.Transcoder.TranscoderBuilder;
import org.jcodec.common.Format;
import org.jcodec.common.tools.MainUtils;
import org.jcodec.common.tools.MainUtils.Cmd;
import org.jcodec.common.tools.MainUtils.Flag;

public class ReExport {
    // private static final Flag FLAG_TEXT = new Flag("text", "Text to display");
    private static final Flag[] FLAGS = new Flag[] {};

    public static void main(String[] args) throws IOException {
        Cmd cmd = MainUtils.parseArguments(args, FLAGS);
        if (cmd.argsLength() < 2) {
            MainUtils.printHelpVarArgs(FLAGS, "input file", "output file");
            System.exit(-1);
        }

        Source source = SourceImpl.create(cmd.getArg(0));
        Sink sink = SinkImpl.createWithFile(cmd.getArg(1), Format.MOV, null, null);
        TranscoderBuilder builder = Transcoder.newTranscoder(source, sink);
        builder.setAudioCopy();
        builder.setVideoCopy();
        builder.create().transcode();
    }
}
