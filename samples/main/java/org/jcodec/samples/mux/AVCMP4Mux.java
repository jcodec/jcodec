package org.jcodec.samples.mux;

import static org.jcodec.common.io.NIOUtils.writableChannel;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.jcodec.codecs.h264.BufferH264ES;
import org.jcodec.codecs.h264.H264Decoder;
import org.jcodec.common.Codec;
import org.jcodec.common.Muxer;
import org.jcodec.common.MuxerTrack;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Packet;
import org.jcodec.common.tools.MainUtils;
import org.jcodec.common.tools.MainUtils.Cmd;
import org.jcodec.containers.mp4.muxer.MP4Muxer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Sample code. Muxes H.264 ( MPEG4 AVC ) elementary stream into MP4 ( ISO
 * 14496-1/14496-12/14496-14, Quicktime ) container
 * 
 * @author The JCodec project
 * 
 */
public class AVCMP4Mux {
    public static void main(String[] args) throws Exception {
        Cmd cmd = MainUtils.parseArguments(args);
        if (cmd.argsLength() < 2) {
            MainUtils.printHelpVarArgs(new HashMap<String, String>() {
                {
                    put("q", "Look for stream parameters only in the beginning of stream");
                }
            }, "in.264", "out.mp4");
            System.exit(-1);
        }

        File in = new File(cmd.getArg(0));
        File out = new File(cmd.getArg(1));

        SeekableByteChannel file = writableChannel(out);
        MP4Muxer muxer = MP4Muxer.createMP4MuxerToChannel(file);
        mux(muxer, in);

        muxer.finish();

        file.close();
    }

    private static void mux(Muxer muxer, File f) throws IOException {
        MuxerTrack track = null;
        BufferH264ES es = new BufferH264ES(NIOUtils.mapFile(f));

        Packet frame = null;
        while ((frame = es.nextFrame()) != null) {
            if (track == null) {
                track = muxer.addVideoTrack(Codec.H264, new H264Decoder().getCodecMeta(frame.getData()));
            }
            track.addFrame(frame);
        }
    }
}