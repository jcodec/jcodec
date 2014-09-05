package org.jcodec.containers.mp4.boxes;

import java.util.HashMap;
import java.util.Map;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Default box factory
 * 
 * @author The JCodec project
 * 
 */
public class BoxFactory {
    private Map<String, Class<? extends Box>> mappings = new HashMap<String, Class<? extends Box>>();
    private static BoxFactory instance = new BoxFactory();

    public static BoxFactory getDefault() {
        return instance;
    }

    public BoxFactory() {
        mappings.put(MovieExtendsBox.fourcc(), MovieExtendsBox.class);
        mappings.put(MovieExtendsHeaderBox.fourcc(), MovieExtendsHeaderBox.class);
        mappings.put(SegmentIndexBox.fourcc(), SegmentIndexBox.class);
        mappings.put(SegmentTypeBox.fourcc(), SegmentTypeBox.class);
        mappings.put(TrackExtendsBox.fourcc(), TrackExtendsBox.class);
        mappings.put(VideoMediaHeaderBox.fourcc(), VideoMediaHeaderBox.class);
        mappings.put(FileTypeBox.fourcc(), FileTypeBox.class);
        mappings.put(MovieBox.fourcc(), MovieBox.class);
        mappings.put(MovieHeaderBox.fourcc(), MovieHeaderBox.class);
        mappings.put(TrakBox.fourcc(), TrakBox.class);
        mappings.put(TrackHeaderBox.fourcc(), TrackHeaderBox.class);
        mappings.put("edts", NodeBox.class);
        mappings.put(EditListBox.fourcc(), EditListBox.class);
        mappings.put(MediaBox.fourcc(), MediaBox.class);
        mappings.put(MediaHeaderBox.fourcc(), MediaHeaderBox.class);
        mappings.put(MediaInfoBox.fourcc(), MediaInfoBox.class);
        mappings.put(HandlerBox.fourcc(), HandlerBox.class);
        mappings.put(DataInfoBox.fourcc(), DataInfoBox.class);
        mappings.put("stbl", NodeBox.class);
        mappings.put(SampleDescriptionBox.fourcc(), SampleDescriptionBox.class);
        mappings.put(TimeToSampleBox.fourcc(), TimeToSampleBox.class);
        mappings.put(SyncSamplesBox.fourcc(), SyncSamplesBox.class);
        mappings.put(PartialSyncSamplesBox.fourcc(), PartialSyncSamplesBox.class);
        mappings.put(SampleToChunkBox.fourcc(), SampleToChunkBox.class);
        mappings.put(SampleSizesBox.fourcc(), SampleSizesBox.class);
        mappings.put(ChunkOffsetsBox.fourcc(), ChunkOffsetsBox.class);
        mappings.put("mvex", NodeBox.class);
        mappings.put("moof", NodeBox.class);
        mappings.put("traf", NodeBox.class);
        mappings.put("mfra", NodeBox.class);
        mappings.put("skip", NodeBox.class);
        mappings.put("meta", LeafBox.class);
        mappings.put(DataRefBox.fourcc(), DataRefBox.class);
        mappings.put("ipro", NodeBox.class);
        mappings.put("sinf", NodeBox.class);
        mappings.put(ChunkOffsets64Box.fourcc(), ChunkOffsets64Box.class);
        mappings.put(SoundMediaHeaderBox.fourcc(), SoundMediaHeaderBox.class);
        mappings.put("clip", NodeBox.class);
        mappings.put(ClipRegionBox.fourcc(), ClipRegionBox.class);
        mappings.put(LoadSettingsBox.fourcc(), LoadSettingsBox.class);
        mappings.put("tapt", NodeBox.class);
        mappings.put("gmhd", NodeBox.class);
        mappings.put("tmcd", LeafBox.class);
        mappings.put("tref", NodeBox.class);
        mappings.put(ClearApertureBox.fourcc(), ClearApertureBox.class);
        mappings.put(ProductionApertureBox.fourcc(), ProductionApertureBox.class);
        mappings.put(EncodedPixelBox.fourcc(), EncodedPixelBox.class);
        mappings.put(GenericMediaInfoBox.fourcc(), GenericMediaInfoBox.class);
        mappings.put(TimecodeMediaInfoBox.fourcc(), TimecodeMediaInfoBox.class);
        mappings.put("udta", NodeBox.class);
        mappings.put(CompositionOffsetsBox.fourcc(), CompositionOffsetsBox.class);
        mappings.put(NameBox.fourcc(), NameBox.class);

        mappings.put(MovieFragmentHeaderBox.fourcc(), MovieFragmentHeaderBox.class);
        mappings.put(TrackFragmentHeaderBox.fourcc(), TrackFragmentHeaderBox.class);
        mappings.put(MovieFragmentBox.fourcc(), MovieFragmentBox.class);
        mappings.put(TrackFragmentBox.fourcc(), TrackFragmentBox.class);
        mappings.put(TrackFragmentBaseMediaDecodeTimeBox.fourcc(), TrackFragmentBaseMediaDecodeTimeBox.class);
        mappings.put(TrunBox.fourcc(), TrunBox.class);
    }

    public void override(String fourcc, Class<? extends Box> cls) {
        mappings.put(fourcc, cls);
    }
    
    public void clear() {
        mappings.clear();
    }

    public Class<? extends Box> toClass(String fourcc) {
        return mappings.get(fourcc);
    }
}