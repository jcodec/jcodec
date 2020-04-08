package org.jcodec.containers.mkv

import org.jcodec.containers.mkv.boxes.*
import org.jcodec.containers.mkv.util.EbmlUtil.toHexString
import org.jcodec.platform.Platform
import java.util.*

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed under FreeBSD License
 *
 * EBML IO implementation
 *
 * @author The JCodec project
 */
class MKVType private constructor(private val _name: String, @JvmField val id: ByteArray, val clazz: Class<out EbmlBase>) {
    fun name(): String {
        return _name
    }

    override fun toString(): String {
        return _name
    }

    companion object {
        private val _values: MutableList<MKVType> = ArrayList()

        // EBML Id's
        val Void = MKVType("Void", byteArrayOf(0xEC.toByte()), EbmlVoid::class.java)
        val CRC32 = MKVType("CRC32", byteArrayOf(0xBF.toByte()), EbmlBin::class.java)

        @JvmField
        val EBML = MKVType("EBML", byteArrayOf(0x1A, 0x45, 0xDF.toByte(), 0xA3.toByte()), EbmlMaster::class.java)

        @JvmField
        val EBMLVersion = MKVType("EBMLVersion", byteArrayOf(0x42, 0x86.toByte()), EbmlUint::class.java)

        @JvmField
        val EBMLReadVersion = MKVType("EBMLReadVersion", byteArrayOf(0x42, 0xF7.toByte()), EbmlUint::class.java)

        @JvmField
        val EBMLMaxIDLength = MKVType("EBMLMaxIDLength", byteArrayOf(0x42, 0xF2.toByte()), EbmlUint::class.java)

        @JvmField
        val EBMLMaxSizeLength = MKVType("EBMLMaxSizeLength", byteArrayOf(0x42, 0xF3.toByte()), EbmlUint::class.java)

        //All strings are UTF8 in java, this EbmlBase is specified as pure ASCII EbmlBase in Matroska spec
        @JvmField
        val DocType = MKVType("DocType", byteArrayOf(0x42, 0x82.toByte()), EbmlString::class.java)

        @JvmField
        val DocTypeVersion = MKVType("DocTypeVersion", byteArrayOf(0x42, 0x87.toByte()), EbmlUint::class.java)

        @JvmField
        val DocTypeReadVersion = MKVType("DocTypeReadVersion", byteArrayOf(0x42, 0x85.toByte()), EbmlUint::class.java)

        @JvmField
        val Segment = MKVType("Segment", MkvSegment.SEGMENT_ID, MkvSegment::class.java)

        @JvmField
        val SeekHead = MKVType("SeekHead", byteArrayOf(0x11, 0x4D, 0x9B.toByte(), 0x74), EbmlMaster::class.java)

        @JvmField
        val Seek = MKVType("Seek", byteArrayOf(0x4D, 0xBB.toByte()), EbmlMaster::class.java)

        @JvmField
        val SeekID = MKVType("SeekID", byteArrayOf(0x53, 0xAB.toByte()), EbmlBin::class.java)

        @JvmField
        val SeekPosition = MKVType("SeekPosition", byteArrayOf(0x53, 0xAC.toByte()), EbmlUint::class.java)

        @JvmField
        val Info = MKVType("Info", byteArrayOf(0x15, 0x49.toByte(), 0xA9.toByte(), 0x66.toByte()), EbmlMaster::class.java)
        val SegmentUID = MKVType("SegmentUID", byteArrayOf(0x73, 0xA4.toByte()), EbmlBin::class.java)

        //All strings are UTF8 in java
        val SegmentFilename = MKVType("SegmentFilename", byteArrayOf(0x73, 0x84.toByte()), EbmlString::class.java)
        val PrevUID = MKVType("PrevUID", byteArrayOf(0x3C, 0xB9.toByte(), 0x23), EbmlBin::class.java)
        val PrevFilename = MKVType("PrevFilename", byteArrayOf(0x3C, 0x83.toByte(), 0xAB.toByte()), EbmlString::class.java)
        val NextUID = MKVType("NextUID", byteArrayOf(0x3E, 0xB9.toByte(), 0x23), EbmlBin::class.java)

        //An escaped filename corresponding to the next segment.
        val NextFilenam = MKVType("NextFilenam", byteArrayOf(0x3E, 0x83.toByte(), 0xBB.toByte()), EbmlString::class.java)

        // A randomly generated unique ID that all segments related to each other must use (128 bits).
        val SegmentFamily = MKVType("SegmentFamily", byteArrayOf(0x44, 0x44), EbmlBin::class.java)

        // A tuple of corresponding ID used by chapter codecs to represent this segment.
        val ChapterTranslate = MKVType("ChapterTranslate", byteArrayOf(0x69, 0x24), EbmlMaster::class.java)

        //Specify an edition UID on which this correspondance applies. When not specified, it means for all editions found in the segment.
        val ChapterTranslateEditionUID = MKVType("ChapterTranslateEditionUID", byteArrayOf(0x69, 0xFC.toByte()), EbmlUint::class.java)

        //The chapter codec using this ID (0: Matroska Script, 1: DVD-menu).
        val ChapterTranslateCodec = MKVType("ChapterTranslateCodec", byteArrayOf(0x69, 0xBF.toByte()), EbmlUint::class.java)

        // The binary value used to represent this segment in the chapter codec data. The format depends on the ChapProcessCodecID used.
        val ChapterTranslateID = MKVType("ChapterTranslateID", byteArrayOf(0x69, 0xA5.toByte()), EbmlBin::class.java)

        // Timecode scale in nanoseconds (1.000.000 means all timecodes in the segment are expressed in milliseconds).
        // Every timecode of a block (cluster timecode + block timecode ) is multiplied by this value to obtain real timecode of a block
        @JvmField
        val TimecodeScale = MKVType("TimecodeScale", byteArrayOf(0x2A, 0xD7.toByte(), 0xB1.toByte()), EbmlUint::class.java)

        @JvmField
        val Duration = MKVType("Duration", byteArrayOf(0x44, 0x89.toByte()), EbmlFloat::class.java)

        @JvmField
        val DateUTC = MKVType("DateUTC", byteArrayOf(0x44, 0x61.toByte()), EbmlDate::class.java)
        val Title = MKVType("Title", byteArrayOf(0x7B, 0xA9.toByte()), EbmlString::class.java)

        @JvmField
        val MuxingApp = MKVType("MuxingApp", byteArrayOf(0x4D, 0x80.toByte()), EbmlString::class.java)

        @JvmField
        val WritingApp = MKVType("WritingApp", byteArrayOf(0x57, 0x41), EbmlString::class.java)

        //The lower level EbmlBase containing the (monolithic) Block structure.
        @JvmField
        val Cluster = MKVType("Cluster", EbmlMaster.CLUSTER_ID, EbmlMaster::class.java)

        //Absolute timecode of the cluster (based on TimecodeScale).
        @JvmField
        val Timecode = MKVType("Timecode", byteArrayOf(0xE7.toByte()), EbmlUint::class.java)

        //  The list of tracks that are not used in that part of the stream. It is useful when using overlay tracks on seeking. Then you should decide what track to use.
        val SilentTracks = MKVType("SilentTracks", byteArrayOf(0x58, 0x54), EbmlMaster::class.java)

        //  One of the track number that are not used from now on in the stream. It could change later if not specified as silent in a further Cluster
        val SilentTrackNumber = MKVType("SilentTrackNumber", byteArrayOf(0x58, 0xD7.toByte()), EbmlUint::class.java)

        //  The Position of the Cluster in the segment (0 in live broadcast streams). It might help to resynchronise offset on damaged streams.
        @JvmField
        val Position = MKVType("Position", byteArrayOf(0xA7.toByte()), EbmlUint::class.java)

        // Size of the previous Cluster, in octets. Can be useful for backward playing.
        val PrevSize = MKVType("PrevSize", byteArrayOf(0xAB.toByte()), EbmlUint::class.java)

        //Similar to Block but without all the extra information, mostly used to reduced overhead when no extra feature is needed. (see SimpleBlock Structure)
        @JvmField
        val SimpleBlock = MKVType("SimpleBlock", MkvBlock.SIMPLEBLOCK_ID, MkvBlock::class.java)

        //Basic container of information containing a single Block or BlockVirtual, and information specific to that Block/VirtualBlock.
        @JvmField
        val BlockGroup = MKVType("BlockGroup", byteArrayOf(0xA0.toByte()), EbmlMaster::class.java)

        // Block containing the actual data to be rendered and a timecode relative to the Cluster Timecode. (see Block Structure)
        @JvmField
        val Block = MKVType("Block", MkvBlock.BLOCK_ID, MkvBlock::class.java)

        // Contain additional blocks to complete the main one. An EBML parser that has no knowledge of the Block structure could still see and use/skip these data.
        val BlockAdditions = MKVType("BlockAdditions", byteArrayOf(0x75, 0xA1.toByte()), EbmlMaster::class.java)

        //  Contain the BlockAdditional and some parameters.
        val BlockMore = MKVType("BlockMore", byteArrayOf(0xA6.toByte()), EbmlMaster::class.java)

        //  An ID to identify the BlockAdditional level.
        val BlockAddID = MKVType("BlockAddID", byteArrayOf(0xEE.toByte()), EbmlUint::class.java)

        // Interpreted by the codec as it wishes (using the BlockAddID).
        val BlockAdditional = MKVType("BlockAdditional", byteArrayOf(0xA5.toByte()), EbmlBin::class.java)

        /**
         * The duration of the Block (based on TimecodeScale).
         * This EbmlBase is mandatory when DefaultDuration is set for the track (but can be omitted as other default values).
         * When not written and with no DefaultDuration, the value is assumed to be the difference between the timecode
         * of this Block and the timecode of the next Block in "display" order (not coding order).
         * This EbmlBase can be useful at the end of a Track (as there is not other Block available);
         * or when there is a break in a track like for subtitle tracks.
         * When set to 0 that means the frame is not a keyframe.
         */
        @JvmField
        val BlockDuration = MKVType("BlockDuration", byteArrayOf(0x9B.toByte()), EbmlUint::class.java)

        // This frame is referenced and has the specified cache priority. In cache only a frame of the same or higher priority can replace this frame. A value of 0 means the frame is not referenced
        val ReferencePriority = MKVType("ReferencePriority", byteArrayOf(0xFA.toByte()), EbmlUint::class.java)

        //Timecode of another frame used as a reference (ie: B or P frame). The timecode is relative to the block it's attached to.
        @JvmField
        val ReferenceBlock = MKVType("ReferenceBlock", byteArrayOf(0xFB.toByte()), EbmlSint::class.java)

        //  The new codec state to use. Data interpretation is private to the codec. This information should always be referenced by a seek entry.
        val CodecState = MKVType("CodecState", byteArrayOf(0xA4.toByte()), EbmlBin::class.java)

        //  Contains slices description.
        val Slices = MKVType("Slices", byteArrayOf(0x8E.toByte()), EbmlMaster::class.java)

        //  Contains extra time information about the data contained in the Block. While there are a few files in the wild with this EbmlBase, it is no longer in use and has been deprecated. Being able to interpret this EbmlBase is not required for playback.
        val TimeSlice = MKVType("TimeSlice", byteArrayOf(0xE8.toByte()), EbmlMaster::class.java)

        //  The reverse number of the frame in the lace (0 is the last frame, 1 is the next to last, etc). While there are a few files in the wild with this EbmlBase, it is no longer in use and has been deprecated. Being able to interpret this EbmlBase is not required for playback.
        val LaceNumber = MKVType("LaceNumber", byteArrayOf(0xCC.toByte()), EbmlUint::class.java)

        //A top-level block of information with many tracks described.
        @JvmField
        val Tracks = MKVType("Tracks", byteArrayOf(0x16, 0x54.toByte(), 0xAE.toByte(), 0x6B.toByte()), EbmlMaster::class.java)

        //Describes a track with all EbmlBases.
        @JvmField
        val TrackEntry = MKVType("TrackEntry", byteArrayOf(0xAE.toByte()), EbmlMaster::class.java)

        //The track number as used in the Block Header (using more than 127 tracks is not encouraged, though the design allows an unlimited number).
        @JvmField
        val TrackNumber = MKVType("TrackNumber", byteArrayOf(0xD7.toByte()), EbmlUint::class.java)

        //A unique ID to identify the Track. This should be kept the same when making a direct stream copy of the Track to another file.
        @JvmField
        val TrackUID = MKVType("TrackUID", byteArrayOf(0x73, 0xC5.toByte()), EbmlUint::class.java)

        //A set of track types coded on 8 bits (1: video, 2: audio, 3: complex, 0x10: logo, 0x11: subtitle, 0x12: buttons, 0x20: control).
        @JvmField
        val TrackType = MKVType("TrackType", byteArrayOf(0x83.toByte()), EbmlUint::class.java)

        //  Set if the track is usable. (1 bit)
        @JvmField
        val FlagEnabled = MKVType("FlagEnabled", byteArrayOf(0xB9.toByte()), EbmlUint::class.java)

        //  Set if that track (audio, video or subs) SHOULD be active if no language found matches the user preference. (1 bit)
        @JvmField
        val FlagDefault = MKVType("FlagDefault", byteArrayOf(0x88.toByte()), EbmlUint::class.java)

        //   Set if that track MUST be active during playback. There can be many forced track for a kind (audio, video or subs); the player should select the one which language matches the user preference or the default + forced track. Overlay MAY happen between a forced and non-forced track of the same kind. (1 bit)
        @JvmField
        val FlagForced = MKVType("FlagForced", byteArrayOf(0x55, 0xAA.toByte()), EbmlUint::class.java)

        //   Set if the track may contain blocks using lacing. (1 bit)
        @JvmField
        val FlagLacing = MKVType("FlagLacing", byteArrayOf(0x9C.toByte()), EbmlUint::class.java)

        //  The minimum number of frames a player should be able to cache during playback. If set to 0, the reference pseudo-cache system is not used.
        @JvmField
        val MinCache = MKVType("MinCache", byteArrayOf(0x6D, 0xE7.toByte()), EbmlUint::class.java)

        //  The maximum cache size required to store referenced frames in and the current frame. 0 means no cache is needed.
        @JvmField
        val MaxCache = MKVType("MaxCache", byteArrayOf(0x6D, 0xF8.toByte()), EbmlUint::class.java)

        //Number of nanoseconds (not scaled via TimecodeScale) per frame ('frame' in the Matroska sense -- one EbmlBase put into a (Simple)Block).
        @JvmField
        val DefaultDuration = MKVType("DefaultDuration", byteArrayOf(0x23, 0xE3.toByte(), 0x83.toByte()), EbmlUint::class.java)

        // The maximum value of BlockAddID. A value 0 means there is no BlockAdditions for this track.
        val MaxBlockAdditionID = MKVType("MaxBlockAdditionID", byteArrayOf(0x55, 0xEE.toByte()), EbmlUint::class.java)

        //A human-readable track name.
        @JvmField
        val Name = MKVType("Name", byteArrayOf(0x53, 0x6E), EbmlString::class.java)

        // Specifies the language of the track in the Matroska languages form.
        @JvmField
        val Language = MKVType("Language", byteArrayOf(0x22, 0xB5.toByte(), 0x9C.toByte()), EbmlString::class.java)

        // An ID corresponding to the codec, see the codec page for more info.
        @JvmField
        val CodecID = MKVType("CodecID", byteArrayOf(0x86.toByte()), EbmlString::class.java)

        //Private data only known to the codec.
        @JvmField
        val CodecPrivate = MKVType("CodecPrivate", byteArrayOf(0x63.toByte(), 0xA2.toByte()), EbmlBin::class.java)

        //  A human-readable string specifying the codec.
        @JvmField
        val CodecName = MKVType("CodecName", byteArrayOf(0x25.toByte(), 0x86.toByte(), 0x88.toByte()), EbmlString::class.java)

        //  The UID of an attachment that is used by this codec.
        @JvmField
        val AttachmentLink = MKVType("AttachmentLink", byteArrayOf(0x74, 0x46), EbmlUint::class.java)

        // The codec can decode potentially damaged data (1 bit).
        @JvmField
        val CodecDecodeAll = MKVType("CodecDecodeAll", byteArrayOf(0xAA.toByte()), EbmlUint::class.java)

        //  Specify that this track is an overlay track for the Track specified (in the u-integer). That means when this track has a gap (see SilentTracks) the overlay track should be used instead. The order of multiple TrackOverlay matters, the first one is the one that should be used. If not found it should be the second, etc.
        @JvmField
        val TrackOverlay = MKVType("TrackOverlay", byteArrayOf(0x6F, 0xAB.toByte()), EbmlUint::class.java)

        // The track identification for the given Chapter Codec.
        val TrackTranslate = MKVType("TrackTranslate", byteArrayOf(0x66, 0x24), EbmlMaster::class.java)

        //  Specify an edition UID on which this translation applies. When not specified, it means for all editions found in the segment.
        val TrackTranslateEditionUID = MKVType("TrackTranslateEditionUID", byteArrayOf(0x66, 0xFC.toByte()), EbmlUint::class.java)

        //  The chapter codec using this ID (0: Matroska Script, 1: DVD-menu).
        val TrackTranslateCodec = MKVType("TrackTranslateCodec", byteArrayOf(0x66, 0xBF.toByte()), EbmlUint::class.java)

        // The binary value used to represent this track in the chapter codec data. The format depends on the ChapProcessCodecID used.
        val TrackTranslateTrackID = MKVType("TrackTranslateTrackID", byteArrayOf(0x66, 0xA5.toByte()), EbmlBin::class.java)

        @JvmField
        val Video = MKVType("Video", byteArrayOf(0xE0.toByte()), EbmlMaster::class.java)

        // Set if the video is interlaced. (1 bit)
        @JvmField
        val FlagInterlaced = MKVType("FlagInterlaced", byteArrayOf(0x9A.toByte()), EbmlUint::class.java)

        // Stereo-3D video mode (0: mono, 1: side by side (left eye is first); 2: top-bottom (right eye is first); 3: top-bottom (left eye is first); 4: checkboard (right is first); 5: checkboard (left is first); 6: row interleaved (right is first); 7: row interleaved (left is first); 8: column interleaved (right is first); 9: column interleaved (left is first); 10: anaglyph (cyan/red); 11: side by side (right eye is first); 12: anaglyph (green/magenta); 13 both eyes laced in one Block (left eye is first); 14 both eyes laced in one Block (right eye is first)) . There are some more details on 3D support in the Specification Notes.
        @JvmField
        val StereoMode = MKVType("StereoMode", byteArrayOf(0x53, 0xB8.toByte()), EbmlUint::class.java)

        // Alpha Video Mode. Presence of this EbmlBase indicates that the BlockAdditional EbmlBase could contain Alpha data.
        @JvmField
        val AlphaMode = MKVType("AlphaMode", byteArrayOf(0x53, 0xC0.toByte()), EbmlUint::class.java)

        @JvmField
        val PixelWidth = MKVType("PixelWidth", byteArrayOf(0xB0.toByte()), EbmlUint::class.java)

        @JvmField
        val PixelHeight = MKVType("PixelHeight", byteArrayOf(0xBA.toByte()), EbmlUint::class.java)

        //  The number of video pixels to remove at the bottom of the image (for HDTV content).
        @JvmField
        val PixelCropBottom = MKVType("PixelCropBottom", byteArrayOf(0x54, 0xAA.toByte()), EbmlUint::class.java)

        // The number of video pixels to remove at the top of the image.
        @JvmField
        val PixelCropTop = MKVType("PixelCropTop", byteArrayOf(0x54, 0xBB.toByte()), EbmlUint::class.java)

        // The number of video pixels to remove on the left of the image.
        @JvmField
        val PixelCropLeft = MKVType("PixelCropLeft", byteArrayOf(0x54, 0xCC.toByte()), EbmlUint::class.java)

        //  The number of video pixels to remove on the right of the image.
        @JvmField
        val PixelCropRight = MKVType("PixelCropRight", byteArrayOf(0x54, 0xDD.toByte()), EbmlUint::class.java)

        @JvmField
        val DisplayWidth = MKVType("DisplayWidth", byteArrayOf(0x54, 0xB0.toByte()), EbmlUint::class.java)

        @JvmField
        val DisplayHeight = MKVType("DisplayHeight", byteArrayOf(0x54, 0xBA.toByte()), EbmlUint::class.java)

        //  How DisplayWidth & DisplayHeight should be interpreted (0: pixels, 1: centimeters, 2: inches, 3: Display Aspect Ratio).
        @JvmField
        val DisplayUnit = MKVType("DisplayUnit", byteArrayOf(0x54, 0xB2.toByte()), EbmlUint::class.java)

        //  Specify the possible modifications to the aspect ratio (0: free resizing, 1: keep aspect ratio, 2: fixed).
        @JvmField
        val AspectRatioType = MKVType("AspectRatioType", byteArrayOf(0x54, 0xB3.toByte()), EbmlUint::class.java)

        //  Same value as in AVI (32 bits).
        val ColourSpace = MKVType("ColourSpace", byteArrayOf(0x2E, 0xB5.toByte(), 0x24), EbmlBin::class.java)

        @JvmField
        val Audio = MKVType("Audio", byteArrayOf(0xE1.toByte()), EbmlMaster::class.java)

        @JvmField
        val SamplingFrequency = MKVType("SamplingFrequency", byteArrayOf(0xB5.toByte()), EbmlFloat::class.java)

        @JvmField
        val OutputSamplingFrequency = MKVType("OutputSamplingFrequency", byteArrayOf(0x78, 0xB5.toByte()), EbmlFloat::class.java)

        @JvmField
        val Channels = MKVType("Channels", byteArrayOf(0x9F.toByte()), EbmlUint::class.java)

        @JvmField
        val BitDepth = MKVType("BitDepth", byteArrayOf(0x62, 0x64), EbmlUint::class.java)

        //  Operation that needs to be applied on tracks to create this virtual track. For more details look at the Specification Notes on the subject.
        val TrackOperation = MKVType("TrackOperation", byteArrayOf(0xE2.toByte()), EbmlMaster::class.java)

        // Contains the list of all video plane tracks that need to be combined to create this 3D track
        val TrackCombinePlanes = MKVType("TrackCombinePlanes", byteArrayOf(0xE3.toByte()), EbmlMaster::class.java)

        //  Contains a video plane track that need to be combined to create this 3D track
        val TrackPlane = MKVType("TrackPlane", byteArrayOf(0xE4.toByte()), EbmlMaster::class.java)

        //  The trackUID number of the track representing the plane.
        val TrackPlaneUID = MKVType("TrackPlaneUID", byteArrayOf(0xE5.toByte()), EbmlUint::class.java)

        //  The kind of plane this track corresponds to (0: left eye, 1: right eye, 2: background).
        val TrackPlaneType = MKVType("TrackPlaneType", byteArrayOf(0xE6.toByte()), EbmlUint::class.java)

        // Contains the list of all tracks whose Blocks need to be combined to create this virtual track
        val TrackJoinBlocks = MKVType("TrackJoinBlocks", byteArrayOf(0xE9.toByte()), EbmlMaster::class.java)

        // The trackUID number of a track whose blocks are used to create this virtual track.
        val TrackJoinUID = MKVType("TrackJoinUID", byteArrayOf(0xED.toByte()), EbmlUint::class.java)

        // Settings for several content encoding mechanisms like compression or encryption.
        val ContentEncodings = MKVType("ContentEncodings", byteArrayOf(0x6D, 0x80.toByte()), EbmlMaster::class.java)

        // Settings for one content encoding like compression or encryption.
        val ContentEncoding = MKVType("ContentEncoding", byteArrayOf(0x62, 0x40), EbmlMaster::class.java)

        // Tells when this modification was used during encoding/muxing starting with 0 and counting upwards. The decoder/demuxer has to start with the highest order number it finds and work its way down. This value has to be unique over all ContentEncodingOrder EbmlBases in the segment.
        val ContentEncodingOrder = MKVType("ContentEncodingOrder", byteArrayOf(0x50, 0x31), EbmlUint::class.java)

        //  A bit field that describes which EbmlBases have been modified in this way. Values (big endian) can be OR'ed. Possible values: 1 - all frame contents, 2 - the track's private data, 4 - the next ContentEncoding (next ContentEncodingOrder. Either the data inside ContentCompression and/or ContentEncryption)
        val ContentEncodingScope = MKVType("ContentEncodingScope", byteArrayOf(0x50, 0x32), EbmlUint::class.java)

        //   A value describing what kind of transformation has been done. Possible values: 0 - compression, 1 - encryption
        val ContentEncodingType = MKVType("ContentEncodingType", byteArrayOf(0x50, 0x33), EbmlUint::class.java)

        // Settings describing the compression used. Must be present if the value of ContentEncodingType is 0 and absent otherwise. Each block must be decompressable even if no previous block is available in order not to prevent seeking.
        val ContentCompression = MKVType("ContentCompression", byteArrayOf(0x50, 0x34), EbmlMaster::class.java)

        //  The compression algorithm used. Algorithms that have been specified so far are: 0 - zlib, 1 - bzlib, 2 - lzo1x, 3 - Header Stripping
        val ContentCompAlgo = MKVType("ContentCompAlgo", byteArrayOf(0x42, 0x54.toByte()), EbmlUint::class.java)

        //  Settings that might be needed by the decompressor. For Header Stripping (ContentCompAlgo=3); the bytes that were removed from the beggining of each frames of the track.
        val ContentCompSettings = MKVType("ContentCompSettings", byteArrayOf(0x42, 0x55), EbmlBin::class.java)

        //  Settings describing the encryption used. Must be present if the value of ContentEncodingType is 1 and absent otherwise.
        val ContentEncryption = MKVType("ContentEncryption", byteArrayOf(0x50, 0x35), EbmlMaster::class.java)

        //  The encryption algorithm used. The value '0' means that the contents have not been encrypted but only signed. Predefined values: 1 - DES, 2 - 3DES, 3 - Twofish, 4 - Blowfish, 5 - AES
        val ContentEncAlgo = MKVType("ContentEncAlgo", byteArrayOf(0x47, 0xE1.toByte()), EbmlUint::class.java)

        // For public key algorithms this is the ID of the public key the the data was encrypted with.
        val ContentEncKeyID = MKVType("ContentEncKeyID", byteArrayOf(0x47, 0xE2.toByte()), EbmlBin::class.java)

        //  A cryptographic signature of the contents.
        val ContentSignature = MKVType("ContentSignature", byteArrayOf(0x47, 0xE3.toByte()), EbmlBin::class.java)

        //  This is the ID of the private key the data was signed with.
        val ContentSigKeyID = MKVType("ContentSigKeyID", byteArrayOf(0x47, 0xE4.toByte()), EbmlBin::class.java)

        // The algorithm used for the signature. A value of '0' means that the contents have not been signed but only encrypted. Predefined values: 1 - RSA
        val ContentSigAlgo = MKVType("ContentSigAlgo", byteArrayOf(0x47, 0xE5.toByte()), EbmlUint::class.java)

        // The hash algorithm used for the signature. A value of '0' means that the contents have not been signed but only encrypted. Predefined values: 1 - SHA1-160, 2 - MD5
        val ContentSigHashAlgo = MKVType("ContentSigHashAlgo", byteArrayOf(0x47, 0xE6.toByte()), EbmlUint::class.java)

        @JvmField
        val Cues = MKVType("Cues", byteArrayOf(0x1C, 0x53, 0xBB.toByte(), 0x6B), EbmlMaster::class.java)

        @JvmField
        val CuePoint = MKVType("CuePoint", byteArrayOf(0xBB.toByte()), EbmlMaster::class.java)

        @JvmField
        val CueTime = MKVType("CueTime", byteArrayOf(0xB3.toByte()), EbmlUint::class.java)

        @JvmField
        val CueTrackPositions = MKVType("CueTrackPositions", byteArrayOf(0xB7.toByte()), EbmlMaster::class.java)

        @JvmField
        val CueTrack = MKVType("CueTrack", byteArrayOf(0xF7.toByte()), EbmlUint::class.java)

        @JvmField
        val CueClusterPosition = MKVType("CueClusterPosition", byteArrayOf(0xF1.toByte()), EbmlUint::class.java)

        // The relative position of the referenced block inside the cluster with 0 being the first possible position for an EbmlBase inside that cluster.
        val CueRelativePosition = MKVType("CueRelativePosition", byteArrayOf(0xF0.toByte()), EbmlUint::class.java)

        // The duration of the block according to the segment time base. If missing the track's DefaultDuration does not apply and no duration information is available in terms of the cues.
        val CueDuration = MKVType("CueDuration", byteArrayOf(0xB2.toByte()), EbmlUint::class.java)

        //Number of the Block in the specified Cluster.
        @JvmField
        val CueBlockNumber = MKVType("CueBlockNumber", byteArrayOf(0x53, 0x78), EbmlUint::class.java)

        // The position of the Codec State corresponding to this Cue EbmlBase. 0 means that the data is taken from the initial Track Entry.
        val CueCodecState = MKVType("CueCodecState", byteArrayOf(0xEA.toByte()), EbmlUint::class.java)

        // The Clusters containing the required referenced Blocks.
        val CueReference = MKVType("CueReference", byteArrayOf(0xDB.toByte()), EbmlMaster::class.java)

        // Timecode of the referenced Block.
        val CueRefTime = MKVType("CueRefTime", byteArrayOf(0x96.toByte()), EbmlUint::class.java)
        val Attachments = MKVType("Attachments", byteArrayOf(0x19, 0x41, 0xA4.toByte(), 0x69), EbmlMaster::class.java)
        val AttachedFile = MKVType("AttachedFile", byteArrayOf(0x61, 0xA7.toByte()), EbmlMaster::class.java)
        val FileDescription = MKVType("FileDescription", byteArrayOf(0x46, 0x7E.toByte()), EbmlString::class.java)
        val FileName = MKVType("FileName", byteArrayOf(0x46, 0x6E.toByte()), EbmlString::class.java)
        val FileMimeType = MKVType("FileMimeType", byteArrayOf(0x46, 0x60.toByte()), EbmlString::class.java)
        val FileData = MKVType("FileData", byteArrayOf(0x46, 0x5C.toByte()), EbmlBin::class.java)
        val FileUID = MKVType("FileUID", byteArrayOf(0x46, 0xAE.toByte()), EbmlUint::class.java)
        val Chapters = MKVType("Chapters", byteArrayOf(0x10, 0x43.toByte(), 0xA7.toByte(), 0x70.toByte()), EbmlMaster::class.java)
        val EditionEntry = MKVType("EditionEntry", byteArrayOf(0x45.toByte(), 0xB9.toByte()), EbmlMaster::class.java)
        val EditionUID = MKVType("EditionUID", byteArrayOf(0x45.toByte(), 0xBC.toByte()), EbmlUint::class.java)
        val EditionFlagHidden = MKVType("EditionFlagHidden", byteArrayOf(0x45.toByte(), 0xBD.toByte()), EbmlUint::class.java)
        val EditionFlagDefault = MKVType("EditionFlagDefault", byteArrayOf(0x45.toByte(), 0xDB.toByte()), EbmlUint::class.java)
        val EditionFlagOrdered = MKVType("EditionFlagOrdered", byteArrayOf(0x45.toByte(), 0xDD.toByte()), EbmlUint::class.java)
        val ChapterAtom = MKVType("ChapterAtom", byteArrayOf(0xB6.toByte()), EbmlMaster::class.java)
        val ChapterUID = MKVType("ChapterUID", byteArrayOf(0x73.toByte(), 0xC4.toByte()), EbmlUint::class.java)

        //  A unique string ID to identify the Chapter. Use for WebVTT cue identifier storage.
        val ChapterStringUID = MKVType("ChapterStringUID", byteArrayOf(0x56, 0x54), EbmlString::class.java)
        val ChapterTimeStart = MKVType("ChapterTimeStart", byteArrayOf(0x91.toByte()), EbmlUint::class.java)
        val ChapterTimeEnd = MKVType("ChapterTimeEnd", byteArrayOf(0x92.toByte()), EbmlUint::class.java)
        val ChapterFlagHidden = MKVType("ChapterFlagHidden", byteArrayOf(0x98.toByte()), EbmlUint::class.java)
        val ChapterFlagEnabled = MKVType("ChapterFlagEnabled", byteArrayOf(0x45.toByte(), 0x98.toByte()), EbmlUint::class.java)

        //  A segment to play in place of this chapter. Edition ChapterSegmentEditionUID should be used for this segment, otherwise no edition is used.
        val ChapterSegmentUID = MKVType("ChapterSegmentUID", byteArrayOf(0x6E, 0x67), EbmlBin::class.java)

        //  The EditionUID to play from the segment linked in ChapterSegmentUID.
        val ChapterSegmentEditionUID = MKVType("ChapterSegmentEditionUID", byteArrayOf(0x6E, 0xBC.toByte()), EbmlUint::class.java)
        val ChapterPhysicalEquiv = MKVType("ChapterPhysicalEquiv", byteArrayOf(0x63.toByte(), 0xC3.toByte()), EbmlUint::class.java)
        val ChapterTrack = MKVType("ChapterTrack", byteArrayOf(0x8F.toByte()), EbmlMaster::class.java)
        val ChapterTrackNumber = MKVType("ChapterTrackNumber", byteArrayOf(0x89.toByte()), EbmlUint::class.java)
        val ChapterDisplay = MKVType("ChapterDisplay", byteArrayOf(0x80.toByte()), EbmlMaster::class.java)
        val ChapString = MKVType("ChapString", byteArrayOf(0x85.toByte()), EbmlString::class.java)
        val ChapLanguage = MKVType("ChapLanguage", byteArrayOf(0x43.toByte(), 0x7C.toByte()), EbmlString::class.java)
        val ChapCountry = MKVType("ChapCountry", byteArrayOf(0x43.toByte(), 0x7E.toByte()), EbmlString::class.java)

        // Contains all the commands associated to the Atom.
        val ChapProcess = MKVType("ChapProcess", byteArrayOf(0x69, 0x44), EbmlMaster::class.java)

        // Contains the type of the codec used for the processing. A value of 0 means native Matroska processing (to be defined); a value of 1 means the DVD command set is used. More codec IDs can be added later.
        val ChapProcessCodecID = MKVType("ChapProcessCodecID", byteArrayOf(0x69, 0x55), EbmlUint::class.java)

        // Some optional data attached to the ChapProcessCodecID information. For ChapProcessCodecID = 1, it is the "DVD level" equivalent.
        val ChapProcessPrivate = MKVType("ChapProcessPrivate", byteArrayOf(0x45, 0x0D), EbmlBin::class.java)

        // Contains all the commands associated to the Atom.
        val ChapProcessCommand = MKVType("ChapProcessCommand", byteArrayOf(0x69, 0x11), EbmlMaster::class.java)

        // Defines when the process command should be handled (0: during the whole chapter, 1: before starting playback, 2: after playback of the chapter).
        val ChapProcessTime = MKVType("ChapProcessTime", byteArrayOf(0x69, 0x22), EbmlUint::class.java)

        // Contains the command information. The data should be interpreted depending on the ChapProcessCodecID value. For ChapProcessCodecID = 1, the data correspond to the binary DVD cell pre/post commands.
        val ChapProcessData = MKVType("ChapProcessData", byteArrayOf(0x69, 0x33), EbmlBin::class.java)

        @JvmField
        val Tags = MKVType("Tags", byteArrayOf(0x12, 0x54.toByte(), 0xC3.toByte(), 0x67.toByte()), EbmlMaster::class.java)
        val Tag = MKVType("Tag", byteArrayOf(0x73, 0x73.toByte()), EbmlMaster::class.java)
        val Targets = MKVType("Targets", byteArrayOf(0x63, 0xC0.toByte()), EbmlMaster::class.java)
        val TargetTypeValue = MKVType("TargetTypeValue", byteArrayOf(0x68, 0xCA.toByte()), EbmlUint::class.java)
        val TargetType = MKVType("TargetType", byteArrayOf(0x63, 0xCA.toByte()), EbmlString::class.java)
        val TagTrackUID = MKVType("TagTrackUID", byteArrayOf(0x63, 0xC5.toByte()), EbmlUint::class.java)

        //  A unique ID to identify the EditionEntry(s) the tags belong to. If the value is 0 at this level, the tags apply to all editions in the Segment.
        val TagEditionUID = MKVType("TagEditionUID", byteArrayOf(0x63, 0xC9.toByte()), EbmlUint::class.java)
        val TagChapterUID = MKVType("TagChapterUID", byteArrayOf(0x63, 0xC4.toByte()), EbmlUint::class.java)
        val TagAttachmentUID = MKVType("TagAttachmentUID", byteArrayOf(0x63, 0xC6.toByte()), EbmlUint::class.java)
        val SimpleTag = MKVType("SimpleTag", byteArrayOf(0x67, 0xC8.toByte()), EbmlMaster::class.java)
        val TagName = MKVType("TagName", byteArrayOf(0x45, 0xA3.toByte()), EbmlString::class.java)
        val TagLanguage = MKVType("TagLanguage", byteArrayOf(0x44, 0x7A), EbmlString::class.java)
        val TagDefault = MKVType("TagDefault", byteArrayOf(0x44, 0x84.toByte()), EbmlUint::class.java)
        val TagString = MKVType("TagString", byteArrayOf(0x44, 0x87.toByte()), EbmlString::class.java)
        val TagBinary = MKVType("TagBinary", byteArrayOf(0x44, 0x85.toByte()), EbmlBin::class.java)
        var firstLevelHeaders = arrayOf(SeekHead, Info, Cluster, Tracks, Cues, Attachments, Chapters, Tags, EBMLVersion, EBMLReadVersion, EBMLMaxIDLength, EBMLMaxSizeLength, DocType, DocTypeVersion, DocTypeReadVersion)
        fun values(): Array<MKVType> {
            return _values.toTypedArray()
        }

        @JvmStatic
        fun <T : EbmlBase?> createByType(g: MKVType): T {
            return try {
                val elem = Platform.newInstance(g.clazz, arrayOf<Any>(g.id)) as T
                elem!!.type = g
                elem
            } catch (e: Exception) {
                e.printStackTrace()
                EbmlBin(g.id) as T
            }
        }

        @JvmStatic
        fun <T : EbmlBase?> createById(id: ByteArray?, offset: Long): T {
            val values = values()
            for (i in values.indices) {
                val t = values[i]
                if (Platform.arrayEqualsByte(t.id, id)) {
                    val createByType = createByType<T>(t)
                    return createByType
                }
            }
            System.err.println("WARNING: unspecified ebml ID (" + toHexString(id!!) + ") encountered at position 0x"
                    + java.lang.Long.toHexString(offset).toUpperCase())
            val t = EbmlVoid(id) as T
            t!!.type = Void
            return t
        }

        fun isHeaderFirstByte(b: Byte): Boolean {
            val values = values()
            for (i in values.indices) {
                val t = values[i]
                if (t.id[0] == b) return true
            }
            return false
        }

        fun isSpecifiedHeader(b: ByteArray?): Boolean {
            val values = values()
            for (i in values.indices) {
                val firstLevelHeader = values[i]
                if (Platform.arrayEqualsByte(firstLevelHeader.id, b)) return true
            }
            return false
        }

        fun isFirstLevelHeader(b: ByteArray?): Boolean {
            for (firstLevelHeader in firstLevelHeaders) if (Platform.arrayEqualsByte(firstLevelHeader.id, b)) return true
            return false
        }

        val children: MutableMap<MKVType, Set<MKVType>> = HashMap()

        // TODO: this may be optimized of-course if it turns out to be a frequently called method
        fun getParent(t: MKVType): MKVType? {
            for ((key, value) in children) {
                if (value.contains(t)) return key
            }
            return null
        }

        fun possibleChild(parent: EbmlMaster?, child: EbmlBase): Boolean {
            if (parent == null) return if (child.type == EBML || child.type == Segment) true else false

            // Any unknown EbmlBase is assigned type of Void by parses, thus two different checks

            // 1. since Void/CRC32 can occur anywhere in the tree,
            //     look if they violate size-offset  contract of the parent.
            //     Violated size-offset contract implies the global EbmlBase actually belongs to parent
            if (Platform.arrayEqualsByte(child.id, Void.id) || Platform.arrayEqualsByte(child.id, CRC32.id)) return child.offset != parent.dataOffset + parent.dataLen

            // 2. In case Void/CRC32 type is assigned, child EbmlBase is assumed as global,
            //    thus it can appear anywhere in the tree
            if (child.type == Void || child.type == CRC32) return true
            val candidates = children[parent.type]
            return candidates != null && candidates.contains(child.type)
        }

        fun possibleChildById(parent: EbmlMaster?, typeId: ByteArray?): Boolean {
            // Only EBML or Segment are allowed at top level
            if (parent == null && (Platform.arrayEqualsByte(EBML.id, typeId) || Platform.arrayEqualsByte(Segment.id, typeId))) return true

            // Other EbmlBases at top level are not allowed
            if (parent == null) return false

            // Void and CRC32 EbmlBases are global and are allowed everywhere in the hierarchy
            if (Platform.arrayEqualsByte(Void.id, typeId) || Platform.arrayEqualsByte(CRC32.id, typeId)) return true

            // for any other EbmlBase we have to check the spec
            for (aCandidate in children[parent.type]!!) if (Platform.arrayEqualsByte(aCandidate.id, typeId)) return true
            return false
        }

        @JvmStatic
        fun findFirst(master: EbmlBase, path: Array<MKVType>): EbmlBase? {
            val tlist: List<MKVType> = LinkedList(Arrays.asList(*path))
            return findFirstSub(master, tlist)
        }

        @JvmStatic
        fun <T> findFirstTree(tree: List<EbmlBase>, path: Array<MKVType>): T? {
            val tlist: List<MKVType> = LinkedList(Arrays.asList(*path))
            for (e in tree) {
                val z = findFirstSub(e, tlist)
                if (z != null) return z as T
            }
            return null
        }

        private fun findFirstSub(elem: EbmlBase, path: List<MKVType>): EbmlBase? {
            val path = path.toMutableList()
            if (path.size == 0) return null
            if (elem.type != path[0]) return null
            if (path.size == 1) return elem
            val head: MKVType = path.removeAt(0)
            var result: EbmlBase? = null
            if (elem is EbmlMaster) {
                val iter: Iterator<EbmlBase> = elem.children.iterator()
                while (iter.hasNext() && result == null) result = findFirstSub(iter.next(), path)
            }
            path.add(0, head)
            return result
        }

        @JvmStatic
        fun <T> findList(tree: List<EbmlBase>, class1: Class<T>?, path: Array<MKVType>): List<T> {
            val result: MutableList<T> = LinkedList()
            val tlist = LinkedList(listOf(*path))
            if (tlist.size > 0) for (node in tree) {
                val head: MKVType = tlist.removeAt(0)
                if (head == null || head == node.type) {
                    findSubList(node, tlist, result)
                }
                tlist.add(0, head)
            }
            return result
        }

        private fun <T> findSubList(element: EbmlBase, path: List<MKVType>, result: MutableCollection<T>) {
            val path = path.toMutableList()
            if (path.size > 0) {
                val head: MKVType = path.removeAt(0)
                if (element is EbmlMaster) {
                    for (candidate in element.children) {
                        if (head == null || head == candidate.type) {
                            findSubList(candidate, path, result)
                        }
                    }
                }
                path.add(0, head)
            } else {
                result.add(element as T)
            }
        }

        @JvmStatic
        fun findAllTree(tree: List<EbmlBase>, path: Array<MKVType>): List<EbmlBase> {
            val result: MutableList<EbmlBase> = LinkedList()
            val tlist = LinkedList(Arrays.asList(*path))
            if (tlist.size > 0) for (node in tree) {
                val head: MKVType = tlist.removeAt(0)
                if (head == null || head == node.type) {
                    findSub(node, tlist, result)
                }
                tlist.add(0, head)
            }
            return result
        }

        @JvmStatic
        fun findAll(master: EbmlBase, path: Array<MKVType>): List<EbmlBase> {
            val result: MutableList<EbmlBase> = LinkedList()
            val tlist = LinkedList(Arrays.asList(*path))
            if (master.type != tlist[0]) return result
            tlist.removeAt(0)
            findSub(master, tlist, result)
            return result
        }

        private fun findSub(master: EbmlBase, path: List<MKVType>, result: MutableCollection<EbmlBase>) {
            if (path.size > 0) {
                val path = path.toMutableList()
                val head: MKVType = path.removeAt(0)
                if (master is EbmlMaster) {
                    for (candidate in master.children) {
                        if (head == null || head == candidate.type) {
                            findSub(candidate, path, result)
                        }
                    }
                }
                path.add(0, head)
            } else {
                result.add(master)
            }
        }

        init {
            children[EBML] = HashSet(Arrays.asList(*arrayOf(EBMLVersion, EBMLReadVersion, EBMLMaxIDLength, EBMLMaxSizeLength, DocType, DocTypeVersion, DocTypeReadVersion)))
            children[Segment] = HashSet(Arrays.asList(*arrayOf(SeekHead, Info, Cluster, Tracks, Cues, Attachments, Chapters, Tags)))
            children[SeekHead] = HashSet(Arrays.asList(*arrayOf(Seek)))
            children[Seek] = HashSet(Arrays.asList(*arrayOf(SeekID, SeekPosition)))
            children[Info] = HashSet(Arrays.asList(*arrayOf(SegmentUID, SegmentFilename, PrevUID, PrevFilename, NextUID, NextFilenam, SegmentFamily, ChapterTranslate, TimecodeScale, Duration, DateUTC, Title, MuxingApp, WritingApp)))
            children[ChapterTranslate] = HashSet(Arrays.asList(*arrayOf(ChapterTranslateEditionUID, ChapterTranslateCodec, ChapterTranslateID)))
            children[Cluster] = HashSet(Arrays.asList(*arrayOf(Timecode, SilentTracks, Position, PrevSize, SimpleBlock, BlockGroup)))
            children[SilentTracks] = HashSet(Arrays.asList(*arrayOf(SilentTrackNumber)))
            children[BlockGroup] = HashSet(Arrays.asList(*arrayOf(Block, BlockAdditions, BlockDuration, ReferencePriority, ReferenceBlock, CodecState, Slices)))
            children[BlockAdditions] = HashSet(Arrays.asList(*arrayOf(BlockMore)))
            children[BlockMore] = HashSet(Arrays.asList(*arrayOf(BlockAddID, BlockAdditional)))
            children[Slices] = HashSet(Arrays.asList(*arrayOf(TimeSlice)))
            children[TimeSlice] = HashSet(Arrays.asList(*arrayOf(LaceNumber)))
            children[Tracks] = HashSet(Arrays.asList(*arrayOf(TrackEntry)))
            children[TrackEntry] = HashSet(Arrays.asList(*arrayOf(TrackNumber, TrackUID, TrackType, TrackType, FlagDefault, FlagForced, FlagLacing, MinCache, MaxCache, DefaultDuration, MaxBlockAdditionID, Name, Language, CodecID, CodecPrivate, CodecName, AttachmentLink, CodecDecodeAll, TrackOverlay, TrackTranslate, Video, Audio, TrackOperation, ContentEncodings)))
            children[TrackTranslate] = HashSet(Arrays.asList(*arrayOf(TrackTranslateEditionUID, TrackTranslateCodec, TrackTranslateTrackID)))
            children[Video] = HashSet(Arrays.asList(*arrayOf(FlagInterlaced, StereoMode, AlphaMode, PixelWidth, PixelHeight, PixelCropBottom, PixelCropTop, PixelCropLeft, PixelCropRight, DisplayWidth, DisplayHeight, DisplayUnit, AspectRatioType, ColourSpace)))
            children[Audio] = HashSet(Arrays.asList(*arrayOf(SamplingFrequency, OutputSamplingFrequency, Channels, BitDepth)))
            children[TrackOperation] = HashSet(Arrays.asList(*arrayOf(TrackCombinePlanes, TrackJoinBlocks)))
            children[TrackCombinePlanes] = HashSet(Arrays.asList(*arrayOf(TrackPlane)))
            children[TrackPlane] = HashSet(Arrays.asList(*arrayOf(TrackPlaneUID, TrackPlaneType)))
            children[TrackJoinBlocks] = HashSet(Arrays.asList(*arrayOf(TrackJoinUID)))
            children[ContentEncodings] = HashSet(Arrays.asList(*arrayOf(ContentEncoding)))
            children[ContentEncoding] = HashSet(Arrays.asList(*arrayOf(ContentEncodingOrder, ContentEncodingScope, ContentEncodingType, ContentCompression, ContentEncryption)))
            children[ContentCompression] = HashSet(Arrays.asList(*arrayOf(ContentCompAlgo, ContentCompSettings)))
            children[ContentEncryption] = HashSet(Arrays.asList(*arrayOf(ContentEncAlgo, ContentEncKeyID, ContentSignature, ContentSigKeyID, ContentSigAlgo, ContentSigHashAlgo)))
            children[Cues] = HashSet(Arrays.asList(*arrayOf(CuePoint)))
            children[CuePoint] = HashSet(Arrays.asList(*arrayOf(CueTime, CueTrackPositions)))
            children[CueTrackPositions] = HashSet(Arrays.asList(*arrayOf(CueTrack, CueClusterPosition, CueRelativePosition, CueDuration, CueBlockNumber, CueCodecState, CueReference)))
            children[CueReference] = HashSet(Arrays.asList(*arrayOf(CueRefTime)))
            children[Attachments] = HashSet(Arrays.asList(*arrayOf(AttachedFile)))
            children[AttachedFile] = HashSet(Arrays.asList(*arrayOf(FileDescription, FileName, FileMimeType, FileData, FileUID)))
            children[Chapters] = HashSet(Arrays.asList(*arrayOf(EditionEntry)))
            children[EditionEntry] = HashSet(Arrays.asList(*arrayOf(EditionUID, EditionFlagHidden, EditionFlagDefault, EditionFlagOrdered, ChapterAtom)))
            children[ChapterAtom] = HashSet(Arrays.asList(*arrayOf(ChapterUID, ChapterStringUID, ChapterTimeStart, ChapterTimeEnd, ChapterFlagHidden, ChapterFlagEnabled, ChapterSegmentUID, ChapterSegmentEditionUID, ChapterPhysicalEquiv, ChapterTrack, ChapterDisplay, ChapProcess)))
            children[ChapterTrack] = HashSet(Arrays.asList(*arrayOf(ChapterTrackNumber)))
            children[ChapterDisplay] = HashSet(Arrays.asList(*arrayOf(ChapString, ChapLanguage, ChapCountry)))
            children[ChapProcess] = HashSet(Arrays.asList(*arrayOf(ChapProcessCodecID, ChapProcessPrivate, ChapProcessCommand)))
            children[ChapProcessCommand] = HashSet(Arrays.asList(*arrayOf(ChapProcessTime, ChapProcessData)))
            children[Tags] = HashSet(Arrays.asList(*arrayOf(Tag)))
            children[Tag] = HashSet(Arrays.asList(*arrayOf(Targets, SimpleTag)))
            children[Targets] = HashSet(Arrays.asList(*arrayOf(TargetTypeValue, TargetType, TagTrackUID, TagEditionUID, TagChapterUID, TagAttachmentUID)))
            children[SimpleTag] = HashSet(Arrays.asList(*arrayOf(TagName, TagLanguage, TagDefault, TagString, TagBinary)))
        }
    }

    init {
        _values.add(this)
    }
}