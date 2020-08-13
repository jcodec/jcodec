package org.jcodec.containers.mp4;
import org.jcodec.common.model.RationalLarge;
import org.jcodec.containers.mp4.boxes.Box;
import org.jcodec.containers.mp4.boxes.Edit;
import org.jcodec.containers.mp4.boxes.MovieBox;
import org.jcodec.containers.mp4.boxes.NodeBox;
import org.jcodec.containers.mp4.boxes.TimeToSampleBox;
import org.jcodec.containers.mp4.boxes.TimeToSampleBox.TimeToSampleEntry;
import org.jcodec.containers.mp4.boxes.TimecodeSampleEntry;
import org.jcodec.containers.mp4.boxes.TrakBox;
import org.jcodec.containers.mp4.demuxer.TimecodeMP4DemuxerTrack;

import java.io.IOException;
import java.util.List;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Quicktime time conversion utilities
 * 
 * @author The JCodec project
 * 
 */
public class QTTimeUtil {

    /**
     * Calculates track duration considering edits
     * 
     * @param track
     * @return
     */
    public static long getEditedDuration(TrakBox track) {
        List<Edit> edits = track.getEdits();
        if (edits == null)
            return track.getDuration();

        long duration = 0;
        for (Edit edit : edits) {
            duration += edit.getDuration();
        }
        return duration;
    }

    /**
     * Finds timevalue of a frame number
     * 
     * might be an expensive operation sinse it traverses compressed time to
     * sample table
     * 
     * @param frameNumber
     * @return
     */
    public static long frameToTimevalue(TrakBox trak, int frameNumber) {
        TimeToSampleBox stts = NodeBox.findFirstPath(trak, TimeToSampleBox.class, Box.path("mdia.minf.stbl.stts"));
        TimeToSampleEntry[] timeToSamples = stts.getEntries();
        long pts = 0;
        int sttsInd = 0, sttsSubInd = frameNumber;
        while (sttsSubInd >= timeToSamples[sttsInd].getSampleCount()) {
            sttsSubInd -= timeToSamples[sttsInd].getSampleCount();
            pts += timeToSamples[sttsInd].getSampleCount() * timeToSamples[sttsInd].getSampleDuration();
            sttsInd++;
        }

        return pts + timeToSamples[sttsInd].getSampleDuration() * sttsSubInd;
    }

    /**
     * Finds frame by timevalue
     * 
     * @param tv
     * @return
     */
    public static int timevalueToFrame(TrakBox trak, long tv) {
        TimeToSampleEntry[] tts = NodeBox.findFirstPath(trak, TimeToSampleBox.class, Box.path("mdia.minf.stbl.stts")).getEntries();
        int frame = 0;
        for (int i = 0; tv > 0 && i < tts.length; i++) {
            long rem = tv / tts[i].getSampleDuration();
            tv -= tts[i].getSampleCount() * tts[i].getSampleDuration();
            frame += tv > 0 ? tts[i].getSampleCount() : rem;
        }

        return frame;
    }

    /**
     * Converts media timevalue to edited timevalue
     * 
     * @param trak
     * @param mediaTv
     * @param movieTimescale
     * @return
     */
    public static long mediaToEdited(TrakBox trak, long mediaTv, int movieTimescale) {
        if (trak.getEdits() == null || trak.getEdits().isEmpty())
            return mediaTv;
        long accum = 0;
        for (Edit edit : trak.getEdits()) {
            if (mediaTv < edit.getMediaTime())
                return accum;
            long duration = trak.rescale(edit.getDuration(), movieTimescale);
            if (edit.getMediaTime() != -1
                    && (mediaTv >= edit.getMediaTime() && mediaTv < edit.getMediaTime() + duration)) {
                accum += mediaTv - edit.getMediaTime();
                break;
            }
            accum += duration;
        }

        return accum;
    }

    /**
     * Converts edited timevalue to media timevalue
     * 
     * @param trak
     * @param mediaTv
     * @param movieTimescale
     * @return
     */
    public static long editedToMedia(TrakBox trak, long editedTv, int movieTimescale) {
        if (trak.getEdits() == null)
            return editedTv;
        long accum = 0;
        for (Edit edit : trak.getEdits()) {
            long duration = trak.rescale(edit.getDuration(), movieTimescale);

            if (accum + duration > editedTv) {
                return edit.getMediaTime() + editedTv - accum;
            }

            accum += duration;
        }

        return accum;
    }

    /**
     * Calculates frame number as it shows in quicktime player
     * 
     * @param movie
     * @param mediaFrameNo
     * @return
     */
    public static int qtPlayerFrameNo(MovieBox movie, int mediaFrameNo) {
        TrakBox videoTrack = movie.getVideoTrack();

        long editedTv = mediaToEdited(videoTrack, frameToTimevalue(videoTrack, mediaFrameNo), movie.getTimescale());

        return tv2QTFrameNo(movie, editedTv);
    }

    public static int tv2QTFrameNo(MovieBox movie, long tv) {
        TrakBox videoTrack = movie.getVideoTrack();
        TrakBox timecodeTrack = movie.getTimecodeTrack();
        
        if (timecodeTrack != null && BoxUtil.containsBox2(videoTrack, "tref", "tmcd")) {
            return timevalueToTimecodeFrame(timecodeTrack, new RationalLarge(tv, videoTrack.getTimescale()),
                    movie.getTimescale());
        } else {
            return timevalueToFrame(videoTrack, tv);
        }
    }

    /**
     * Calculates and formats standard time as in Quicktime player
     * 
     * @param movie
     * @param mediaFrameNo
     * @return
     */
    public static String qtPlayerTime(MovieBox movie, int mediaFrameNo) {
        TrakBox videoTrack = movie.getVideoTrack();
        long editedTv = mediaToEdited(videoTrack, frameToTimevalue(videoTrack, mediaFrameNo), movie.getTimescale());

        int sec = (int) (editedTv / videoTrack.getTimescale());
        return String.format("%02d", sec / 3600) + "_" + String.format("%02d", (sec % 3600) / 60) + "_"
                + String.format("%02d", sec % 60);
    }

    /**
     * Calculates and formats tape timecode as in Quicktime player
     * 
     * @param timecodeTrack
     * @param tv
     * @param startCounter
     * @return
     * @throws IOException
     */
    public static String qtPlayerTimecodeFromMovie(MovieBox movie, TimecodeMP4DemuxerTrack timecodeTrack, int mediaFrameNo)
            throws IOException {
        TrakBox videoTrack = movie.getVideoTrack();
        long editedTv = mediaToEdited(videoTrack, frameToTimevalue(videoTrack, mediaFrameNo), movie.getTimescale());

        TrakBox tt = timecodeTrack.getBox();
        int ttTimescale = tt.getTimescale();
        long ttTv = editedToMedia(tt, editedTv * ttTimescale / videoTrack.getTimescale(), movie.getTimescale());

        return formatTimecode(
                timecodeTrack.getBox(),
                timecodeTrack.getStartTimecode()
                        + timevalueToTimecodeFrame(timecodeTrack.getBox(), new RationalLarge(ttTv, ttTimescale),
                                movie.getTimescale()));
    }

    /**
     * Calculates and formats tape timecode as in Quicktime player
     * 
     * @param timecodeTrack
     * @param tv
     * @param startCounter
     * @return
     * @throws IOException
     */
    public static String qtPlayerTimecode(TimecodeMP4DemuxerTrack timecodeTrack, RationalLarge tv, int movieTimescale)
            throws IOException {
        TrakBox tt = timecodeTrack.getBox();
        int ttTimescale = tt.getTimescale();
        long ttTv = editedToMedia(tt, tv.multiplyS(ttTimescale), movieTimescale);

        return formatTimecode(
                timecodeTrack.getBox(),
                timecodeTrack.getStartTimecode()
                        + timevalueToTimecodeFrame(timecodeTrack.getBox(), new RationalLarge(ttTv, ttTimescale),
                                movieTimescale));
    }

    /**
     * Converts timevalue to frame number based on timecode track
     * 
     * @param timecodeTrack
     * @param tv
     * @return
     */
    public static int timevalueToTimecodeFrame(TrakBox timecodeTrack, RationalLarge tv, int movieTimescale) {
        TimecodeSampleEntry se = (TimecodeSampleEntry) timecodeTrack.getSampleEntries()[0];
        return (int) ((2 * tv.multiplyS(se.getTimescale()) / se.getFrameDuration()) + 1) / 2;
    }

    /**
     * Formats tape timecode based on frame counter
     * 
     * @param timecodeTrack
     * @param counter
     * @return
     */
    public static String formatTimecode(TrakBox timecodeTrack, int counter) {
        TimecodeSampleEntry tmcd = NodeBox.findFirstPath(timecodeTrack, TimecodeSampleEntry.class, Box.path("mdia.minf.stbl.stsd.tmcd"));
        byte nf = tmcd.getNumFrames();

        String tc = String.format("%02d", counter % nf);
        counter /= nf;
        tc = String.format("%02d", counter % 60) + ":" + tc;
        counter /= 60;
        tc = String.format("%02d", counter % 60) + ":" + tc;
        counter /= 60;
        tc = String.format("%02d", counter) + ":" + tc;

        return tc;
    }
}
