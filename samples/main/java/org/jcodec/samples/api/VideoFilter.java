package org.jcodec.samples.api;

import static org.jcodec.common.Format.MOV;
import static org.jcodec.common.Tuple._3;

import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

import org.jcodec.api.transcode.Sink;
import org.jcodec.api.transcode.SinkImpl;
import org.jcodec.api.transcode.Source;
import org.jcodec.api.transcode.SourceImpl;
import org.jcodec.api.transcode.Transcoder;
import org.jcodec.api.transcode.Transcoder.TranscoderBuilder;
import org.jcodec.api.transcode.filter.AWTFilter;
import org.jcodec.common.Codec;
import org.jcodec.common.Demuxer;
import org.jcodec.common.DemuxerTrack;
import org.jcodec.common.Format;
import org.jcodec.common.JCodecUtil;
import org.jcodec.common.model.Packet;
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
    private static final Flag FLAG_TEXT = Flag.flag("text", null, "Text to display");
    private static final Flag[] FLAGS = new Flag[] {
        FLAG_TEXT
    };
    
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
            g.drawString(text, x(frameNo, rgb.getWidth(), rgb.getHeight()), y(frameNo, rgb.getWidth(), rgb.getHeight()));
            
            frameNo++;
            
            return rgb;
        }
        
        private int y(int frameNo2, int w, int h) {
            if ((frameNo % 100) == 0) {
                posY = (int)(Math.random() * h/4) + h/4;
            }
            return (int)Math.sqrt(frameNo2 * 60)/2 + posY;
        }
        private int x(int frameNo2, int w, int h) {
            if ((frameNo % 100) == 0) {
                posX = (int)(Math.random() * w/4) + h/4;
            }
            return (int)Math.sqrt(frameNo2 * 60)/2 + posX;
        }
        private int fontSize(int frameNo, int w, int h) {
            int inc = (frameNo % 100);
            return Math.min(w, h) / 2 + (int)Math.sqrt(inc * 60);
        }
    }

    public static void main(String[] args) throws IOException {
        Cmd cmd = MainUtils.parseArguments(args, FLAGS);
        if (cmd.argsLength() < 2) {
            MainUtils.printHelpArgs(FLAGS, new String[]{"input file", "output file"});
            System.exit(-1);
        }

        File input = new File(cmd.getArg(0));
        File output = new File(cmd.getArg(1));

        Format format = JCodecUtil.detectFormat(input);
        Codec videoCodec = getVideoCodec(format, input);
        Codec audioCodec = getAudioCodec(format, input);
        Source source = new SourceImpl(input.getAbsolutePath(), format, _3(0, 0, videoCodec), _3(0, 0, audioCodec));
        Sink sink = new SinkImpl(output.getAbsolutePath(), MOV, videoCodec, audioCodec);

        TranscoderBuilder builder = Transcoder.newTranscoder();
        builder.addSource(source);
        builder.addSink(sink);
        builder.addFilter(0, new TextFilter(cmd.getStringFlagD(FLAG_TEXT, "JCodec")));
        builder.setAudioMapping(0, 0, true);
        builder.setVideoMapping(0, 0, false);

        Transcoder transcoder = builder.create();
        transcoder.transcode();
    }

    private static Codec getAudioCodec(Format format, File input) throws IOException {
        Demuxer demuxer = JCodecUtil.createDemuxer(format, input);
        return detectCodecInternal(demuxer.getAudioTracks());
    }

    private static Codec getVideoCodec(Format format, File input) throws IOException {
        Demuxer demuxer = JCodecUtil.createDemuxer(format, input);
        return detectCodecInternal(demuxer.getVideoTracks());
    }

    private static Codec detectCodecInternal(List<? extends DemuxerTrack> tracks) throws IOException {
        if (tracks.size() == 0)
            return null;
        DemuxerTrack track = tracks.get(0);
        Codec result = track.getMeta().getCodec();
        if (result == null) {
            Packet nextFrame = track.nextFrame();
            if (nextFrame == null)
                return null;
            result = JCodecUtil.detectDecoder(nextFrame.getData());
        }

        return result;
    }
}
