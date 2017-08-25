package org.jcodec.samples.api;

import static org.jcodec.common.Format.MOV;

import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import org.jcodec.api.transcode.Sink;
import org.jcodec.api.transcode.SinkImpl;
import org.jcodec.api.transcode.Source;
import org.jcodec.api.transcode.SourceImpl;
import org.jcodec.api.transcode.Transcoder;
import org.jcodec.api.transcode.Transcoder.TranscoderBuilder;
import org.jcodec.api.transcode.filter.AWTFilter;
import org.jcodec.common.tools.MainUtils;
import org.jcodec.common.tools.MainUtils.Cmd;
import org.jcodec.common.tools.MainUtils.Flag;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class VideoFilter {
    private static final Flag FLAG_TEXT = new Flag("text", "Text to display");
    private static final Flag[] FLAGS = new Flag[] { FLAG_TEXT };

    static class TextFilter extends AWTFilter {
        private int frameNo;
        private int posX;
        private int posY;
        private String text;

        public TextFilter(String text) {
            this.text = text;
        }

        @Override
        protected BufferedImage filterBufferedImage(BufferedImage rgb) {
            Graphics g = rgb.getGraphics();
            g.setFont(new Font("Arial", Font.PLAIN, fontSize(frameNo, rgb.getWidth(), rgb.getHeight())));
            g.drawString(text, x(frameNo, rgb.getWidth(), rgb.getHeight()),
                    y(frameNo, rgb.getWidth(), rgb.getHeight()));

            frameNo++;

            return rgb;
        }

        private int y(int frameNo2, int w, int h) {
            if ((frameNo % 100) == 0) {
                posY = (int) (Math.random() * h / 4) + h / 4;
            }
            return (int) Math.sqrt(frameNo2 * 60) / 2 + posY;
        }

        private int x(int frameNo2, int w, int h) {
            if ((frameNo % 100) == 0) {
                posX = (int) (Math.random() * w / 4) + h / 4;
            }
            return (int) Math.sqrt(frameNo2 * 60) / 2 + posX;
        }

        private int fontSize(int frameNo, int w, int h) {
            int inc = (frameNo % 100);
            return Math.min(w, h) / 2 + (int) Math.sqrt(inc * 60);
        }
    }

    public static void main(String[] args) throws IOException {
        Cmd cmd = MainUtils.parseArguments(args, FLAGS);
        if (cmd.argsLength() < 2) {
            MainUtils.printHelpVarArgs(FLAGS, "input file", "output file");
            System.exit(-1);
        }

        File input = new File(cmd.getArg(0));
        File output = new File(cmd.getArg(1));

        Source source = SourceImpl.create(input.getAbsolutePath());
        Sink sink = SinkImpl.createSameAs(output.getAbsolutePath(), source);

        TranscoderBuilder builder = Transcoder.newTranscoder(source, sink);
        builder.addVideoFilter(new TextFilter(cmd.getStringFlagD(FLAG_TEXT, "JCodec")));
        builder.setAudioCopy();

        builder.create().transcode();
    }
}
