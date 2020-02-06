package org.jcodec.samples.mp4;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import org.jcodec.common.DemuxerTrackMeta;
import org.jcodec.common.SeekableDemuxerTrack;
import org.jcodec.common.TrackType;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Packet;
import org.jcodec.containers.mp4.boxes.TextMetaDataSampleEntry;
import org.jcodec.containers.mp4.demuxer.MP4Demuxer;
import org.jcodec.containers.mp4.demuxer.MP4DemuxerTrackMeta;
import org.jcodec.samples.mp4.Test1Proto.Words;
import org.jcodec.samples.mp4.Test2Proto.Tag;

public class PrintProtos {
    public static void main(String[] args) throws IOException {
        SeekableByteChannel ch = null;
        try {
            ch = NIOUtils.readableFileChannel(args[0]);
            MP4Demuxer demuxer = MP4Demuxer.createMP4Demuxer(ch);
            List<SeekableDemuxerTrack> tracks = demuxer.getTracks();
            for (SeekableDemuxerTrack track : tracks) {
                DemuxerTrackMeta meta = track.getMeta();
                TrackType type = meta.getType();
                System.out.println(meta.getIndex() + " - " + type);
                if (type == TrackType.META) {
                    MP4DemuxerTrackMeta mp4meta = (MP4DemuxerTrackMeta) meta;
                    TextMetaDataSampleEntry se = ((TextMetaDataSampleEntry) mp4meta.getSampleEntries()[0]);
                    System.out.println(se.getMimeFormat());
                    if ("application/transcription_1".equals(se.getMimeFormat())) {
                        printWords(track);
                    } else if ("application/audio_tags_1".equals(se.getMimeFormat())) {
                        printTags(track);
                    }
                }
            }
        } finally {
            NIOUtils.closeQuietly(ch);
        }
    }

    private static void printTags(SeekableDemuxerTrack track) throws IOException {
        System.out.println("Audio tags: ");
        Packet frame = null;
        while ((frame = track.nextFrame()) != null) {
            ByteBuffer data = frame.getData();
            while (data.remaining() >= 4) {
                int size = data.getInt();
                Tag tag = Tag.parseFrom(NIOUtils.read(data, size));
                System.out.println(tag);
            }
        }
    }

    private static void printWords(SeekableDemuxerTrack track) throws IOException {
        System.out.println("Transcriptions: ");
        Packet frame = null;
        while ((frame = track.nextFrame()) != null) {
            ByteBuffer data = frame.getData();
            while (data.remaining() >= 4) {
                int size = data.getInt();
                Words words = Words.parseFrom(NIOUtils.read(data, size));
                System.out.println(words);
            }
        }
    }
}
