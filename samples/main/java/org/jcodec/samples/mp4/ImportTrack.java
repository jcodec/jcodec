package org.jcodec.samples.mp4;

import java.io.File;
import java.util.Arrays;

import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.tools.MainUtils;
import org.jcodec.common.tools.MainUtils.Cmd;
import org.jcodec.common.tools.MainUtils.Flag;
import org.jcodec.containers.mp4.MP4TrackType;
import org.jcodec.containers.mp4.MP4Util;
import org.jcodec.containers.mp4.MP4Util.Movie;
import org.jcodec.containers.mp4.boxes.TrakBox;
import org.jcodec.movtool.Flatten;

/**
 * Imports specified tracks into a movie
 * 
 * @author Stanislav Vitvitskiy
 *
 */
public class ImportTrack {
    private static final Flag FLAG_TYPE = Flag.flag("type", "t", "Track types ['video', 'audio', 'meta']");
    private static final Flag[] flags = { FLAG_TYPE };

    public static void main(String[] args) throws Exception {
        final Cmd cmd = MainUtils.parseArguments(args, flags);
        if (cmd.argsLength() < 2) {
            MainUtils.printHelpCmd("import", flags, Arrays.asList(new String[] { "to movie", "from movie" }));
            System.exit(-1);
            return;
        }
        SeekableByteChannel fromCh = null;
        String flag = cmd.getStringFlag(FLAG_TYPE);
        MP4TrackType tt = null;
        if ("video".equals(flag)) {
            tt = MP4TrackType.VIDEO;
        } else if ("audio".equals(flag)) {
            tt = MP4TrackType.SOUND;
        } else if ("meta".equals(flag)) {
            tt = MP4TrackType.META;
        } else if (flag != null) {
            System.err.println("Unsupported track type: " + flag);
            System.exit(1);
            return;
        }
        try {
            File to = new File(cmd.getArg(0));
            File from = new File(cmd.getArg(1));
            fromCh = NIOUtils.readableChannel(from);
            Movie toMv = MP4Util.createRefFullMovieFromFile(to);
            Movie fromMv = MP4Util.createRefFullMovieFromFile(from);

            for (TrakBox trak : fromMv.getMoov().getTracks()) {
                MP4TrackType trackType = TrakBox.getTrackType(trak);
                if (tt != null && trackType != tt)
                    continue;
                toMv.getMoov().appendTrack(toMv.getMoov().importTrack(fromMv.getMoov(), trak));
            }
            new Flatten().flattenOnTop(toMv, to);
        } finally {
            if (fromCh != null)
                fromCh.close();
        }
    }
}
