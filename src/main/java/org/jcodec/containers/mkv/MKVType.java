package org.jcodec.containers.mkv;
import static org.jcodec.containers.mkv.util.EbmlUtil.toHexString;

import java.util.Iterator;

import org.jcodec.containers.mkv.boxes.EbmlBase;
import org.jcodec.containers.mkv.boxes.EbmlBin;
import org.jcodec.containers.mkv.boxes.EbmlDate;
import org.jcodec.containers.mkv.boxes.EbmlFloat;
import org.jcodec.containers.mkv.boxes.EbmlMaster;
import org.jcodec.containers.mkv.boxes.EbmlSint;
import org.jcodec.containers.mkv.boxes.EbmlString;
import org.jcodec.containers.mkv.boxes.EbmlUint;
import org.jcodec.containers.mkv.boxes.EbmlVoid;
import org.jcodec.containers.mkv.boxes.MkvBlock;
import org.jcodec.containers.mkv.boxes.MkvSegment;
import org.jcodec.platform.Platform;

import java.lang.System;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed under FreeBSD License
 * 
 * EBML IO implementation
 * 
 * @author The JCodec project
 * 
 */
public final class MKVType {
    private final static List<MKVType> _values =new ArrayList<MKVType>();
    // EBML Id's
    public final static MKVType Void = new MKVType("Void", new byte[]{(byte)0xEC}, EbmlVoid.class);
    public final static MKVType CRC32 = new MKVType("CRC32", new byte[]{(byte)0xBF}, EbmlBin.class);
    public final static MKVType EBML = new MKVType("EBML", new byte[]{0x1A, 0x45, (byte)0xDF, (byte)0xA3}, EbmlMaster.class);
    public final static MKVType EBMLVersion = new MKVType("EBMLVersion", new byte[]{0x42, (byte)0x86}, EbmlUint.class);
    public final static MKVType EBMLReadVersion = new MKVType("EBMLReadVersion", new byte[]{0x42, (byte)0xF7}, EbmlUint.class);
    public final static MKVType EBMLMaxIDLength = new MKVType("EBMLMaxIDLength", new byte[]{0x42, (byte)0xF2}, EbmlUint.class);
    public final static MKVType EBMLMaxSizeLength = new MKVType("EBMLMaxSizeLength", new byte[]{0x42, (byte)0xF3}, EbmlUint.class);
    //All strings are UTF8 in java, this EbmlBase is specified as pure ASCII EbmlBase in Matroska spec
    public final static MKVType DocType = new MKVType("DocType", new byte[]{0x42, (byte)0x82}, EbmlString.class); 
    public final static MKVType DocTypeVersion = new MKVType("DocTypeVersion", new byte[]{0x42, (byte)0x87}, EbmlUint.class);
    public final static MKVType DocTypeReadVersion = new MKVType("DocTypeReadVersion", new byte[]{0x42, (byte)0x85}, EbmlUint.class);

    // ================================
    // [0] The Root Element that contains all other Top-Level Elements (Elements defined only at Level 1). A Matroska file is composed of 1 Segment.
    // ================================
    public final static MKVType Segment = new MKVType("Segment", MkvSegment.SEGMENT_ID, MkvSegment.class);

    // --------------------------------
    // [1] Contains the Segment Position of other Top-Level Elements.
    // --------------------------------
    public final static MKVType SeekHead = new MKVType("SeekHead", new byte[]{0x11, 0x4D, (byte)0x9B, 0x74}, EbmlMaster.class);
    //     [2] Contains a single seek entry to an EBML Element.
    public final static MKVType Seek = new MKVType("Seek", new byte[]{0x4D, (byte)0xBB}, EbmlMaster.class);
    //         [3] The binary ID corresponding to the Element name.
    public final static MKVType SeekID = new MKVType("SeekID", new byte[]{0x53, (byte)0xAB}, EbmlBin.class);
    //         [3] The Segment Position of the Element.
    public final static MKVType SeekPosition = new MKVType("SeekPosition", new byte[]{0x53, (byte)0xAC}, EbmlUint.class);

    // --------------------------------
    // [1] Contains general information about the Segment.
    // --------------------------------
    public final static MKVType Info = new MKVType("Info", new byte[]{0x15, (byte)0x49, (byte)0xA9, (byte)0x66}, EbmlMaster.class);
    //     [2] A randomly generated unique ID to identify the Segment amongst many others (128 bits). If the Segment is a part of a
    //         Linked Segment, then this Element is REQUIRED.
    public final static MKVType SegmentUID = new MKVType("SegmentUID", new byte[]{0x73, (byte)0xA4}, EbmlBin.class);
    //     [2] A filename corresponding to this Segment.
    public final static MKVType SegmentFilename = new MKVType("SegmentFilename", new byte[]{0x73, (byte)0x84}, EbmlString.class);
    //     [2] A unique ID to identify the previous Segment of a Linked Segment (128 bits). If the Segment is a part of a Linked Segment that
    //         uses Hard Linking, then either the PrevUID or the NextUID Element is REQUIRED. If a Segment contains a PrevUID but not a NextUID,
    //         then it MAY be considered as the last Segment of the Linked Segment. The PrevUID MUST NOT be equal to the SegmentUID.
    public final static MKVType PrevUID = new MKVType("PrevUID", new byte[]{0x3C, (byte)0xB9, 0x23}, EbmlBin.class);
    //     [2] A filename corresponding to the file of the previous Linked Segment. Provision of the previous filename is for display convenience,
    //         but PrevUID SHOULD be considered authoritative for identifying the previous Segment in a Linked Segment.
    public final static MKVType PrevFilename = new MKVType("PrevFilename", new byte[]{0x3C, (byte)0x83, (byte)0xAB}, EbmlString.class);
    //     [2] A unique ID to identify the next Segment of a Linked Segment (128 bits). If the Segment is a part of a Linked Segment that uses Hard
    //         Linking, then either the PrevUID or the NextUID Element is REQUIRED. If a Segment contains a NextUID but not a PrevUID, then it MAY
    //         be considered as the first Segment of the Linked Segment. The NextUID MUST NOT be equal to the SegmentUID.
    public final static MKVType NextUID = new MKVType("NextUID", new byte[]{0x3E, (byte)0xB9, 0x23}, EbmlBin.class);
    //     [2] A filename corresponding to the file of the next Linked Segment. Provision of the next filename is for display convenience, but
    //         NextUID SHOULD be considered authoritative for identifying the Next Segment.
    public final static MKVType NextFilename = new MKVType("NextFilenam", new byte[]{0x3E, (byte)0x83, (byte)0xBB}, EbmlString.class);
    //     [2] A randomly generated unique ID that all Segments of a Linked Segment MUST share (128 bits). If the Segment is a part of a Linked
    //         Segment that uses Soft Linking, then this Element is REQUIRED.
    public final static MKVType SegmentFamily = new MKVType("SegmentFamily", new byte[]{0x44,0x44}, EbmlBin.class);
    //     [2] A tuple of corresponding ID used by chapter codecs to represent this Segment.
    public final static MKVType ChapterTranslate = new MKVType("ChapterTranslate", new byte[]{0x69,0x24}, EbmlMaster.class);
    //         [3] Specify an edition UID on which this correspondence applies. When not specified, it means for all editions found in the Segment.
    public final static MKVType ChapterTranslateEditionUID = new MKVType("ChapterTranslateEditionUID", new byte[]{0x69, (byte) 0xFC}, EbmlUint.class);
    //         [3] The chapter codec
    //                 0 - Matroska Script,
    //                 1 - DVD-menu
    public final static MKVType ChapterTranslateCodec = new MKVType("ChapterTranslateCodec", new byte[]{0x69, (byte) 0xBF}, EbmlUint.class);
    //         [3] The binary value used to represent this Segment in the chapter codec data. The format depends on the ChapProcessCodecID used.
    public final static MKVType ChapterTranslateID = new MKVType("ChapterTranslateID", new byte[]{0x69, (byte)0xA5}, EbmlBin.class);
    //     [2] Timestamp scale in nanoseconds (1.000.000 means all timestamps in the Segment are expressed in milliseconds).
    public final static MKVType TimecodeScale = new MKVType("TimecodeScale", new byte[]{0x2A, (byte)0xD7, (byte)0xB1}, EbmlUint.class);
    //     [2] Duration of the Segment in nanoseconds based on TimestampScale.
    public final static MKVType Duration = new MKVType("Duration", new byte[]{0x44, (byte)0x89}, EbmlFloat.class);
    //     [2] The date and time that the Segment was created by the muxing application or library.
    public final static MKVType DateUTC = new MKVType("DateUTC", new byte[]{0x44, (byte)0x61}, EbmlDate.class);
    //     [2] General name of the Segment.
    public final static MKVType Title = new MKVType("Title", new byte[]{0x7B, (byte)0xA9}, EbmlString.class);
    //     [2] Muxing application or library (example: "libmatroska-0.4.3"). Include the full name of the application or library followed by the version number.
    public final static MKVType MuxingApp = new MKVType("MuxingApp", new byte[]{0x4D, (byte)0x80}, EbmlString.class);
    //     [2] Writing application (example: "mkvmerge-0.3.3"). Include the full name of the application followed by the version number.
    public final static MKVType WritingApp = new MKVType("WritingApp", new byte[]{0x57, 0x41}, EbmlString.class);

    // --------------------------------
    // [1] The Top-Level Element containing the (monolithic) Block structure.
    // --------------------------------
    public final static MKVType Cluster = new MKVType("Cluster", EbmlMaster.CLUSTER_ID, EbmlMaster.class);
    //     [2] Absolute timecode of the cluster (based on TimecodeScale).
    public final static MKVType Timecode = new MKVType("Timecode", new byte[]{(byte)0xE7}, EbmlUint.class);
    //     [2] The list of tracks that are not used in that part of the stream. It is useful when using overlay tracks on seeking. Then you should decide what track to use.
    public final static MKVType SilentTracks = new MKVType("SilentTracks", new byte[]{0x58, 0x54}, EbmlMaster.class);
    //         [3] One of the track number that are not used from now on in the stream. It could change later if not specified as silent in a further Cluster
    public final static MKVType SilentTrackNumber = new MKVType("SilentTrackNumber", new byte[]{0x58, (byte)0xD7}, EbmlUint.class);
    //     [2] The Position of the Cluster in the segment (0 in live broadcast streams). It might help to resynchronise offset on damaged streams.
    public final static MKVType Position = new MKVType("Position", new byte[]{(byte)0xA7}, EbmlUint.class);
    //     [2] Size of the previous Cluster, in octets. Can be useful for backward playing.
    public final static MKVType PrevSize = new MKVType("PrevSize", new byte[]{(byte)0xAB}, EbmlUint.class);
    //     [2] Similar to Block but without all the extra information, mostly used to reduced overhead when no extra feature is needed. (see SimpleBlock Structure)
    public final static MKVType SimpleBlock = new MKVType("SimpleBlock", MkvBlock.SIMPLEBLOCK_ID, MkvBlock.class);
    //     [2] Basic container of information containing a single Block or BlockVirtual, and information specific to that Block/VirtualBlock.
    public final static MKVType BlockGroup = new MKVType("BlockGroup", new byte[]{(byte)0xA0}, EbmlMaster.class);
    //         [3] Block containing the actual data to be rendered and a timecode relative to the Cluster Timecode. (see Block Structure)
    public final static MKVType Block = new MKVType("Block", MkvBlock.BLOCK_ID, MkvBlock.class);
    //         [3] Contain additional blocks to complete the main one. An EBML parser that has no knowledge of the Block structure could still see and use/skip these data.
    public final static MKVType BlockAdditions = new MKVType("BlockAdditions", new byte[]{0x75, (byte) 0xA1}, EbmlMaster.class);
    //             [4] Contain the BlockAdditional and some parameters.
    public final static MKVType BlockMore = new MKVType("BlockMore", new byte[]{(byte)0xA6}, EbmlMaster.class);
    //                 [5] An ID to identify the BlockAdditional level.
    public final static MKVType BlockAddID = new MKVType("BlockAddID", new byte[]{(byte)0xEE}, EbmlUint.class);
    //                 [5] Interpreted by the codec as it wishes (using the BlockAddID).
    public final static MKVType BlockAdditional = new MKVType("BlockAdditional", new byte[]{(byte) 0xA5}, EbmlBin.class);
    //         [3] The duration of the Block (based on TimestampScale). The BlockDuration Element can be useful at the end of a Track to define the duration of the last
    //             frame (as there is no subsequent Block available), or when there is a break in a track like for subtitle tracks. When not written and with no DefaultDuration,
    //             the value is assumed to be the difference between the timestamp of this Block and the timestamp of the next Block in "display" order (not coding order).
    //             BlockDuration MUST be set if the associated TrackEntry stores a DefaultDuration value.
    public final static MKVType BlockDuration = new MKVType("BlockDuration", new byte[]{(byte)0x9B}, EbmlUint.class);
    //         [3] This frame is referenced and has the specified cache priority. In cache only a frame of the same or higher priority can replace this frame. A value of 0
    //             means the frame is not referenced
    public final static MKVType ReferencePriority = new MKVType("ReferencePriority", new byte[]{(byte) 0xFA}, EbmlUint.class);
    //         [3] Timecode of another frame used as a reference (ie: B or P frame). The timecode is relative to the block it's attached to.
    public final static MKVType ReferenceBlock = new MKVType("ReferenceBlock", new byte[]{(byte)0xFB}, EbmlSint.class);
    //         [3] The new codec state to use. Data interpretation is private to the codec. This information should always be referenced by a seek entry.
    public final static MKVType CodecState = new MKVType("CodecState", new byte[]{(byte)0xA4}, EbmlBin.class);
    //         [3] Duration in nanoseconds of the silent data added to the Block (padding at the end of the Block for positive value, at the beginning of the Block
    //             for negative value). The duration of DiscardPadding is not calculated in the duration of the TrackEntry and SHOULD be discarded during playback.
    public final static MKVType DiscardPadding = new MKVType("DiscardPadding", new byte[]{0x75, (byte)0xA2}, EbmlSint.class);
    //         [3] Contains slices description.
    public final static MKVType Slices = new MKVType("Slices", new byte[]{(byte)0x8E}, EbmlMaster.class);
    //             [4] (DEPRECATED) Contains extra time information about the data contained in the Block. While there are a few files in the wild with this EbmlBase,
    //                 it is no longer in use and has been deprecated. Being able to interpret this EbmlBase is not required for playback.
    public final static MKVType TimeSlice = new MKVType("TimeSlice", new byte[]{(byte)0xE8}, EbmlMaster.class);
    //                 [5] (DEPRECATED) The reverse number of the frame in the lace (0 is the last frame, 1 is the next to last, etc). While there are a few files in
    //                     the wild with this EbmlBase, it is no longer in use and has been deprecated. Being able to interpret this EbmlBase is not required for playback.
    public final static MKVType LaceNumber = new MKVType("LaceNumber", new byte[]{(byte)0xCC}, EbmlUint.class);

    // --------------------------------
    // [1] A Top-Level Element of information with many tracks described.
    // --------------------------------
    public final static MKVType Tracks = new MKVType("Tracks", new byte[]{0x16, (byte)0x54, (byte)0xAE, (byte)0x6B}, EbmlMaster.class);
    //     [2] Describes a track with all Elements.
    public final static MKVType TrackEntry = new MKVType("TrackEntry", new byte[]{(byte)0xAE}, EbmlMaster.class);
    //         [3] The track number as used in the Block Header (using more than 127 tracks is not encouraged, though the design allows an unlimited number).
    public final static MKVType TrackNumber = new MKVType("TrackNumber", new byte[]{(byte)0xD7}, EbmlUint.class);
    //         [3] A unique ID to identify the Track. This should be kept the same when making a direct stream copy of the Track to another file.
    public final static MKVType TrackUID = new MKVType("TrackUID", new byte[]{0x73, (byte)0xC5}, EbmlUint.class);
    //         [3] A set of track types coded on 8 bits (1: video, 2: audio, 3: complex, 0x10: logo, 0x11: subtitle, 0x12: buttons, 0x20: control).
    public final static MKVType TrackType = new MKVType("TrackType", new byte[]{(byte)0x83}, EbmlUint.class);
    //         [3] Set if the track is usable. (1 bit)
    public final static MKVType FlagEnabled = new MKVType("FlagEnabled", new byte[]{(byte)0xB9}, EbmlUint.class);
    //         [3] Set if that track (audio, video or subs) SHOULD be active if no language found matches the user preference. (1 bit)
    public final static MKVType FlagDefault = new MKVType("FlagDefault", new byte[]{(byte)0x88}, EbmlUint.class);
    //         [3] Set if that track MUST be active during playback. There can be many forced track for a kind (audio, video or subs); the player should select the one which language matches the user preference or the default + forced track. Overlay MAY happen between a forced and non-forced track of the same kind. (1 bit)
    public final static MKVType FlagForced = new MKVType("FlagForced", new byte[]{0x55,(byte)0xAA}, EbmlUint.class);
    //         [3] Set if the track may contain blocks using lacing. (1 bit)
    public final static MKVType FlagLacing = new MKVType("FlagLacing", new byte[]{(byte)0x9C}, EbmlUint.class);
    //         [3] The minimum number of frames a player should be able to cache during playback. If set to 0, the reference pseudo-cache system is not used.
    public final static MKVType MinCache = new MKVType("MinCache", new byte[]{0x6D,(byte)0xE7}, EbmlUint.class);
    //         [3] The maximum cache size required to store referenced frames in and the current frame. 0 means no cache is needed.
    public final static MKVType MaxCache = new MKVType("MaxCache", new byte[]{0x6D,(byte)0xF8}, EbmlUint.class);
    //         [3] Number of nanoseconds (not scaled via TimecodeScale) per frame ('frame' in the Matroska sense -- one EbmlBase put into a (Simple)Block).
    public final static MKVType DefaultDuration = new MKVType("DefaultDuration", new byte[]{0x23, (byte)0xE3, (byte)0x83}, EbmlUint.class);
    //         [3] The period in nanoseconds (not scaled by TimestampScale) between two successive fields at the output of the decoding process
    public final static MKVType DefaultDecodedFieldDuration = new MKVType("DefaultDecodedFieldDuration", new byte[]{0x23, (byte)0x4E, (byte)0x7A}, EbmlUint.class);
    //         [3] The maximum value of BlockAddID. A value 0 means there is no BlockAdditions for this track.
    public final static MKVType MaxBlockAdditionID = new MKVType("MaxBlockAdditionID", new byte[]{0x55, (byte)0xEE}, EbmlUint.class);
    //         [3] Contains elements that extend the track format, by adding content either to each frame (with BlockAddID) or to the track as a whole (with BlockAddIDExtraData)
    public final static MKVType BlockAdditionMapping = new MKVType("BlockAdditionMapping", new byte[]{0x41, (byte)0xE4}, EbmlMaster.class);
    //             [4] If the track format extension needs content beside frames, the value refers to the BlockAddID value being described. To keep MaxBlockAdditionID as low as possible, small values SHOULD be used.
    public final static MKVType BlockAddIDValue = new MKVType("BlockAddIDValue", new byte[]{0x41, (byte)0xF0}, EbmlUint.class);
    //             [4] A human-friendly name describing the type of BlockAdditional data as defined by the associated Block Additional Mapping.
    public final static MKVType BlockAddIDName = new MKVType("BlockAddIDName", new byte[]{0x41, (byte)0xA4}, EbmlString.class);
    //             [4] Stores the registered identifer of the Block Additional Mapping to define how the BlockAdditional data should be handled.
    public final static MKVType BlockAddIDType = new MKVType("BlockAddIDType", new byte[]{0x41, (byte)0xE7}, EbmlUint.class);
    //             [4] Extra binary data that the BlockAddIDType can use to interpret the BlockAdditional data. The intepretation of the binary data depends on the BlockAddIDType value and the corresponding Block Additional Mapping.
    public final static MKVType BlockAddIDExtraData = new MKVType("BlockAddIDExtraData", new byte[]{0x41, (byte)0xED}, EbmlBin.class);
    //         [3] A human-readable track name.
    public final static MKVType Name = new MKVType("Name", new byte[]{0x53, 0x6E}, EbmlString.class);
    //         [3] Specifies the language of the track in the Matroska languages form.
    public final static MKVType Language = new MKVType("Language", new byte[]{0x22, (byte)0xB5, (byte)0x9C}, EbmlString.class);
    //         [3] Specifies the language of the track according to BCP 47 and using the IANA Language Subtag Registry. If this Element is used, then any Language Elements used in the same TrackEntry MUST be ignored.
    public final static MKVType LanguageIETF = new MKVType("LanguageIETF", new byte[]{0x22, (byte)0xB5, (byte)0x9C}, EbmlString.class);
    //         [3] An ID corresponding to the codec, see the codec page for more info.
    public final static MKVType CodecID = new MKVType("CodecID", new byte[]{(byte)0x86}, EbmlString.class);
    //         [3] Private data only known to the codec.
    public final static MKVType CodecPrivate = new MKVType("CodecPrivate", new byte[]{(byte)0x63, (byte)0xA2}, EbmlBin.class);
    //         [3] A human-readable string specifying the codec.
    public final static MKVType CodecName = new MKVType("CodecName", new byte[]{(byte)0x25,(byte)0x86,(byte)0x88}, EbmlString.class);
    //         [3] The UID of an attachment that is used by this codec. (DEPRECATED)
    public final static MKVType AttachmentLink = new MKVType("AttachmentLink", new byte[]{0x74, 0x46}, EbmlUint.class);
    //         [3] The codec can decode potentially damaged data (1 bit).
    public final static MKVType CodecDecodeAll = new MKVType("CodecDecodeAll", new byte[]{(byte)0xAA}, EbmlUint.class);
    //         [3] Specify that this track is an overlay track for the Track specified (in the u-integer). That means when this track has a gap (see SilentTracks) the overlay track should be used instead. The order of multiple TrackOverlay matters, the first one is the one that should be used. If not found it should be the second, etc.
    public final static MKVType TrackOverlay = new MKVType("TrackOverlay", new byte[]{0x6F,(byte)0xAB}, EbmlUint.class);
    //         [3] CodecDelay is The codec-built-in delay in nanoseconds. This value MUST be subtracted from each block timestamp in order to get the actual timestamp. The value SHOULD be small so the muxing of tracks with the same actual timestamp are in the same Cluster.
    public final static MKVType CodecDelay = new MKVType("CodecDelay", new byte[]{0x56,(byte)0xAA}, EbmlUint.class);
    //         [3] After a discontinuity, SeekPreRoll is the duration in nanoseconds of the data the decoder MUST decode before the decoded data is valid.
    public final static MKVType SeekPreRoll = new MKVType("SeekPreRoll", new byte[]{0x56,(byte)0xBB}, EbmlUint.class);
    //         [3] The track identification for the given Chapter Codec.
    public final static MKVType TrackTranslate = new MKVType("TrackTranslate", new byte[]{0x66, 0x24}, EbmlMaster.class);
    //             [4] Specify an edition UID on which this translation applies. When not specified, it means for all editions found in the segment.
    public final static MKVType TrackTranslateEditionUID = new MKVType("TrackTranslateEditionUID", new byte[]{0x66,(byte)0xFC}, EbmlUint.class);
    //             [4] The chapter codec using this ID (0: Matroska Script, 1: DVD-menu).
    public final static MKVType TrackTranslateCodec = new MKVType("TrackTranslateCodec", new byte[]{0x66,(byte)0xBF}, EbmlUint.class);
    //             [4] The binary value used to represent this track in the chapter codec data. The format depends on the ChapProcessCodecID used.
    public final static MKVType TrackTranslateTrackID = new MKVType("TrackTranslateTrackID", new byte[]{0x66,(byte)0xA5}, EbmlBin.class);
    //         [3] Video settings.
    public final static MKVType Video = new MKVType("Video", new byte[]{(byte)0xE0}, EbmlMaster.class);
    //             [4] A flag to declare if the video is known to be progressive or interlaced and if applicable to declare details about the interlacement.
    //                     0 - undetermined,
    //                     1 - interlaced,
    //                     2 - progressive
    public final static MKVType FlagInterlaced = new MKVType("FlagInterlaced", new byte[]{(byte)0x9A}, EbmlUint.class);
    //             [4] Declare the field ordering of the video. If FlagInterlaced is not set to 1, this Element MUST be ignored.
    //                     0 - progressive,
    //                     1 - tff,
    //                     2 - undetermined,
    //                     6 - bff,
    //                     9 - bff(swapped),
    //                     14 - tff(swapped)
    public final static MKVType FieldOrder = new MKVType("FieldOrder", new byte[]{(byte)0x9D}, EbmlUint.class);
    //             [4] Stereo-3D video mode. There are some more details on 3D support in the Specification Notes.
    //                     0 - mono,
    //                     1 - side by side (left eye first),
    //                     2 - top - bottom (right eye is first),
    //                     3 - top - bottom (left eye is first),
    //                     4 - checkboard (right eye is first),
    //                     5 - checkboard (left eye is first),
    //                     6 - row interleaved (right eye is first),
    //                     7 - row interleaved (left eye is first),
    //                     8 - column interleaved (right eye is first),
    //                     9 - column interleaved (left eye is first),
    //                     10 - anaglyph (cyan/red),
    //                     11 - side by side (right eye first),
    //                     12 - anaglyph (green/magenta),
    //                     13 - both eyes laced in one Block (left eye is first),
    //                     14 - both eyes laced in one Block (right eye is first)
    public final static MKVType StereoMode = new MKVType("StereoMode", new byte[]{0x53,(byte)0xB8}, EbmlUint.class);
    //             [4] Alpha Video Mode. Presence of this EbmlBase indicates that the BlockAdditional EbmlBase could contain Alpha data.
    public final static MKVType AlphaMode = new MKVType("AlphaMode", new byte[]{0x53,(byte)0xC0}, EbmlUint.class);
    //             [4] Width of the encoded video frames in pixels.
    public final static MKVType PixelWidth = new MKVType("PixelWidth", new byte[]{(byte)0xB0}, EbmlUint.class);
    //             [4] Height of the encoded video frames in pixels.
    public final static MKVType PixelHeight = new MKVType("PixelHeight", new byte[]{(byte)0xBA}, EbmlUint.class);
    //             [4] The number of video pixels to remove at the bottom of the image (for HDTV content).
    public final static MKVType PixelCropBottom = new MKVType("PixelCropBottom", new byte[]{0x54,(byte)0xAA}, EbmlUint.class);
    //             [4] The number of video pixels to remove at the top of the image.
    public final static MKVType PixelCropTop = new MKVType("PixelCropTop", new byte[]{0x54,(byte)0xBB}, EbmlUint.class);
    //             [4] The number of video pixels to remove on the left of the image.
    public final static MKVType PixelCropLeft = new MKVType("PixelCropLeft", new byte[]{0x54,(byte)0xCC}, EbmlUint.class);
    //             [4] The number of video pixels to remove on the right of the image.
    public final static MKVType PixelCropRight = new MKVType("PixelCropRight", new byte[]{0x54,(byte)0xDD}, EbmlUint.class);
    //             [4] Width of the video frames to display. Applies to the video frame after cropping (PixelCrop* Elements). If the DisplayUnit of the same TrackEntry
    //                 is 0, then the default value for DisplayWidth is equal to PixelWidth - PixelCropLeft - PixelCropRight, else there is no default value.
    public final static MKVType DisplayWidth = new MKVType("DisplayWidth", new byte[]{0x54, (byte)0xB0}, EbmlUint.class);
    //             [4] Height of the video frames to display. Applies to the video frame after cropping (PixelCrop* Elements). If the DisplayUnit of the same TrackEntry
    //                 is 0, then the default value for DisplayHeight is equal to PixelHeight - PixelCropTop - PixelCropBottom, else there is no default value.
    public final static MKVType DisplayHeight = new MKVType("DisplayHeight", new byte[]{0x54, (byte)0xBA}, EbmlUint.class);
    //             [4] How DisplayWidth & DisplayHeight should be interpreted (0: pixels, 1: centimeters, 2: inches, 3: Display Aspect Ratio).
    public final static MKVType DisplayUnit = new MKVType("DisplayUnit", new byte[]{0x54,(byte)0xB2}, EbmlUint.class);
    //             [4] Specify the possible modifications to the aspect ratio (0: free resizing, 1: keep aspect ratio, 2: fixed).
    public final static MKVType AspectRatioType = new MKVType("AspectRatioType", new byte[]{0x54,(byte)0xB3}, EbmlUint.class);
    //             [4] Same value as in AVI (32 bits).
    public final static MKVType ColourSpace = new MKVType("ColourSpace", new byte[]{0x2E, (byte)0xB5,0x24}, EbmlBin.class);
    //             [4] Settings describing the colour format.
    public final static MKVType Colour = new MKVType("Colour", new byte[]{0x55, (byte)0xB0}, EbmlMaster.class);
    //                 [5] The Matrix Coefficients of the video used to derive luma and chroma values from red, green, and blue color primaries. For clarity, the value and meanings for MatrixCoefficients are adopted from Table 4 of ISO/IEC 23001-8:2016 or ITU-T H.273.
    //                         0 - Identity,
    //                         1 - ITU-R BT.709,
    //                         2 - unspecified,
    //                         3 - reserved,
    //                         4 - US FCC 73.682,
    //                         5 - ITU-R BT.470BG,
    //                         6 - SMPTE 170M,
    //                         7 - SMPTE 240M,
    //                         8 - YCoCg,
    //                         9 - BT2020 Non-constant Luminance,
    //                         10 - BT2020 Constant Luminance,
    //                         11 - SMPTE ST 2085,
    //                         12 - Chroma-derived Non-constant Luminance,
    //                         13 - Chroma-derived Constant Luminance,
    //                         14 - ITU-R BT.2100-0
    public final static MKVType MatrixCoefficients = new MKVType("MatrixCoefficients", new byte[]{0x55, (byte)0xB1}, EbmlUint.class);
    //                 [5] Number of decoded bits per channel. A value of 0 indicates that the BitsPerChannel is unspecified.
    public final static MKVType BitsPerChannel = new MKVType("BitsPerChannel", new byte[]{0x55, (byte)0xB2}, EbmlUint.class);
    //                 [5] The amount of pixels to remove in the Cr and Cb channels for every pixel not removed horizontally. Example: For video with 4:2:0
    //                     chroma subsampling, the ChromaSubsamplingHorz SHOULD be set to 1.
    public final static MKVType ChromaSubsamplingHorz = new MKVType("ChromaSubsamplingHorz", new byte[]{0x55, (byte)0xB3}, EbmlUint.class);
    //                 [5] The amount of pixels to remove in the Cr and Cb channels for every pixel not removed vertically. Example: For video with 4:2:0 chroma
    //                     subsampling, the ChromaSubsamplingVert SHOULD be set to 1.
    public final static MKVType ChromaSubsamplingVert = new MKVType("ChromaSubsamplingVert", new byte[]{0x55, (byte)0xB4}, EbmlUint.class);
    //                 [5] The amount of pixels to remove in the Cb channel for every pixel not removed horizontally. This is additive with ChromaSubsamplingHorz.
    //                     Example: For video with 4:2:1 chroma subsampling, the ChromaSubsamplingHorz SHOULD be set to 1 and CbSubsamplingHorz SHOULD be set to 1.
    public final static MKVType CbSubsamplingHorz = new MKVType("CbSubsamplingHorz", new byte[]{0x55, (byte)0xB5}, EbmlUint.class);
    //                 [5] The amount of pixels to remove in the Cb channel for every pixel not removed vertically. This is additive with ChromaSubsamplingVert.
    public final static MKVType CbSubsamplingVert = new MKVType("CbSubsamplingVert", new byte[]{0x55, (byte)0xB6}, EbmlUint.class);
    //                 [5] How chroma is subsampled horizontally.
    //                         0 - unspecified,
    //                         1 - left collocated,
    //                         2 - half
    public final static MKVType ChromaSitingHorz = new MKVType("ChromaSitingHorz", new byte[]{0x55, (byte)0xB7}, EbmlUint.class);
    //                 [5] How chroma is subsampled vertically.
    //                         0 - unspecified,
    //                         1 - top collocated,
    //                         2 - half
    public final static MKVType ChromaSitingVert = new MKVType("ChromaSitingVert", new byte[]{0x55, (byte)0xB8}, EbmlUint.class);
    //                 [5] Clipping of the color ranges.
    //                         0 - unspecified,
    //                         1 - broadcast range,
    //                         2 - full range (no clipping),
    //                         3 - defined by MatrixCoefficients / TransferCharacteristics
    public final static MKVType Range = new MKVType("Range", new byte[]{0x55, (byte)0xB9}, EbmlUint.class);
    //                 [5] The transfer characteristics of the video. For clarity, the value and meanings for TransferCharacteristics are adopted from Table 3 of ISO/IEC 23091-4 or ITU-T H.273.
    //                         0 - reserved,
    //                         1 - ITU-R BT.709,
    //                         2 - unspecified,
    //                         3 - reserved,
    //                         4 - Gamma 2.2 curve - BT.470M,
    //                         5 - Gamma 2.8 curve - BT.470BG,
    //                         6 - SMPTE 170M,
    //                         7 - SMPTE 240M,
    //                         8 - Linear,
    //                         9 - Log,
    //                         10 - Log Sqrt,
    //                         11 - IEC 61966-2-4,
    //                         12 - ITU-R BT.1361 Extended Colour Gamut,
    //                         13 - IEC 61966-2-1,
    //                         14 - ITU-R BT.2020 10 bit,
    //                         15 - ITU-R BT.2020 12 bit,
    //                         16 - ITU-R BT.2100 Perceptual Quantization,
    //                         17 - SMPTE ST 428-1,
    //                         18 - ARIB STD-B67 (HLG)
    public final static MKVType TransferCharacteristics = new MKVType("TransferCharacteristics", new byte[]{0x55, (byte)0xBA}, EbmlUint.class);
    //                 [5] The colour primaries of the video. For clarity, the value and meanings for Primaries are adopted from Table 2 of ISO/IEC 23091-4 or ITU-T H.273.
    //                         0 - reserved,
    //                         1 - ITU-R BT.709,
    //                         2 - unspecified,
    //                         3 - reserved,
    //                         4 - ITU-R BT.470M,
    //                         5 - ITU-R BT.470BG - BT.601 625,
    //                         6 - ITU-R BT.601 525 - SMPTE 170M,
    //                         7 - SMPTE 240M,
    //                         8 - FILM,
    //                         9 - ITU-R BT.2020,
    //                         10 - SMPTE ST 428-1,
    //                         11 - SMPTE RP 432-2,
    //                         12 - SMPTE EG 432-2,
    //                         22 - EBU Tech. 3213-E - JEDEC P22 phosphors
    public final static MKVType Primaries = new MKVType("Primaries", new byte[]{0x55, (byte)0xBB}, EbmlUint.class);
    //                 [5] Maximum brightness of a single pixel (Maximum Content Light Level) in candelas per square meter (cd/m2).
    public final static MKVType MaxCLL = new MKVType("MaxCLL", new byte[]{0x55, (byte)0xBC}, EbmlUint.class);
    //                 [5] Maximum brightness of a single full frame (Maximum Frame-Average Light Level) in candelas per square meter (cd/m2).
    public final static MKVType MaxFALL = new MKVType("MaxFALL", new byte[]{0x55, (byte)0xBD}, EbmlUint.class);
    //                 [5] SMPTE 2086 mastering data.
    public final static MKVType MasteringMetadata = new MKVType("MasteringMetadata", new byte[]{0x55, (byte)0xD0}, EbmlMaster.class);
    //                     [6] Red X chromaticity coordinate as defined by CIE 1931.
    public final static MKVType PrimaryRChromaticityX = new MKVType("PrimaryRChromaticityX", new byte[]{0x55, (byte)0xD1}, EbmlFloat.class);
    //                     [6] Red Y chromaticity coordinate as defined by CIE 1931.
    public final static MKVType PrimaryRChromaticityY = new MKVType("PrimaryRChromaticityY", new byte[]{0x55, (byte)0xD2}, EbmlFloat.class);
    //                     [6] Green X chromaticity coordinate as defined by CIE 1931.
    public final static MKVType PrimaryGChromaticityX = new MKVType("PrimaryGChromaticityX", new byte[]{0x55, (byte)0xD3}, EbmlFloat.class);
    //                     [6] Green Y chromaticity coordinate as defined by CIE 1931.
    public final static MKVType PrimaryGChromaticityY = new MKVType("PrimaryGChromaticityY", new byte[]{0x55, (byte)0xD4}, EbmlFloat.class);
    //                     [6] Blue X chromaticity coordinate as defined by CIE 1931.
    public final static MKVType PrimaryBChromaticityX = new MKVType("PrimaryBChromaticityX", new byte[]{0x55, (byte)0xD5}, EbmlFloat.class);
    //                     [6] Blue Y chromaticity coordinate as defined by CIE 1931.
    public final static MKVType PrimaryBChromaticityY = new MKVType("PrimaryBChromaticityY", new byte[]{0x55, (byte)0xD6}, EbmlFloat.class);
    //                     [6] White X chromaticity coordinate as defined by CIE 1931.
    public final static MKVType WhitePointChromaticityX = new MKVType("WhitePointChromaticityX", new byte[]{0x55, (byte)0xD7}, EbmlFloat.class);
    //                     [6] White Y chromaticity coordinate as defined by CIE 1931.
    public final static MKVType WhitePointChromaticityY = new MKVType("WhitePointChromaticityY", new byte[]{0x55, (byte)0xD8}, EbmlFloat.class);
    //                     [6] Maximum luminance. Represented in candelas per square meter (cd/m2).
    public final static MKVType LuminanceMax = new MKVType("LuminanceMax", new byte[]{0x55, (byte)0xD9}, EbmlFloat.class);
    //                     [6] Minimum luminance. Represented in candelas per square meter (cd/m2).
    public final static MKVType LuminanceMin = new MKVType("LuminanceMin", new byte[]{0x55, (byte)0xDA}, EbmlFloat.class);
    //             [4] Describes the video projection details. Used to render spherical and VR videos.
    public final static MKVType Projection = new MKVType("Projection", new byte[]{0x76, 0x70}, EbmlMaster.class);
    //                 [5] Describes the projection used for this video track.
    //                         0 - rectangular,
    //                         1 - equirectangular,
    //                         2 - cubemap,
    //                         3 - mesh
    public final static MKVType ProjectionType = new MKVType("ProjectionType", new byte[]{0x76, (byte)0x71}, EbmlUint.class);
    //                 [5] Private data that only applies to a specific projection. * If ProjectionType equals 0 (Rectangular), then this element must not be present.
    //                     * If ProjectionType equals 1 (Equirectangular), then this element must be present and contain the same binary data that would be stored inside
    //                     an ISOBMFF Equirectangular Projection Box ('equi'). * If ProjectionType equals 2 (Cubemap), then this element must be present and contain the 
    //                     same binary data that would be stored inside an ISOBMFF Cubemap Projection Box ('cbmp'). * If ProjectionType equals 3 (Mesh), then this element
    //                     must be present and contain the same binary data that would be stored inside an ISOBMFF Mesh Projection Box ('mshp'). ISOBMFF box size and fourcc
    //                     fields are not included in the binary data, but the FullBox version and flag fields are. This is to avoid redundant framing information while
    //                     preserving versioning and semantics between the two container formats.
    public final static MKVType ProjectionPrivate = new MKVType("ProjectionPrivate", new byte[]{0x76, (byte)0x72}, EbmlBin.class);
    //                 [5] Specifies a yaw rotation to the projection. Value represents a clockwise rotation, in degrees, around the up vector. This rotation must be applied 
    //                     before any ProjectionPosePitch or ProjectionPoseRoll rotations. The value of this field should be in the -180 to 180 degree range.
    public final static MKVType ProjectionPoseYaw = new MKVType("ProjectionPoseYaw", new byte[]{0x76, (byte)0x73}, EbmlFloat.class);
    //                 [5] Specifies a pitch rotation to the projection. Value represents a counter-clockwise rotation, in degrees, around the right vector. This rotation must 
    //                     be applied after the ProjectionPoseYaw rotation and before the ProjectionPoseRoll rotation. The value of this field should be in the -90 to 90 degree range.
    public final static MKVType ProjectionPosePitch = new MKVType("ProjectionPosePitch", new byte[]{0x76, (byte)0x74}, EbmlFloat.class);
    //                 [5] Specifies a roll rotation to the projection. Value represents a counter-clockwise rotation, in degrees, around the forward vector. This rotation must 
    //                     be applied after the ProjectionPoseYaw and ProjectionPosePitch rotations. The value of this field should be in the -180 to 180 degree range.
    public final static MKVType ProjectionPoseRoll = new MKVType("ProjectionPoseRoll", new byte[]{0x76, (byte)0x75}, EbmlFloat.class);
    //         [3] Audio settings.
    public final static MKVType Audio = new MKVType("Audio", new byte[]{(byte)0xE1}, EbmlMaster.class);
    //             [4] Sampling frequency in Hz.
    public final static MKVType SamplingFrequency = new MKVType("SamplingFrequency", new byte[]{(byte)0xB5}, EbmlFloat.class);
    //             [4] Real output sampling frequency in Hz (used for SBR techniques). The default value for OutputSamplingFrequency of the same TrackEntry 
    //                 is equal to the SamplingFrequency.
    public final static MKVType OutputSamplingFrequency = new MKVType("OutputSamplingFrequency", new byte[]{0x78, (byte)0xB5}, EbmlFloat.class);
    //             [4] Numbers of channels in the track.
    public final static MKVType Channels = new MKVType("Channels", new byte[]{(byte)0x9F}, EbmlUint.class);
    //             [4] Bits per sample, mostly used for PCM.
    public final static MKVType BitDepth = new MKVType("BitDepth", new byte[]{0x62, 0x64}, EbmlUint.class);
    //         [3] Operation that needs to be applied on tracks to create this virtual track. For more details look at the Specification Notes on the subject.
    public final static MKVType TrackOperation = new MKVType("TrackOperation", new byte[]{(byte)0xE2}, EbmlMaster.class);
    //             [4] Contains the list of all video plane tracks that need to be combined to create this 3D track
    public final static MKVType TrackCombinePlanes = new MKVType("TrackCombinePlanes", new byte[]{(byte)0xE3}, EbmlMaster.class);
    //                 [5] Contains a video plane track that need to be combined to create this 3D track
    public final static MKVType TrackPlane = new MKVType("TrackPlane", new byte[]{(byte)0xE4}, EbmlMaster.class);
    //                     [6] The trackUID number of the track representing the plane.
    public final static MKVType TrackPlaneUID = new MKVType("TrackPlaneUID", new byte[]{(byte)0xE5}, EbmlUint.class);
    //                     [6] The kind of plane this track corresponds to (0: left eye, 1: right eye, 2: background).
    public final static MKVType TrackPlaneType = new MKVType("TrackPlaneType", new byte[]{(byte)0xE6}, EbmlUint.class);
    //                 [5] Contains the list of all tracks whose Blocks need to be combined to create this virtual track
    public final static MKVType TrackJoinBlocks = new MKVType("TrackJoinBlocks", new byte[]{(byte)0xE9}, EbmlMaster.class);
    //                     [6] The trackUID number of a track whose blocks are used to create this virtual track.
    public final static MKVType TrackJoinUID = new MKVType("TrackJoinUID", new byte[]{(byte)0xED}, EbmlUint.class);
    //         [3] Settings for several content encoding mechanisms like compression or encryption.
    public final static MKVType ContentEncodings = new MKVType("ContentEncodings", new byte[]{0x6D, (byte)0x80}, EbmlMaster.class);
    //             [4] Settings for one content encoding like compression or encryption.
    public final static MKVType ContentEncoding = new MKVType("ContentEncoding", new byte[]{0x62, 0x40}, EbmlMaster.class);
    //                 [5] Tells when this modification was used during encoding/muxing starting with 0 and counting upwards. The decoder/demuxer has to start
    //                     with the highest order number it finds and work its way down. This value has to be unique over all ContentEncodingOrder EbmlBases in the segment.
    public final static MKVType ContentEncodingOrder = new MKVType("ContentEncodingOrder", new byte[]{0x50, 0x31}, EbmlUint.class);
    //                 [5] A bit field that describes which Elements have been modified in this way. Values (big-endian) can be OR'ed.
    //                         1 - All frame contents, excluding lacing data,
    //                         2 - The track's private data,
    //                         4 - The next ContentEncoding (next ContentEncodingOrder. Either the data inside ContentCompression and/or ContentEncryption)
    public final static MKVType ContentEncodingScope = new MKVType("ContentEncodingScope", new byte[]{0x50, 0x32}, EbmlUint.class);
    //                 [5] A value describing what kind of transformation is applied.
    //                         0 - Compression,
    //                         1 - Encryption
    public final static MKVType ContentEncodingType = new MKVType("ContentEncodingType", new byte[]{0x50, 0x33}, EbmlUint.class);
    //                 [5] Settings describing the compression used. Must be present if the value of ContentEncodingType is 0 and absent otherwise.
    //                     Each block must be decompressable even if no previous block is available in order not to prevent seeking.
    public final static MKVType ContentCompression = new MKVType("ContentCompression", new byte[]{0x50, 0x34}, EbmlMaster.class);
    //                     [6] The compression algorithm used.
    //                             0 - zlib,
    //                             1 - bzlib,
    //                             2 - lzo1x,
    //                             3 - Header Stripping
    public final static MKVType ContentCompAlgo = new MKVType("ContentCompAlgo", new byte[]{0x42, (byte)0x54}, EbmlUint.class);
    //                     [6] Settings that might be needed by the decompressor. For Header Stripping (ContentCompAlgo=3); the bytes that were removed
    //                         from the beggining of each frames of the track.
    public final static MKVType ContentCompSettings = new MKVType("ContentCompSettings", new byte[]{0x42, 0x55}, EbmlBin.class);
    //                 [5] Settings describing the encryption used. Must be present if the value of ContentEncodingType is 1 and absent otherwise.
    public final static MKVType ContentEncryption = new MKVType("ContentEncryption", new byte[]{0x50, 0x35}, EbmlMaster.class);
    //                     [6] The encryption algorithm used. The value '0' means that the contents have not been encrypted but only signed.
    //                             0 - Not encrypted,
    //                             1 - DES - FIPS 46-3,
    //                             2 - Triple DES - RFC 1851,
    //                             3 - Twofish,
    //                             4 - Blowfish,
    //                             5 - AES - FIPS 187
    public final static MKVType ContentEncAlgo = new MKVType("ContentEncAlgo", new byte[]{0x47, (byte)0xE1}, EbmlUint.class);
    //                     [6] For public key algorithms this is the ID of the public key the the data was encrypted with.
    public final static MKVType ContentEncKeyID = new MKVType("ContentEncKeyID", new byte[]{0x47, (byte)0xE2}, EbmlBin.class);
    //                     [6] Settings describing the encryption algorithm used. If ContentEncAlgo != 5 this MUST be ignored.
    public final static MKVType ContentEncAESSettings = new MKVType("ContentEncAESSettings", new byte[]{0x47, (byte)0xE7}, EbmlMaster.class);
    //                         [7] The AES cipher mode used in the encryption.
    //                                 1 - AES-CTR / Counter, NIST SP 800-38A,
    //                                 2 - AES-CBC / Cipher Block Chaining, NIST SP 800-38A
    public final static MKVType AESSettingsCipherMode = new MKVType("AESSettingsCipherMode", new byte[]{0x47, (byte)0xE8}, EbmlUint.class);
    //                     [6] A cryptographic signature of the contents.
    public final static MKVType ContentSignature = new MKVType("ContentSignature", new byte[]{0x47, (byte)0xE3}, EbmlBin.class);
    //                     [6] This is the ID of the private key the data was signed with.
    public final static MKVType ContentSigKeyID = new MKVType("ContentSigKeyID", new byte[]{0x47, (byte)0xE4}, EbmlBin.class);
    //                     [6] The algorithm used for the signature.
    //                             0 - Not signed,
    //                             1 - RSA
    public final static MKVType ContentSigAlgo = new MKVType("ContentSigAlgo", new byte[]{0x47, (byte)0xE5}, EbmlUint.class);
    //                     [6] The hash algorithm used for the signature.
    //                             0 - Not signed,
    //                             1 - SHA1-160,
    //                             2 - MD5
    public final static MKVType ContentSigHashAlgo = new MKVType("ContentSigHashAlgo", new byte[]{0x47, (byte)0xE6}, EbmlUint.class);

    // --------------------------------
    // [1] A Top-Level Element to speed seeking access. All entries are local to the Segment. This Element SHOULD be set when
    //     the Segment is not transmitted as a live stream (see #livestreaming).
    // --------------------------------
    public final static MKVType Cues = new MKVType("Cues", new byte[]{0x1C,0x53,(byte)0xBB,0x6B}, EbmlMaster.class);
    //     [2] Contains all information relative to a seek point in the Segment.
    public final static MKVType CuePoint = new MKVType("CuePoint", new byte[]{(byte)0xBB}, EbmlMaster.class);
    //         [3] Absolute timestamp according to the Segment time base.
    public final static MKVType CueTime = new MKVType("CueTime", new byte[]{(byte)0xB3}, EbmlUint.class);
    //         [3] Contain positions for different tracks corresponding to the timestamp.
    public final static MKVType CueTrackPositions = new MKVType("CueTrackPositions", new byte[]{(byte)0xB7}, EbmlMaster.class);
    //             [4] The track for which a position is given.
    public final static MKVType CueTrack = new MKVType("CueTrack", new byte[]{(byte)0xF7}, EbmlUint.class);
    //             [4] The Segment Position of the Cluster containing the associated Block.
    public final static MKVType CueClusterPosition = new MKVType("CueClusterPosition", new byte[]{(byte)0xF1}, EbmlUint.class);
    //             [4] The relative position of the referenced block inside the cluster with 0 being the first possible position for an EbmlBase inside that cluster.
    public final static MKVType CueRelativePosition = new MKVType("CueRelativePosition", new byte[]{(byte)0xF0}, EbmlUint.class);
    //             [4] The duration of the block according to the segment time base. If missing the track's DefaultDuration does not apply and no duration information is available in terms of the cues.
    public final static MKVType CueDuration = new MKVType("CueDuration", new byte[]{(byte)0xB2}, EbmlUint.class);
    //             [4] Number of the Block in the specified Cluster.
    public final static MKVType CueBlockNumber = new MKVType("CueBlockNumber", new byte[]{0x53, 0x78}, EbmlUint.class);
    //             [4] The position of the Codec State corresponding to this Cue EbmlBase. 0 means that the data is taken from the initial Track Entry.
    public final static MKVType CueCodecState = new MKVType("CueCodecState", new byte[]{(byte)0xEA}, EbmlUint.class);
    //             [4] The Clusters containing the required referenced Blocks.
    public final static MKVType CueReference = new MKVType("CueReference", new byte[]{(byte)0xDB}, EbmlMaster.class);
    //                 [5] Timecode of the referenced Block.
    public final static MKVType CueRefTime = new MKVType("CueRefTime", new byte[]{(byte)0x96}, EbmlUint.class);

    // --------------------------------
    // [1] Contain attached files.
    // --------------------------------
    public final static MKVType Attachments = new MKVType("Attachments", new byte[]{0x19, 0x41, (byte)0xA4, 0x69}, EbmlMaster.class);
    //     [2] An attached file.
    public final static MKVType AttachedFile = new MKVType("AttachedFile", new byte[]{0x61, (byte)0xA7}, EbmlMaster.class);
    //         [3] A human-friendly name for the attached file.
    public final static MKVType FileDescription = new MKVType("FileDescription", new byte[]{0x46, (byte)0x7E}, EbmlString.class);
    //         [3] Filename of the attached file.
    public final static MKVType FileName = new MKVType("FileName", new byte[]{0x46, (byte)0x6E}, EbmlString.class);
    //         [3] MIME type of the file.
    public final static MKVType FileMimeType = new MKVType("FileMimeType", new byte[]{0x46, (byte)0x60}, EbmlString.class);
    //         [3] The data of the file.
    public final static MKVType FileData = new MKVType("FileData", new byte[]{0x46, (byte)0x5C}, EbmlBin.class);
    //         [3] Unique ID representing the file, as random as possible.
    public final static MKVType FileUID = new MKVType("FileUID", new byte[]{0x46, (byte)0xAE}, EbmlUint.class);

    // --------------------------------
    // [1] A system to define basic menus and partition data. For more detailed information, look at the Chapters Explanation.
    // --------------------------------
    public final static MKVType Chapters = new MKVType("Chapters", new byte[]{0x10, (byte)0x43, (byte)0xA7, (byte)0x70}, EbmlMaster.class);
    //     [2] Contains all information about a Segment edition.
    public final static MKVType EditionEntry = new MKVType("EditionEntry", new byte[]{(byte)0x45, (byte)0xB9}, EbmlMaster.class);
    //         [3] A unique ID to identify the edition. It's useful for tagging an edition.
    public final static MKVType EditionUID = new MKVType("EditionUID", new byte[]{(byte)0x45, (byte)0xBC}, EbmlUint.class);
    //         [3] 	If an edition is hidden (1), it SHOULD NOT be available to the user interface (but still to Control Tracks; see flag notes). (1 bit)
    public final static MKVType EditionFlagHidden = new MKVType("EditionFlagHidden", new byte[]{(byte)0x45, (byte)0xBD}, EbmlUint.class);
    //         [3] If a flag is set (1) the edition SHOULD be used as the default one. (1 bit)
    public final static MKVType EditionFlagDefault = new MKVType("EditionFlagDefault", new byte[]{(byte)0x45, (byte)0xDB}, EbmlUint.class);
    //         [3] Specify if the chapters can be defined multiple times and the order to play them is enforced. (1 bit)
    public final static MKVType EditionFlagOrdered = new MKVType("EditionFlagOrdered", new byte[]{(byte)0x45, (byte)0xDD}, EbmlUint.class);
    //         [3+] Contains the atom information to use as the chapter atom (apply to all tracks).
    public final static MKVType ChapterAtom = new MKVType("ChapterAtom", new byte[]{(byte)0xB6}, EbmlMaster.class);
    //             [4] A unique ID to identify the Chapter.
    public final static MKVType ChapterUID = new MKVType("ChapterUID", new byte[]{(byte)0x73, (byte)0xC4}, EbmlUint.class);
    //             [4] A unique string ID to identify the Chapter. Use for WebVTT cue identifier storage.
    public final static MKVType ChapterStringUID = new MKVType("ChapterStringUID", new byte[]{0x56,0x54}, EbmlString.class);
    //             [4] Timestamp of the start of Chapter (not scaled).
    public final static MKVType ChapterTimeStart = new MKVType("ChapterTimeStart", new byte[]{(byte)0x91}, EbmlUint.class);
    //             [4] Timestamp of the end of Chapter (timestamp excluded, not scaled).
    public final static MKVType ChapterTimeEnd = new MKVType("ChapterTimeEnd", new byte[]{(byte)0x92}, EbmlUint.class);
    //             [4] If a chapter is hidden (1), it SHOULD NOT be available to the user interface (but still to Control Tracks; see flag notes). (1 bit)
    public final static MKVType ChapterFlagHidden = new MKVType("ChapterFlagHidden", new byte[]{(byte)0x98}, EbmlUint.class);
    //             [4] Specify whether the chapter is enabled. It can be enabled/disabled by a Control Track. When disabled, the movie SHOULD skip 
    //                 all the content between the TimeStart and TimeEnd of this chapter (see flag notes). (1 bit)
    public final static MKVType ChapterFlagEnabled = new MKVType("ChapterFlagEnabled", new byte[]{(byte)0x45, (byte)0x98}, EbmlUint.class);
    //             [4] A segment to play in place of this chapter. Edition ChapterSegmentEditionUID should be used for this segment, otherwise no edition is used.
    public final static MKVType ChapterSegmentUID = new MKVType("ChapterSegmentUID", new byte[]{0x6E,0x67}, EbmlBin.class);
    //             [4] The EditionUID to play from the Segment linked in ChapterSegmentUID. If ChapterSegmentEditionUID is undeclared, then no Edition of the
    //                 linked Segment is used.
    public final static MKVType ChapterSegmentEditionUID = new MKVType("ChapterSegmentEditionUID", new byte[]{0x6E,(byte)0xBC}, EbmlUint.class);
    //             [4] Specify the physical equivalent of this ChapterAtom like "DVD" (60) or "SIDE" (50), see complete list of values.
    public final static MKVType ChapterPhysicalEquiv = new MKVType("ChapterPhysicalEquiv", new byte[]{(byte)0x63, (byte)0xC3}, EbmlUint.class);
    //             [4] List of tracks on which the chapter applies. If this Element is not present, all tracks apply
    public final static MKVType ChapterTrack = new MKVType("ChapterTrack", new byte[]{(byte)0x8F}, EbmlMaster.class);
    //                 [5] UID of the Track to apply this chapter to. In the absence of a control track, choosing this chapter will select the listed
    //                     Tracks and deselect unlisted tracks. Absence of this Element indicates that the Chapter SHOULD be applied to any currently used Tracks.
    public final static MKVType ChapterTrackNumber = new MKVType("ChapterTrackNumber", new byte[]{(byte)0x89}, EbmlUint.class);
    //             [4] Contains all possible strings to use for the chapter display.
    public final static MKVType ChapterDisplay = new MKVType("ChapterDisplay", new byte[]{(byte)0x80}, EbmlMaster.class);
    //                 [5] Contains the string to use as the chapter atom.
    public final static MKVType ChapString = new MKVType("ChapString", new byte[]{(byte)0x85}, EbmlString.class);
    //                 [5] The languages corresponding to the string, in the bibliographic ISO-639-2 form. This Element MUST be ignored if the ChapLanguageIETF
    //                     Element is used within the same ChapterDisplay Element.
    public final static MKVType ChapLanguage = new MKVType("ChapLanguage", new byte[]{(byte)0x43, (byte)0x7C}, EbmlString.class);
    //                 [5] Specifies the language used in the ChapString according to BCP 47 and using the IANA Language Subtag Registry. If this Element is used,
    //                     then any ChapLanguage Elements used in the same ChapterDisplay MUST be ignored.
    public final static MKVType ChapLanguageIETF = new MKVType("ChapLanguageIETF", new byte[]{(byte)0x43, (byte)0x7D}, EbmlString.class);
    //                 [5] The countries corresponding to the string, same 2 octets as in Internet domains. This Element MUST be ignored if the ChapLanguageIETF
    //                     Element is used within the same ChapterDisplay Element.
    public final static MKVType ChapCountry = new MKVType("ChapCountry", new byte[]{(byte)0x43, (byte)0x7E}, EbmlString.class);
    //             [4] Contains all the commands associated to the Atom.
    public final static MKVType ChapProcess = new MKVType("ChapProcess", new byte[]{0x69, 0x44}, EbmlMaster.class); 
    //                 [5] Contains the type of the codec used for the processing. A value of 0 means native Matroska processing (to be defined); a value of 1 means
    //                     the DVD command set is used. More codec IDs can be added later.
    public final static MKVType ChapProcessCodecID = new MKVType("ChapProcessCodecID", new byte[]{0x69, 0x55}, EbmlUint.class);
    //                 [5] Some optional data attached to the ChapProcessCodecID information. For ChapProcessCodecID = 1, it is the "DVD level" equivalent.
    public final static MKVType ChapProcessPrivate = new MKVType("ChapProcessPrivate", new byte[]{0x45, 0x0D}, EbmlBin.class);
    //                 [5] Contains all the commands associated to the Atom.
    public final static MKVType ChapProcessCommand = new MKVType("ChapProcessCommand", new byte[]{0x69, 0x11}, EbmlMaster.class);
    //                     [6] Defines when the process command should be handled (0: during the whole chapter, 1: before starting playback, 2: after playback of the chapter).
    public final static MKVType ChapProcessTime = new MKVType("ChapProcessTime", new byte[]{0x69, 0x22}, EbmlUint.class);
    //                     [6] Contains the command information. The data should be interpreted depending on the ChapProcessCodecID value.
    //                         For ChapProcessCodecID = 1, the data correspond to the binary DVD cell pre/post commands.
    public final static MKVType ChapProcessData = new MKVType("ChapProcessData", new byte[]{0x69, 0x33}, EbmlBin.class);

    // --------------------------------
    // [1] Element containing metadata describing Tracks, Editions, Chapters, Attachments, or the Segment as a whole. A list of valid tags can be found here.
    // --------------------------------
    public final static MKVType Tags = new MKVType("Tags", new byte[]{0x12, (byte)0x54, (byte)0xC3, (byte)0x67}, EbmlMaster.class);
    //     [2] A single metadata descriptor.
    public final static MKVType Tag = new MKVType("Tag", new byte[]{0x73, (byte)0x73}, EbmlMaster.class);
    //         [3] Specifies which other elements the metadata represented by the Tag applies to. If empty or not present, then the Tag describes everything in the Segment.
    public final static MKVType               Targets = new MKVType("Targets", new byte[]{0x63, (byte)0xC0}, EbmlMaster.class);
    //             [4] A number to indicate the logical level of the target.
    //                     70 - COLLECTION,
    //                     60 - EDITION / ISSUE / VOLUME / OPUS / SEASON / SEQUEL,
    //                     50 - ALBUM / OPERA / CONCERT / MOVIE / EPISODE / CONCERT,
    //                     40 - PART / SESSION,
    //                     30 - TRACK / SONG / CHAPTER,
    //                     20 - SUBTRACK / PART / MOVEMENT / SCENE,
    //                     10 - SHOT
    public final static MKVType TargetTypeValue = new MKVType("TargetTypeValue", new byte[]{0x68, (byte) 0xCA}, EbmlUint.class);
    //             [4] An informational string that can be used to display the logical level of the target like "ALBUM", "TRACK", "MOVIE", "CHAPTER", etc (see TargetType).
    //                     COLLECTION - COLLECTION,
    //                     EDITION - EDITION,
    //                     ISSUE - ISSUE,
    //                     VOLUME - VOLUME,
    //                     OPUS - OPUS,
    //                     SEASON - SEASON,
    //                     SEQUEL - SEQUEL,
    //                     ALBUM - ALBUM,
    //                     OPERA - OPERA,
    //                     CONCERT - CONCERT,
    //                     MOVIE - MOVIE,
    //                     EPISODE - EPISODE,
    //                     PART - PART,
    //                     SESSION - SESSION,
    //                     TRACK - TRACK,
    //                     SONG - SONG,
    //                     CHAPTER - CHAPTER,
    //                     SUBTRACK - SUBTRACK,
    //                     PART - PART,
    //                     MOVEMENT - MOVEMENT,
    //                     SCENE - SCENE,
    //                     SHOT - SHOT
    public final static MKVType TargetType = new MKVType("TargetType", new byte[]{0x63,(byte)0xCA}, EbmlString.class);
    //             [4] A unique ID to identify the Track(s) the tags belong to. If the value is 0 at this level, the tags apply to all tracks in the Segment.
    public final static MKVType TagTrackUID = new MKVType("TagTrackUID", new byte[]{0x63, (byte)0xC5}, EbmlUint.class);
    //             [4] A unique ID to identify the EditionEntry(s) the tags belong to. If the value is 0 at this level, the tags apply to all editions in the Segment.
    public final static MKVType TagEditionUID = new MKVType("TagEditionUID", new byte[]{0x63,(byte)0xC9}, EbmlUint.class);
    //             [4] A unique ID to identify the Chapter(s) the tags belong to. If the value is 0 at this level, the tags apply to all chapters in the Segment.
    public final static MKVType TagChapterUID = new MKVType("TagChapterUID", new byte[]{0x63, (byte)0xC4}, EbmlUint.class);
    //             [4] A unique ID to identify the Attachment(s) the tags belong to. If the value is 0 at this level, the tags apply to all the attachments in the Segment.
    public final static MKVType TagAttachmentUID = new MKVType("TagAttachmentUID", new byte[]{0x63, (byte)0xC6}, EbmlUint.class);
    //         [3+] Contains general information about the target.
    public final static MKVType SimpleTag = new MKVType("SimpleTag", new byte[]{0x67, (byte)0xC8}, EbmlMaster.class);
    //             [4] The name of the Tag that is going to be stored.
    public final static MKVType TagName = new MKVType("TagName", new byte[]{0x45, (byte)0xA3}, EbmlString.class);
    //             [4] Specifies the language of the tag specified, in the Matroska languages form. This Element MUST be ignored if the TagLanguageIETF Element is
    //                 used within the same SimpleTag Element.
    public final static MKVType TagLanguage = new MKVType("TagLanguage", new byte[]{0x44, 0x7A}, EbmlString.class);
    //             [4] Specifies the language used in the TagString according to BCP 47 and using the IANA Language Subtag Registry. If this Element is used, then any TagLanguage Elements used in the same SimpleTag MUST be ignored.
    public final static MKVType TagLanguageIETF = new MKVType("TagLanguageIETF", new byte[]{0x44, 0x7B}, EbmlString.class);
    //             [4] A boolean value to indicate if this is the default/original language to use for the given tag.
    public final static MKVType TagDefault = new MKVType("TagDefault", new byte[]{0x44, (byte)0x84}, EbmlUint.class);
    //             [4] The value of the Tag.
    public final static MKVType TagString = new MKVType("TagString", new byte[]{0x44, (byte)0x87}, EbmlString.class);
    //             [4] The values of the Tag if it is binary. Note that this cannot be used in the same SimpleTag as TagString.
    public final static MKVType TagBinary = new MKVType("TagBinary", new byte[]{0x44, (byte)0x85}, EbmlBin.class);

    static public MKVType[] firstLevelHeaders = {SeekHead, Info, Cluster, Tracks, Cues, Attachments, Chapters, Tags, EBMLVersion, EBMLReadVersion, EBMLMaxIDLength, EBMLMaxSizeLength, DocType, DocTypeVersion, DocTypeReadVersion };
    public final byte [] id;
    public final Class<? extends EbmlBase> clazz;
    private String _name;

    private MKVType(String name, byte[] id, Class<? extends EbmlBase> clazz) {
        this._name = name;
        this.id = id;
        this.clazz = clazz;
        _values.add(this);
    }
    public String name() {
        return _name;
    }
    public String toString() {
        return _name;
    }
      
    public static MKVType[] values() {
        return _values.toArray(new MKVType[0]);
    }
      
    @SuppressWarnings("unchecked")
    public static <T extends EbmlBase> T createByType(MKVType g) {
        try {
            T elem = (T) Platform.newInstance(g.clazz, new Object[] { g.id });
            elem.type = g;
            return elem;
        } catch (Exception e) {
            e.printStackTrace();
            return (T) new EbmlBin(g.id);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends EbmlBase> T createById(byte[] id, long offset) {
        MKVType[] values = values();
        for (int i = 0; i < values.length; i++) {
            MKVType t = values[i];
            if (Platform.arrayEqualsByte(t.id, id))
                return createByType(t);
        }
        System.err.println("WARNING: unspecified ebml ID (" + toHexString(id) + ") encountered at position 0x"
                + Long.toHexString(offset).toUpperCase());
        T t = (T) new EbmlVoid(id);
        t.type = Void;
        return t;
    }
      
    public static boolean isHeaderFirstByte(byte b) {
        MKVType[] values = values();
        for (int i = 0; i < values.length; i++) {
            MKVType t = values[i];
            if (t.id[0] == b)
                return true;
        }

        return false;
    }
      
    public static boolean isSpecifiedHeader(byte[] b) {
        MKVType[] values = values();
        for (int i = 0; i < values.length; i++) {
            MKVType firstLevelHeader = values[i];
            if (Platform.arrayEqualsByte(firstLevelHeader.id, b))
                return true;
        }
        return false;
    }
      
      public static boolean isFirstLevelHeader(byte[] b){
          for (MKVType firstLevelHeader : firstLevelHeaders)
              if (Platform.arrayEqualsByte(firstLevelHeader.id, b))
                  return true;
          return false;
      }
      
      public static final Map<MKVType, Set<MKVType>> children = new HashMap<MKVType, Set<MKVType>>();
      static {
          children.put(EBML, new HashSet<MKVType>(Arrays.asList(new MKVType[]{EBMLVersion, EBMLReadVersion, EBMLMaxIDLength, EBMLMaxSizeLength, DocType, DocTypeVersion, DocTypeReadVersion})));
          children.put(Segment, new HashSet<MKVType>(Arrays.asList(new MKVType[]{SeekHead, Info, Cluster, Tracks, Cues, Attachments, Chapters, Tags})));
          
          children.put(SeekHead, new HashSet<MKVType>(Arrays.asList(new MKVType[]{Seek})));
          children.put(Seek, new HashSet<MKVType>(Arrays.asList(new MKVType[]{SeekID, SeekPosition})));
          children.put(Info, new HashSet<MKVType>(Arrays.asList(new MKVType[]{SegmentUID, SegmentFilename, PrevUID, PrevFilename, NextUID, NextFilename, SegmentFamily, ChapterTranslate, TimecodeScale, Duration , DateUTC, Title, MuxingApp, WritingApp})));
          children.put(ChapterTranslate, new HashSet<MKVType>(Arrays.asList(new MKVType[]{ChapterTranslateEditionUID, ChapterTranslateCodec, ChapterTranslateID})));
          
          children.put(Cluster, new HashSet<MKVType>(Arrays.asList(new MKVType[]{Timecode, SilentTracks, Position, PrevSize, SimpleBlock, BlockGroup})));
          children.put(SilentTracks, new HashSet<MKVType>(Arrays.asList(new MKVType[]{SilentTrackNumber})));
          children.put(BlockGroup, new HashSet<MKVType>(Arrays.asList(new MKVType[]{Block, BlockAdditions, BlockDuration, ReferencePriority, ReferenceBlock, CodecState, DiscardPadding, Slices})));
          children.put(BlockAdditions, new HashSet<MKVType>(Arrays.asList(new MKVType[]{BlockMore})));
          children.put(BlockMore, new HashSet<MKVType>(Arrays.asList(new MKVType[]{BlockAddID, BlockAdditional})));
          children.put(Slices, new HashSet<MKVType>(Arrays.asList(new MKVType[]{TimeSlice})));
          children.put(TimeSlice, new HashSet<MKVType>(Arrays.asList(new MKVType[]{LaceNumber})));

          children.put(Tracks, new HashSet<MKVType>(Arrays.asList(new MKVType[]{TrackEntry})));
          children.put(TrackEntry, new HashSet<MKVType>(Arrays.asList(new MKVType[]{TrackNumber, TrackUID, TrackType, TrackType, FlagDefault, FlagForced, FlagLacing, MinCache, MaxCache, DefaultDuration, DefaultDecodedFieldDuration, MaxBlockAdditionID, BlockAdditionMapping, Name, Language, LanguageIETF, CodecID, CodecPrivate, CodecName, AttachmentLink, CodecDecodeAll, TrackOverlay, CodecDelay, SeekPreRoll, TrackTranslate, Video, Audio, TrackOperation, ContentEncodings})));
          children.put(BlockAdditionMapping, new HashSet<MKVType>(Arrays.asList(new MKVType[]{BlockAddIDValue, BlockAddIDName, BlockAddIDType, BlockAddIDExtraData})));
          children.put(TrackTranslate, new HashSet<MKVType>(Arrays.asList(new MKVType[]{TrackTranslateEditionUID, TrackTranslateCodec, TrackTranslateTrackID})));
          children.put(Video, new HashSet<MKVType>(Arrays.asList(new MKVType[]{FlagInterlaced, FieldOrder, StereoMode, AlphaMode, PixelWidth, PixelHeight, PixelCropBottom, PixelCropTop, PixelCropLeft, PixelCropRight, DisplayWidth, DisplayHeight, DisplayUnit, AspectRatioType, ColourSpace, Colour, Projection})));
          children.put(Projection, new HashSet<MKVType>(Arrays.asList(new MKVType[]{ProjectionType, ProjectionPrivate, ProjectionPoseYaw, ProjectionPosePitch, ProjectionPoseRoll})));
          children.put(Colour, new HashSet<MKVType>(Arrays.asList(new MKVType[]{MatrixCoefficients, BitsPerChannel, ChromaSubsamplingHorz, ChromaSubsamplingVert, CbSubsamplingHorz, CbSubsamplingVert, ChromaSitingHorz, ChromaSitingVert, Range, TransferCharacteristics, Primaries, MaxCLL, MaxFALL, MasteringMetadata})));
          children.put(MasteringMetadata, new HashSet<MKVType>(Arrays.asList(new MKVType[]{PrimaryRChromaticityX, PrimaryRChromaticityY, PrimaryGChromaticityX, PrimaryGChromaticityY, PrimaryBChromaticityX, PrimaryBChromaticityY, WhitePointChromaticityX, WhitePointChromaticityY, LuminanceMax, LuminanceMin})));
          children.put(Audio, new HashSet<MKVType>(Arrays.asList(new MKVType[]{SamplingFrequency, OutputSamplingFrequency, Channels, BitDepth})));
          children.put(TrackOperation, new HashSet<MKVType>(Arrays.asList(new MKVType[]{TrackCombinePlanes, TrackJoinBlocks})));
          children.put(TrackCombinePlanes, new HashSet<MKVType>(Arrays.asList(new MKVType[]{TrackPlane})));
          children.put(TrackPlane, new HashSet<MKVType>(Arrays.asList(new MKVType[]{TrackPlaneUID, TrackPlaneType})));
          children.put(TrackJoinBlocks, new HashSet<MKVType>(Arrays.asList(new MKVType[]{TrackJoinUID})));
          children.put(ContentEncodings, new HashSet<MKVType>(Arrays.asList(new MKVType[]{ContentEncoding})));
          children.put(ContentEncoding, new HashSet<MKVType>(Arrays.asList(new MKVType[]{ContentEncodingOrder, ContentEncodingScope, ContentEncodingType, ContentCompression, ContentEncryption})));
          children.put(ContentCompression, new HashSet<MKVType>(Arrays.asList(new MKVType[]{ContentCompAlgo, ContentCompSettings})));
          children.put(ContentEncryption, new HashSet<MKVType>(Arrays.asList(new MKVType[]{ContentEncAlgo, ContentEncKeyID, ContentEncAESSettings, ContentSignature, ContentSigKeyID, ContentSigAlgo, ContentSigHashAlgo})));
          children.put(ContentEncAESSettings, new HashSet<MKVType>(Arrays.asList(new MKVType[]{AESSettingsCipherMode})));

          children.put(Cues, new HashSet<MKVType>(Arrays.asList(new MKVType[]{CuePoint})));
          children.put(CuePoint, new HashSet<MKVType>(Arrays.asList(new MKVType[]{CueTime, CueTrackPositions})));
          children.put(CueTrackPositions, new HashSet<MKVType>(Arrays.asList(new MKVType[]{CueTrack, CueClusterPosition, CueRelativePosition, CueDuration, CueBlockNumber, CueCodecState, CueReference})));
          children.put(CueReference, new HashSet<MKVType>(Arrays.asList(new MKVType[]{CueRefTime})));
          
          children.put(Attachments, new HashSet<MKVType>(Arrays.asList(new MKVType[]{AttachedFile})));
          children.put(AttachedFile, new HashSet<MKVType>(Arrays.asList(new MKVType[]{FileDescription, FileName, FileMimeType, FileData, FileUID})));
          
          children.put(Chapters, new HashSet<MKVType>(Arrays.asList(new MKVType[]{EditionEntry})));
          children.put(EditionEntry, new HashSet<MKVType>(Arrays.asList(new MKVType[]{EditionUID, EditionFlagHidden, EditionFlagDefault, EditionFlagOrdered, ChapterAtom})));
          children.put(ChapterAtom, new HashSet<MKVType>(Arrays.asList(new MKVType[]{ChapterUID, ChapterStringUID, ChapterTimeStart, ChapterTimeEnd, ChapterFlagHidden, ChapterFlagEnabled, ChapterSegmentUID, ChapterSegmentEditionUID, ChapterPhysicalEquiv, ChapterTrack, ChapterDisplay, ChapProcess})));
          children.put(ChapterTrack, new HashSet<MKVType>(Arrays.asList(new MKVType[]{ChapterTrackNumber})));
          children.put(ChapterDisplay, new HashSet<MKVType>(Arrays.asList(new MKVType[]{ChapString, ChapLanguage, ChapLanguageIETF, ChapCountry})));
          children.put(ChapProcess, new HashSet<MKVType>(Arrays.asList(new MKVType[]{ChapProcessCodecID, ChapProcessPrivate, ChapProcessCommand})));
          children.put(ChapProcessCommand, new HashSet<MKVType>(Arrays.asList(new MKVType[]{ChapProcessTime, ChapProcessData})));
          
          children.put(Tags, new HashSet<MKVType>(Arrays.asList(new MKVType[]{Tag})));
          children.put(Tag, new HashSet<MKVType>(Arrays.asList(new MKVType[]{Targets, SimpleTag})));
          children.put(Targets, new HashSet<MKVType>(Arrays.asList(new MKVType[]{TargetTypeValue, TargetType, TagTrackUID, TagEditionUID, TagChapterUID, TagAttachmentUID})));
          children.put(SimpleTag, new HashSet<MKVType>(Arrays.asList(new MKVType[]{TagName, TagLanguage, TagLanguageIETF, TagDefault, TagString, TagBinary})));
      }
      
      // TODO: this may be optimized of-course if it turns out to be a frequently called method
      public static MKVType getParent(MKVType t){
          for(Entry<MKVType, Set<MKVType>> ent : children.entrySet()){
              if (ent.getValue().contains(t))
                  return ent.getKey();
          }
          return null;
      }

    public static boolean possibleChild(EbmlMaster parent, EbmlBase child) {
        if (parent == null)
            if (child.type == EBML || child.type == Segment)
                return true;
            else
                return false;
        
        // Any unknown EbmlBase is assigned type of Void by parses, thus two different checks
        
        // 1. since Void/CRC32 can occur anywhere in the tree,
        //     look if they violate size-offset  contract of the parent.
        //     Violated size-offset contract implies the global EbmlBase actually belongs to parent
        if (Platform.arrayEqualsByte(child.id, Void.id) || Platform.arrayEqualsByte(child.id, CRC32.id))
            return !(child.offset == (parent.dataOffset+parent.dataLen));
        
        // 2. In case Void/CRC32 type is assigned, child EbmlBase is assumed as global,
        //    thus it can appear anywhere in the tree
        if (child.type == Void || child.type == CRC32)
            return true;
        
        Set<MKVType> candidates = children.get(parent.type);
        return candidates != null && candidates.contains(child.type);
    }
    
    public static boolean possibleChildById(EbmlMaster parent, byte[] typeId) {
        // Only EBML or Segment are allowed at top level
        if (parent == null && (Platform.arrayEqualsByte(EBML.id, typeId) || Platform.arrayEqualsByte(Segment.id, typeId)))
            return true;
        
        // Other EbmlBases at top level are not allowed
        if (parent == null)
            return false;
        
        // Void and CRC32 EbmlBases are global and are allowed everywhere in the hierarchy
        if (Platform.arrayEqualsByte(Void.id, typeId) || Platform.arrayEqualsByte(CRC32.id, typeId))
            return true;
        
        // for any other EbmlBase we have to check the spec
        for(MKVType aCandidate : children.get(parent.type))
            if (Platform.arrayEqualsByte(aCandidate.id, typeId))
                return true;
        
        return false;
    }
    
    public static EbmlBase findFirst(EbmlBase master, MKVType[] path){
        List<MKVType> tlist = new LinkedList<MKVType>(Arrays.asList(path));
        return findFirstSub(master, tlist);
    }

    
    @SuppressWarnings("unchecked")
    public static <T> T findFirstTree(List<? extends EbmlBase> tree, MKVType[] path){
        List<MKVType> tlist = new LinkedList<MKVType>(Arrays.asList(path));
        for(EbmlBase e : tree){
            EbmlBase z = findFirstSub(e, tlist);
            if (z != null)
                return (T) z;
        }
            
        return null;
    }


    private static EbmlBase findFirstSub(EbmlBase elem, List<MKVType> path) {
        if (path.size() == 0)
            return null;
        
        if (!elem.type.equals(path.get(0)))
            return null;
        
        if (path.size() == 1)
            return elem;
        
        MKVType head = path.remove(0);
        EbmlBase result = null;
        if (elem instanceof EbmlMaster) {
            Iterator<EbmlBase> iter = ((EbmlMaster) elem).children.iterator();
            while(iter.hasNext() && result == null) 
                result = findFirstSub(iter.next(), path);
            
        }
        
        path.add(0, head);
        return result;
        
    }

    public static <T> List<T> findList(List<? extends EbmlBase> tree, Class<T> class1, MKVType[] path) {
        List<T> result = new LinkedList<T>();
        List<MKVType> tlist = new LinkedList<MKVType>(Arrays.asList(path));
        if (tlist.size() > 0)
            for (EbmlBase node : tree) {
                MKVType head = tlist.remove(0);
                if (head == null || head.equals(node.type)) {
                    findSubList(node, tlist, result);
                }
                tlist.add(0, head);
            }

        return result;
    }
    
    @SuppressWarnings("unchecked")
    private static <T> void findSubList(EbmlBase element, List<MKVType> path, Collection<T> result) {
        if (path.size() > 0) {
            MKVType head = path.remove(0);
            if (element instanceof EbmlMaster) {
                EbmlMaster nb = (EbmlMaster) element;
                for (EbmlBase candidate : nb.children) {
                    if (head == null || head.equals(candidate.type)) {
                        findSubList(candidate, path, result);
                    }
                }
            }
            path.add(0, head);
        } else {
            result.add((T)element);
        }
    }
    
    @SuppressWarnings("unchecked")
    public static <T> T[] findAllTree(List<? extends EbmlBase> tree, Class<T> class1, MKVType[] path) {
        List<EbmlBase> result = new LinkedList<EbmlBase>();
        List<MKVType> tlist = new LinkedList<MKVType>(Arrays.asList(path));
        if (tlist.size() > 0)
            for (EbmlBase node : tree) {
                MKVType head = tlist.remove(0);
                if (head == null || head.equals(node.type)) {
                    findSub(node, tlist, result);
                }
                tlist.add(0, head);
            }

        return result.toArray((T[]) Array.newInstance(class1, 0));
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] findAll(EbmlBase master, Class<T> class1, boolean ga, MKVType[] path) {
        List<EbmlBase> result = new LinkedList<EbmlBase>();
        List<MKVType> tlist = new LinkedList<MKVType>(Arrays.asList(path));
        if (!master.type.equals(tlist.get(0)))
            return result.toArray((T[]) Array.newInstance(class1, 0));
        
        tlist.remove(0);
        findSub(master, tlist, result);
        return result.toArray((T[]) Array.newInstance(class1, 0));
    }


    private static void findSub(EbmlBase master, List<MKVType> path, Collection<EbmlBase> result) {
        if (path.size() > 0) {
            MKVType head = path.remove(0);
            if (master instanceof EbmlMaster) {
                EbmlMaster nb = (EbmlMaster) master;
                for (EbmlBase candidate : nb.children) {
                    if (head == null || head.equals(candidate.type)) {
                        findSub(candidate, path, result);
                    }
                }
            }
            path.add(0, head);
        } else {
            result.add(master);
        }
    }
    
}