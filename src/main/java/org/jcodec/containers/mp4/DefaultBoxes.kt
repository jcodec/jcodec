package org.jcodec.containers.mp4

import org.jcodec.containers.mp4.boxes.*
import org.jcodec.containers.mp4.boxes.Box.LeafBox

open class DefaultBoxes : Boxes() {
    init {
        mappings[MovieExtendsBox.fourcc()] = MovieExtendsBox::class.java
        mappings[MovieExtendsHeaderBox.fourcc()] = MovieExtendsHeaderBox::class.java
        mappings[SegmentIndexBox.fourcc()] = SegmentIndexBox::class.java
        mappings[SegmentTypeBox.fourcc()] = SegmentTypeBox::class.java
        mappings[TrackExtendsBox.fourcc()] = TrackExtendsBox::class.java
        mappings[VideoMediaHeaderBox.fourcc()] = VideoMediaHeaderBox::class.java
        mappings[FileTypeBox.fourcc()] = FileTypeBox::class.java
        mappings[MovieBox.fourcc()] = MovieBox::class.java
        mappings[MovieHeaderBox.fourcc()] = MovieHeaderBox::class.java
        mappings[TrakBox.fourcc()] = TrakBox::class.java
        mappings[TrackHeaderBox.fourcc()] = TrackHeaderBox::class.java
        mappings["edts"] = NodeBox::class.java
        mappings[EditListBox.fourcc()] = EditListBox::class.java
        mappings[MediaBox.fourcc()] = MediaBox::class.java
        mappings[MediaHeaderBox.fourcc()] = MediaHeaderBox::class.java
        mappings[MediaInfoBox.fourcc()] = MediaInfoBox::class.java
        mappings[HandlerBox.FOURCC] = HandlerBox::class.java
        mappings[DataInfoBox.fourcc()] = DataInfoBox::class.java
        mappings["stbl"] = NodeBox::class.java
        mappings[SampleDescriptionBox.fourcc()] = SampleDescriptionBox::class.java
        mappings[TimeToSampleBox.fourcc()] = TimeToSampleBox::class.java
        mappings[SyncSamplesBox.STSS] = SyncSamplesBox::class.java
        mappings[PartialSyncSamplesBox.STPS] = PartialSyncSamplesBox::class.java
        mappings[SampleToChunkBox.fourcc()] = SampleToChunkBox::class.java
        mappings[SampleSizesBox.fourcc()] = SampleSizesBox::class.java
        mappings[ChunkOffsetsBox.fourcc()] = ChunkOffsetsBox::class.java
        mappings["keys"] = KeysBox::class.java
        mappings[IListBox.fourcc()] = IListBox::class.java
        mappings["mvex"] = NodeBox::class.java
        mappings["moof"] = NodeBox::class.java
        mappings["traf"] = NodeBox::class.java
        mappings["mfra"] = NodeBox::class.java
        mappings["skip"] = NodeBox::class.java
        mappings[MetaBox.fourcc()] = MetaBox::class.java
        mappings[DataRefBox.fourcc()] = DataRefBox::class.java
        mappings["ipro"] = NodeBox::class.java
        mappings["sinf"] = NodeBox::class.java
        mappings[ChunkOffsets64Box.fourcc()] = ChunkOffsets64Box::class.java
        mappings[SoundMediaHeaderBox.fourcc()] = SoundMediaHeaderBox::class.java
        mappings["clip"] = NodeBox::class.java
        mappings[ClipRegionBox.fourcc()] = ClipRegionBox::class.java
        mappings[LoadSettingsBox.fourcc()] = LoadSettingsBox::class.java
        mappings["tapt"] = NodeBox::class.java
        mappings["gmhd"] = NodeBox::class.java
        mappings["tmcd"] = LeafBox::class.java
        mappings["tref"] = NodeBox::class.java
        mappings[ClearApertureBox.CLEF] = ClearApertureBox::class.java
        mappings[ProductionApertureBox.PROF] = ProductionApertureBox::class.java
        mappings[EncodedPixelBox.ENOF] = EncodedPixelBox::class.java
        mappings[GenericMediaInfoBox.fourcc()] = GenericMediaInfoBox::class.java
        mappings[TimecodeMediaInfoBox.fourcc()] = TimecodeMediaInfoBox::class.java
        mappings[UdtaBox.fourcc()] = UdtaBox::class.java
        mappings[CompositionOffsetsBox.fourcc()] = CompositionOffsetsBox::class.java
        mappings[NameBox.fourcc()] = NameBox::class.java
        mappings["mdta"] = LeafBox::class.java
        mappings[MovieFragmentHeaderBox.fourcc()] = MovieFragmentHeaderBox::class.java
        mappings[TrackFragmentHeaderBox.fourcc()] = TrackFragmentHeaderBox::class.java
        mappings[MovieFragmentBox.fourcc()] = MovieFragmentBox::class.java
        mappings[TrackFragmentBox.fourcc()] = TrackFragmentBox::class.java
        mappings[TrackFragmentBaseMediaDecodeTimeBox.fourcc()] = TrackFragmentBaseMediaDecodeTimeBox::class.java
        mappings[TrunBox.fourcc()] = TrunBox::class.java
    }
}