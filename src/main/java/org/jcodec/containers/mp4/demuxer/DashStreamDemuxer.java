package org.jcodec.containers.mp4.demuxer;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

import org.jcodec.api.JCodecException;
import org.jcodec.common.Codec;
import org.jcodec.common.Demuxer;
import org.jcodec.common.DemuxerTrack;
import org.jcodec.common.DemuxerTrackMeta;
import org.jcodec.common.SeekableDemuxerTrack;
import org.jcodec.common.TrackType;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.Packet;
import org.jcodec.containers.mp4.MP4Packet;
import org.jcodec.containers.mp4.MPDModel;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Demuxes one track out of multiple DASH fragments
 * 
 * @author The JCodec project
 * 
 */
public class DashStreamDemuxer implements Demuxer {
    private List<DashStreamDemuxerTrack> tracks;
    private List<SeekableDemuxerTrack> coded;
    private URL url;
    private MPDModel.MPD mpd;

    public static int INIT_SIZE = 3;

    public DashStreamDemuxer(URL url) throws IOException {
        this.url = url;
        this.tracks = new LinkedList<DashStreamDemuxer.DashStreamDemuxerTrack>();
        this.coded = new LinkedList<SeekableDemuxerTrack>();
        try {
            this.mpd = MPDModel.parse(url);
            if (this.mpd != null && this.mpd.periods != null && this.mpd.periods.size() > 0) {
                MPDModel.Period period = this.mpd.periods.get(0);
                for (MPDModel.AdaptationSet adaptationSet : period.adaptationSets) {
                    DashStreamDemuxerTrack tr = new DashStreamDemuxerTrack(url, adaptationSet, period);
                    tracks.add(tr);
                    coded.add(new CodecMP4DemuxerTrack(tr));
                }
            }
        } catch (JCodecException e) {
            throw new IOException(e);
        }
    }

    static class DashStreamDemuxerTrack implements SeekableDemuxerTrack, Closeable {
        private MPDModel.AdaptationSet adaptationSet;
        private URL url;
        private File initFile;
        private String selectedRprz;
        private Map<Integer, Future<DashMP4DemuxerTrack>> fragments = new HashMap<Integer, Future<DashMP4DemuxerTrack>>();
        private int curFragNo;
        private boolean streaming;
        private int maxDownloadAttampts = 1024;
        private ExecutorService threadPool;
        private int globalFrame;
        private MPDModel.Period period;
        private int frameRate;
        private double segmentDuration;
        private int[] seekFrames;
        static int next_id = 0;
        int id;

        public DashStreamDemuxerTrack(URL url, MPDModel.AdaptationSet adaptationSet, MPDModel.Period period)
                throws IOException {
            this.url = url;
            this.period = period;
            this.adaptationSet = adaptationSet;
            this.threadPool = Executors.newFixedThreadPool(INIT_SIZE, new ThreadFactory() {
                public Thread newThread(Runnable r) {
                    Thread t = Executors.defaultThreadFactory().newThread(r);
                    t.setDaemon(true);
                    return t;
                }
            });
            if (adaptationSet.representations.size() > 0) {
                MPDModel.Representation rprz = adaptationSet.representations.get(0);
                selectedRprz = rprz.id;
                frameRate = rprz.frameRate;
                MPDModel.SegmentTemplate stpl = getSegmentTemplate();
                segmentDuration = stpl != null ? ((double) stpl.duration) / stpl.timescale : period.duration.sec;

                downloadInit();
                for (int i = 0; i < INIT_SIZE; i++) {
                    scheduleFragment(i);
                }
                int numSeg = (int) (period.duration.sec / segmentDuration);
                int segmentFrames = (int) (segmentDuration * frameRate);
                seekFrames = new int[numSeg];
                for (int i = 0, tmp = 0; i < numSeg; i++, tmp += segmentFrames) {
                    seekFrames[i] = tmp + 1;
                }
            }
            id = next_id++;
        }

        private MPDModel.Representation getRrprz() {
            for (MPDModel.Representation rprz : adaptationSet.representations) {
                if (rprz.id.equals(selectedRprz))
                    return rprz;
            }
            return null;
        }

        private MPDModel.SegmentTemplate getSegmentTemplate() {
            MPDModel.Representation rprz = getRrprz();
            return adaptationSet.segmentTemplate == null ? rprz.segmentTemplate : adaptationSet.segmentTemplate;
        }

        private static void sleepQuiet(int millis) {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
            }
        }

        private DashMP4DemuxerTrack downloadFrag(int no) throws IOException {
            MPDModel.Representation rprz = getRrprz();
            MPDModel.SegmentTemplate stpl = getSegmentTemplate();
            for (int i = 0; i < maxDownloadAttampts; i++) {
                try {
                    URL urlInit = null;
                    if (stpl != null && stpl.media != null) {
                        Map<String, Object> vals = new HashMap<String, Object>();
                        vals.put("RepresentationID", selectedRprz);
                        vals.put("Number", stpl.startNumber + no);
                        String tmp = fillTemplate(stpl.media, vals);

                        urlInit = new URL(url, tmp);
                    } else if (rprz.segmentList != null && rprz.segmentList.segmentUrls.size() > no) {
                        MPDModel.SegmentURL segmentURL = rprz.segmentList.segmentUrls.get(no);
                        urlInit = new URL(url, segmentURL.media);
                    }
                    if (urlInit != null) {
                        File tempFile = File.createTempFile("org.jcodec", fileName(urlInit.getPath()));
                        System.out.println("Fetching fragment: " + urlInit.toExternalForm());
                        NIOUtils.fetchUrl(urlInit, tempFile);
                        DashMP4DemuxerTrack demuxer = DashMP4DemuxerTrack
                                .createFromFiles(Arrays.asList(new File[] { initFile, tempFile }));
                        demuxer.setDurationHint(segmentDuration);
                        return demuxer;
                    } else if (rprz.baseURL != null) {
                        DashMP4DemuxerTrack demuxer = DashMP4DemuxerTrack
                                .createFromFiles(Arrays.asList(new File[] { initFile, initFile }));
                        return demuxer;
                    }
                    break;
                } catch (FileNotFoundException e) {
                    if (!streaming)
                        return null;
                    sleepQuiet(100);
                }
            }
            return null;
        }

        private String fillTemplate(String media, Map<String, Object> vals) {
            StringBuilder builder = new StringBuilder();
            char[] charArray = media.toCharArray();
            int varStart = 0;
            for (int i = 0; i < charArray.length; i++) {
                if ('$' == charArray[i]) {
                    if (varStart == 0) {
                        varStart = i + 1;
                    } else {
                        String var = new String(charArray, varStart, i - varStart);
                        int formatStart = var.indexOf('%');
                        if (formatStart != -1) {
                            String format = var.substring(formatStart);
                            var = var.substring(0, formatStart);
                            Object object = vals.get(var);
                            if (object != null) {
                                String val = String.format(format, object);
                                builder.append(val);
                            }
                        } else {
                            Object object = vals.get(var);
                            if (object != null) {
                                builder.append(String.valueOf(object));
                            }
                        }
                        varStart = 0;
                    }
                } else if (varStart == 0) {
                    builder.append(charArray[i]);
                }
            }
            return builder.toString();
        }

        private String fileName(String path) {
            String[] split = path.split("/");
            return split[split.length - 1];
        }

        private void downloadInit() throws IOException {
            MPDModel.Representation rprz = getRrprz();
            if (getSegmentTemplate() != null && getSegmentTemplate().initialization != null) {
                String tmp = getSegmentTemplate().initialization.replace("$RepresentationID$", selectedRprz);
                URL urlInit = new URL(url, tmp);
                File tempFile = File.createTempFile("org.jcodec", fileName(urlInit.getPath()));
                System.out.println("Fetching init: " + urlInit.toExternalForm());
                NIOUtils.fetchUrl(urlInit, tempFile);
                initFile = tempFile;
            } else if (rprz.baseURL != null) {
                URL urlInit = new URL(url, rprz.baseURL);
                File tempFile = File.createTempFile("org.jcodec", fileName(urlInit.getPath()));
                System.out.println("Fetching init: " + urlInit.toExternalForm());
                NIOUtils.fetchUrl(urlInit, tempFile);
                initFile = tempFile;
            } else if (rprz.segmentList != null && rprz.segmentList.initialization != null) {
                URL urlInit = new URL(url, rprz.segmentList.initialization.sourceURL);
                File tempFile = File.createTempFile("org.jcodec", fileName(urlInit.getPath()));
                System.out.println("Fetching init: " + urlInit.toExternalForm());
                NIOUtils.fetchUrl(urlInit, tempFile);
                initFile = tempFile;
            }
        }

        @Override
        public Packet nextFrame() throws IOException {
            try {
                Future<DashMP4DemuxerTrack> curFrag = fragments.get(curFragNo);
                MP4Packet nextFrame = null;
                if (curFrag != null) {
                    nextFrame = getCurFrag(curFrag).nextFrame();
                    if (nextFrame == null) {
                        getCurFrag(curFrag).close();
                        fragments.put(curFragNo, null);
                        curFragNo++;
                    }
                }
                if (nextFrame != null) {
                    ++globalFrame;
                    return setPts(nextFrame);
                }

                curFrag = fragments.get(curFragNo);

                if (curFrag == null) {
                    for (int i = curFragNo; i < curFragNo + INIT_SIZE; i++)
                        scheduleFragment(curFragNo);
                    curFrag = fragments.get(curFragNo);
                }

                if (curFrag == null)
                    return null;

                ++globalFrame;
                return setPts(getCurFrag(curFrag).nextFrame());
            } catch (ExecutionException e) {
                throw new RuntimeException("Execution problem", e);
            }
        }

        private DashMP4DemuxerTrack getCurFrag(Future<DashMP4DemuxerTrack> curFrag) throws ExecutionException {
            while (true) {
                try {
                    return curFrag.get();
                } catch (InterruptedException e) {
                }
            }
        }

        private MP4Packet setPts(MP4Packet frame) {
            double off = curFragNo * segmentDuration;
            frame.setPts((long) (frame.getPts() + off * frame.getTimescale()));
            frame.setMediaPts((long) (frame.getMediaPts() + off * frame.getTimescale()));
            frame.setFrameNo(globalFrame - 1);
            if (id == 5)
            System.out.println(
                    String.format("[%d] PTS: %f DUR: %s", id, (float) frame.getPtsD(), (float) frame.getDurationD()));
            return frame;
        }

        private void scheduleFragment(final int fragNo) {
            if (fragments.get(fragNo) == null) {
                Future<DashMP4DemuxerTrack> future = threadPool.submit(new Callable<DashMP4DemuxerTrack>() {
                    @Override
                    public DashMP4DemuxerTrack call() throws Exception {
                        return downloadFrag(fragNo);
                    }
                });
                fragments.put(fragNo, future);
            }
        }

        @Override
        public DemuxerTrackMeta getMeta() {
            Future<DashMP4DemuxerTrack> future = fragments.get(curFragNo);
            if (future == null)
                return null;
            try {
                DashMP4DemuxerTrack frag = getCurFrag(future);

                MP4DemuxerTrackMeta fragMeta = (MP4DemuxerTrackMeta) frag.getMeta();
                double totalDuration = period.duration.sec;
                int totalFrames = (int) period.duration.sec * frameRate;
                return new MP4DemuxerTrackMeta(fragMeta.getType(), fragMeta.getCodec(), totalDuration, seekFrames,
                        totalFrames, fragMeta.getCodecPrivate(), fragMeta.getVideoCodecMeta(),
                        fragMeta.getAudioCodecMeta(), fragMeta.getSampleEntries(), fragMeta.getCodecPrivateOpaque());
            } catch (ExecutionException e) {
                throw new RuntimeException("Execution problem", e);
            }
        }

        @Override
        public void close() throws IOException {
            Set<Entry<Integer, Future<DashMP4DemuxerTrack>>> entrySet = fragments.entrySet();
            for (Entry<Integer, Future<DashMP4DemuxerTrack>> entry : entrySet) {
                if (entry.getValue() != null) {
                    try {
                        getCurFrag(entry.getValue()).close();
                    } catch (ExecutionException e) {
                        throw new IOException("Execution problem", e);
                    }
                    entry.setValue(null);
                }
            }
        }

        @Override
        public boolean gotoFrame(long frameNo) throws IOException {
            if (frameNo != 0)
                return false;
            curFragNo = 0;
            globalFrame = 0;
            for (int i = 0; i < INIT_SIZE; i++) {
                scheduleFragment(i);
            }
            return true;
        }

        @Override
        public boolean gotoSyncFrame(long frameNo) throws IOException {
            return false;
        }

        @Override
        public long getCurFrame() {
            return globalFrame;
        }

        @Override
        public void seek(double second) throws IOException {
            throw new RuntimeException("unimpl");
        }
    }

    @Override
    public void close() throws IOException {
        for (DashStreamDemuxerTrack track : tracks) {
            track.close();
        }
    }

    @Override
    public List<? extends DemuxerTrack> getTracks() {
        return coded;
    }

    @Override
    public List<? extends DemuxerTrack> getVideoTracks() {
        ArrayList<SeekableDemuxerTrack> result = new ArrayList<SeekableDemuxerTrack>();
        for (SeekableDemuxerTrack demuxerTrack : coded) {
            DemuxerTrackMeta meta = demuxerTrack.getMeta();
            if (meta.getType() == TrackType.VIDEO)
                result.add(demuxerTrack);
        }
        return result;
    }

    @Override
    public List<? extends DemuxerTrack> getAudioTracks() {
        ArrayList<SeekableDemuxerTrack> result = new ArrayList<SeekableDemuxerTrack>();
        for (SeekableDemuxerTrack demuxerTrack : coded) {
            DemuxerTrackMeta meta = demuxerTrack.getMeta();
            if (meta.getType() == TrackType.AUDIO)
                result.add(demuxerTrack);
        }
        return result;
    }
}