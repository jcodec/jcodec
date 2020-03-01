package org.jcodec.movtool;

import java.io.File;

import org.jcodec.common.logging.Logger;
import org.jcodec.common.model.RationalLarge;
import org.jcodec.common.tools.MainUtils;
import org.jcodec.common.tools.MainUtils.Cmd;
import org.jcodec.containers.mp4.boxes.MovieBox;
import org.jcodec.containers.mp4.boxes.MovieFragmentBox;
import org.jcodec.containers.mp4.boxes.TimeToSampleBox;
import org.jcodec.containers.mp4.boxes.TimeToSampleBox.TimeToSampleEntry;
import org.jcodec.containers.mp4.boxes.TrakBox;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * <p>
 * Changes FPS on an MP4 file.
 *
 * @author Stan Vitvitskyy
 */
public class SetFPS {
    private static final int MIN_TIMESCALE_ALLOWED = 25;

    public static class SetFPSEdit implements MP4Edit {
        private RationalLarge newFPS;

        public SetFPSEdit(RationalLarge newFPS) {
            this.newFPS = newFPS;
        }

        @Override
        public void apply(MovieBox mov) {
            TrakBox vt = mov.getVideoTrack();
            TimeToSampleBox stts = vt.getStts();
            TimeToSampleEntry[] entries = stts.getEntries();
            long nSamples = 0;
            long totalDuration = 0;
            for (TimeToSampleEntry e : entries) {
                nSamples += e.getSampleCount();
                totalDuration += e.getSampleCount() * e.getSampleDuration();
            }

            int newTimescale = (int) newFPS.multiply(new RationalLarge(totalDuration, nSamples)).scalarClip();
            if (newTimescale >= MIN_TIMESCALE_ALLOWED) {
                // Playing with timescale if possible
                vt.setTimescale(newTimescale);
                long newDuration = totalDuration * mov.getTimescale() / vt.getTimescale();
                mov.setDuration(newDuration);
                vt.setDuration(newDuration);
            } else {
                // Playing with actual sample durations
                double mul = new RationalLarge(vt.getTimescale() * totalDuration, nSamples).divideBy(newFPS)
                        .scalar();
                Logger.info("Applying multiplier to sample durations: " + mul);
                for (TimeToSampleEntry e : entries) {
                    e.setSampleDuration((int) (e.getSampleDuration() * mul * 100));
                }
                vt.setTimescale(vt.getTimescale() * 100);
            }

            if (newTimescale != vt.getTimescale()) {
                Logger.info("Changing timescale to: " + vt.getTimescale());
                long newDuration = totalDuration * mov.getTimescale() / vt.getTimescale();
                mov.setDuration(newDuration);
                vt.setDuration(newDuration);
            }
        }

        @Override
        public void applyToFragment(MovieBox mov, MovieFragmentBox[] fragmentBox) {
            throw new RuntimeException("Unsupported");
        }
    }

    public static void main1(String[] args) throws Exception {
        Cmd cmd = MainUtils.parseArguments(args, new MainUtils.Flag[]{});
        if (cmd.argsLength() < 2) {
            MainUtils.printHelpNoFlags("movie", "num:den");
            System.exit(-1);
        }
        final RationalLarge newFPS = RationalLarge.parse(cmd.getArg(1));

        new InplaceMP4Editor().modify(new File(cmd.getArg(0)), new SetFPSEdit(newFPS));
    }
}
