package org.jcodec.containers.mkv;

import static java.lang.Long.toHexString;
import static org.jcodec.containers.mkv.util.EbmlUtil.toHexString;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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
public final static MKVType     Void = new MKVType(new byte[]{(byte)0xEC}, EbmlVoid.class);
public final static MKVType     CRC32 = new MKVType(new byte[]{(byte)0xBF}, EbmlBin.class);
public final static MKVType     EBML = new MKVType(new byte[]{0x1A, 0x45, (byte)0xDF, (byte)0xA3}, EbmlMaster.class);
public final static MKVType       EBMLVersion = new MKVType(new byte[]{0x42, (byte)0x86}, EbmlUint.class);
public final static MKVType       EBMLReadVersion = new MKVType(new byte[]{0x42, (byte)0xF7}, EbmlUint.class);
public final static MKVType       EBMLMaxIDLength = new MKVType(new byte[]{0x42, (byte)0xF2}, EbmlUint.class);
public final static MKVType       EBMLMaxSizeLength = new MKVType(new byte[]{0x42, (byte)0xF3}, EbmlUint.class);
      //All strings are UTF8 in java, this EbmlBase is specified as pure ASCII EbmlBase in Matroska spec
public final static MKVType       DocType = new MKVType(new byte[]{0x42, (byte)0x82}, EbmlString.class); 
public final static MKVType       DocTypeVersion = new MKVType(new byte[]{0x42, (byte)0x87}, EbmlUint.class);
public final static MKVType       DocTypeReadVersion = new MKVType(new byte[]{0x42, (byte)0x85}, EbmlUint.class);
public final static MKVType 
    Segment = new MKVType(MkvSegment.SEGMENT_ID, MkvSegment.class);
public final static MKVType       SeekHead = new MKVType(new byte[]{0x11, 0x4D, (byte)0x9B, 0x74}, EbmlMaster.class);
public final static MKVType         Seek = new MKVType(new byte[]{0x4D, (byte)0xBB}, EbmlMaster.class);
public final static MKVType           SeekID = new MKVType(new byte[]{0x53, (byte)0xAB}, EbmlBin.class);
public final static MKVType           SeekPosition = new MKVType(new byte[]{0x53, (byte)0xAC}, EbmlUint.class);
public final static MKVType       Info = new MKVType(new byte[]{0x15, (byte)0x49, (byte)0xA9, (byte)0x66}, EbmlMaster.class);
public final static MKVType         SegmentUID = new MKVType(new byte[]{0x73, (byte)0xA4}, EbmlBin.class);
        //All strings are UTF8 in java
public final static MKVType         SegmentFilename = new MKVType(new byte[]{0x73, (byte)0x84}, EbmlString.class); 
public final static MKVType         PrevUID = new MKVType(new byte[]{0x3C, (byte)0xB9, 0x23}, EbmlBin.class);
public final static MKVType         PrevFilename = new MKVType(new byte[]{0x3C, (byte)0x83, (byte)0xAB}, EbmlString.class);
public final static MKVType         NextUID = new MKVType(new byte[]{0x3E, (byte)0xB9, 0x23}, EbmlBin.class);
        //An escaped filename corresponding to the next segment.
public final static MKVType         NextFilenam = new MKVType(new byte[]{0x3E, (byte)0x83, (byte)0xBB}, EbmlString.class); 
        // A randomly generated unique ID that all segments related to each other must use (128 bits).
public final static MKVType         SegmentFamily = new MKVType(new byte[]{0x44,0x44}, EbmlBin.class); 
        // A tuple of corresponding ID used by chapter codecs to represent this segment.
public final static MKVType         ChapterTranslate = new MKVType(new byte[]{0x69,0x24}, EbmlMaster.class); 
            //Specify an edition UID on which this correspondance applies. When not specified, it means for all editions found in the segment.
public final static MKVType             ChapterTranslateEditionUID = new MKVType(new byte[]{0x69, (byte) 0xFC}, EbmlUint.class); 
            //The chapter codec using this ID (0: Matroska Script, 1: DVD-menu).
public final static MKVType             ChapterTranslateCodec = new MKVType(new byte[]{0x69, (byte) 0xBF}, EbmlUint.class); 
            // The binary value used to represent this segment in the chapter codec data. The format depends on the ChapProcessCodecID used.
public final static MKVType             ChapterTranslateID = new MKVType(new byte[]{0x69, (byte)0xA5}, EbmlBin.class); 
        // Timecode scale in nanoseconds (1.000.000 means all timecodes in the segment are expressed in milliseconds).
        // Every timecode of a block (cluster timecode + block timecode ) is multiplied by this value to obtain real timecode of a block
public final static MKVType         TimecodeScale = new MKVType(new byte[]{0x2A, (byte)0xD7, (byte)0xB1}, EbmlUint.class);
public final static MKVType         
        Duration = new MKVType(new byte[]{0x44, (byte)0x89}, EbmlFloat.class);
public final static MKVType         DateUTC = new MKVType(new byte[]{0x44, (byte)0x61}, EbmlDate.class);
public final static MKVType         Title = new MKVType(new byte[]{0x7B, (byte)0xA9}, EbmlString.class);
public final static MKVType         MuxingApp = new MKVType(new byte[]{0x4D, (byte)0x80}, EbmlString.class);
public final static MKVType         WritingApp = new MKVType(new byte[]{0x57,       0x41}, EbmlString.class);
       
     //The lower level EbmlBase containing the (monolithic) Block structure.
public final static MKVType      Cluster = new MKVType(new byte[]{0x1F, (byte)0x43, (byte)0xB6, (byte)0x75}, EbmlMaster.class); 
         //Absolute timecode of the cluster (based on TimecodeScale).
public final static MKVType          Timecode = new MKVType(new byte[]{(byte)0xE7}, EbmlUint.class); 
         //  The list of tracks that are not used in that part of the stream. It is useful when using overlay tracks on seeking. Then you should decide what track to use.
public final static MKVType          SilentTracks = new MKVType(new byte[]{0x58, 0x54}, EbmlMaster.class); 
             //  One of the track number that are not used from now on in the stream. It could change later if not specified as silent in a further Cluster
public final static MKVType              SilentTrackNumber = new MKVType(new byte[]{0x58, (byte)0xD7}, EbmlUint.class); 
         //  The Position of the Cluster in the segment (0 in live broadcast streams). It might help to resynchronise offset on damaged streams.
public final static MKVType          Position = new MKVType(new byte[]{(byte)0xA7}, EbmlUint.class);  
         // Size of the previous Cluster, in octets. Can be useful for backward playing.
public final static MKVType          PrevSize = new MKVType(new byte[]{(byte)0xAB}, EbmlUint.class);  
         //Similar to Block but without all the extra information, mostly used to reduced overhead when no extra feature is needed. (see SimpleBlock Structure)
public final static MKVType          SimpleBlock = new MKVType(MkvBlock.SIMPLEBLOCK_ID, MkvBlock.class); 
         //Basic container of information containing a single Block or BlockVirtual, and information specific to that Block/VirtualBlock.
public final static MKVType          BlockGroup = new MKVType(new byte[]{(byte)0xA0}, EbmlMaster.class); 
           // Block containing the actual data to be rendered and a timecode relative to the Cluster Timecode. (see Block Structure)
public final static MKVType            Block = new MKVType(MkvBlock.BLOCK_ID, MkvBlock.class); 
           // Contain additional blocks to complete the main one. An EBML parser that has no knowledge of the Block structure could still see and use/skip these data.
public final static MKVType            BlockAdditions = new MKVType(new byte[]{0x75, (byte) 0xA1}, EbmlMaster.class); 
               //  Contain the BlockAdditional and some parameters.
public final static MKVType                BlockMore = new MKVType(new byte[]{(byte)0xA6}, EbmlMaster.class); 
                   //  An ID to identify the BlockAdditional level.
public final static MKVType                    BlockAddID = new MKVType(new byte[]{(byte)0xEE}, EbmlUint.class); 
                   // Interpreted by the codec as it wishes (using the BlockAddID).
public final static MKVType                    BlockAdditional = new MKVType(new byte[]{(byte) 0xA5}, EbmlBin.class); 
          /**
           * The duration of the Block (based on TimecodeScale). 
           * This EbmlBase is mandatory when DefaultDuration is set for the track (but can be omitted as other default values). 
           * When not written and with no DefaultDuration, the value is assumed to be the difference between the timecode 
           * of this Block and the timecode of the next Block in "display" order (not coding order). 
           * This EbmlBase can be useful at the end of a Track (as there is not other Block available); 
           * or when there is a break in a track like for subtitle tracks. 
           * When set to 0 that means the frame is not a keyframe.
           */
public final static MKVType            BlockDuration = new MKVType(new byte[]{(byte)0x9B}, EbmlUint.class);
           // This frame is referenced and has the specified cache priority. In cache only a frame of the same or higher priority can replace this frame. A value of 0 means the frame is not referenced
public final static MKVType            ReferencePriority = new MKVType(new byte[]{(byte) 0xFA}, EbmlUint.class);  
           //Timecode of another frame used as a reference (ie: B or P frame). The timecode is relative to the block it's attached to.
public final static MKVType            ReferenceBlock = new MKVType(new byte[]{(byte)0xFB}, EbmlSint.class); 
           //  The new codec state to use. Data interpretation is private to the codec. This information should always be referenced by a seek entry.
public final static MKVType            CodecState = new MKVType(new byte[]{(byte)0xA4}, EbmlBin.class); 
           //  Contains slices description.
public final static MKVType            Slices = new MKVType(new byte[]{(byte)0x8E}, EbmlMaster.class);
               //  Contains extra time information about the data contained in the Block. While there are a few files in the wild with this EbmlBase, it is no longer in use and has been deprecated. Being able to interpret this EbmlBase is not required for playback.
public final static MKVType                TimeSlice = new MKVType(new byte[]{(byte)0xE8}, EbmlMaster.class); 
                   //  The reverse number of the frame in the lace (0 is the last frame, 1 is the next to last, etc). While there are a few files in the wild with this EbmlBase, it is no longer in use and has been deprecated. Being able to interpret this EbmlBase is not required for playback.
public final static MKVType                    LaceNumber = new MKVType(new byte[]{(byte)0xCC}, EbmlUint.class); 
          
       //A top-level block of information with many tracks described.
public final static MKVType       Tracks = new MKVType(new byte[]{0x16, (byte)0x54, (byte)0xAE, (byte)0x6B}, EbmlMaster.class); 
        //Describes a track with all EbmlBases.
public final static MKVType         TrackEntry = new MKVType(new byte[]{(byte)0xAE}, EbmlMaster.class); 
          //The track number as used in the Block Header (using more than 127 tracks is not encouraged, though the design allows an unlimited number).
public final static MKVType           TrackNumber = new MKVType(new byte[]{(byte)0xD7}, EbmlUint.class); 
          //A unique ID to identify the Track. This should be kept the same when making a direct stream copy of the Track to another file.
public final static MKVType           TrackUID = new MKVType(new byte[]{0x73, (byte)0xC5}, EbmlUint.class); 
          //A set of track types coded on 8 bits (1: video, 2: audio, 3: complex, 0x10: logo, 0x11: subtitle, 0x12: buttons, 0x20: control).
public final static MKVType           TrackType = new MKVType(new byte[]{(byte)0x83}, EbmlUint.class); 
          //  Set if the track is usable. (1 bit)
public final static MKVType           FlagEnabled = new MKVType(new byte[]{(byte)0xB9}, EbmlUint.class); 
          //  Set if that track (audio, video or subs) SHOULD be active if no language found matches the user preference. (1 bit)
public final static MKVType           FlagDefault = new MKVType(new byte[]{(byte)0x88}, EbmlUint.class); 
          //   Set if that track MUST be active during playback. There can be many forced track for a kind (audio, video or subs); the player should select the one which language matches the user preference or the default + forced track. Overlay MAY happen between a forced and non-forced track of the same kind. (1 bit)
public final static MKVType           FlagForced = new MKVType(new byte[]{0x55,(byte)0xAA}, EbmlUint.class); 
          //   Set if the track may contain blocks using lacing. (1 bit)
public final static MKVType           FlagLacing = new MKVType(new byte[]{(byte)0x9C}, EbmlUint.class); 
          //  The minimum number of frames a player should be able to cache during playback. If set to 0, the reference pseudo-cache system is not used.
public final static MKVType           MinCache = new MKVType(new byte[]{0x6D,(byte)0xE7}, EbmlUint.class); 
          //  The maximum cache size required to store referenced frames in and the current frame. 0 means no cache is needed.
public final static MKVType           MaxCache = new MKVType(new byte[]{0x6D,(byte)0xF8}, EbmlUint.class); 
          //Number of nanoseconds (not scaled via TimecodeScale) per frame ('frame' in the Matroska sense -- one EbmlBase put into a (Simple)Block).
public final static MKVType           DefaultDuration = new MKVType(new byte[]{0x23, (byte)0xE3, (byte)0x83}, EbmlUint.class); 
          // The maximum value of BlockAddID. A value 0 means there is no BlockAdditions for this track.
public final static MKVType           MaxBlockAdditionID = new MKVType(new byte[]{0x55, (byte)0xEE}, EbmlUint.class); 
          //A human-readable track name.
public final static MKVType           Name = new MKVType(new byte[]{0x53, 0x6E}, EbmlString.class); 
          // Specifies the language of the track in the Matroska languages form.
public final static MKVType           Language = new MKVType(new byte[]{0x22, (byte)0xB5, (byte)0x9C}, EbmlString.class); 
          // An ID corresponding to the codec, see the codec page for more info.
public final static MKVType           CodecID = new MKVType(new byte[]{(byte)0x86}, EbmlString.class); 
          //Private data only known to the codec.
public final static MKVType           CodecPrivate = new MKVType(new byte[]{(byte)0x63, (byte)0xA2}, EbmlBin.class); 
          //  A human-readable string specifying the codec.
public final static MKVType           CodecName = new MKVType(new byte[]{(byte)0x25,(byte)0x86,(byte)0x88}, EbmlString.class); 
          //  The UID of an attachment that is used by this codec.
public final static MKVType           AttachmentLink = new MKVType(new byte[]{0x74, 0x46}, EbmlUint.class); 
          // The codec can decode potentially damaged data (1 bit).
public final static MKVType           CodecDecodeAll = new MKVType(new byte[]{(byte)0xAA}, EbmlUint.class); 
          //  Specify that this track is an overlay track for the Track specified (in the u-integer). That means when this track has a gap (see SilentTracks) the overlay track should be used instead. The order of multiple TrackOverlay matters, the first one is the one that should be used. If not found it should be the second, etc.
public final static MKVType           TrackOverlay = new MKVType(new byte[]{0x6F,(byte)0xAB}, EbmlUint.class); 
          // The track identification for the given Chapter Codec.
public final static MKVType           TrackTranslate = new MKVType(new byte[]{0x66, 0x24}, EbmlMaster.class); 
              //  Specify an edition UID on which this translation applies. When not specified, it means for all editions found in the segment.
public final static MKVType               TrackTranslateEditionUID = new MKVType(new byte[]{0x66,(byte)0xFC}, EbmlUint.class); 
              //  The chapter codec using this ID (0: Matroska Script, 1: DVD-menu).
public final static MKVType               TrackTranslateCodec = new MKVType(new byte[]{0x66,(byte)0xBF}, EbmlUint.class); 
              // The binary value used to represent this track in the chapter codec data. The format depends on the ChapProcessCodecID used.
public final static MKVType               TrackTranslateTrackID = new MKVType(new byte[]{0x66,(byte)0xA5}, EbmlBin.class); 
public final static MKVType           Video = new MKVType(new byte[]{(byte)0xE0}, EbmlMaster.class);
              // Set if the video is interlaced. (1 bit)
public final static MKVType               FlagInterlaced = new MKVType(new byte[]{(byte)0x9A}, EbmlUint.class); 
              // Stereo-3D video mode (0: mono, 1: side by side (left eye is first); 2: top-bottom (right eye is first); 3: top-bottom (left eye is first); 4: checkboard (right is first); 5: checkboard (left is first); 6: row interleaved (right is first); 7: row interleaved (left is first); 8: column interleaved (right is first); 9: column interleaved (left is first); 10: anaglyph (cyan/red); 11: side by side (right eye is first); 12: anaglyph (green/magenta); 13 both eyes laced in one Block (left eye is first); 14 both eyes laced in one Block (right eye is first)) . There are some more details on 3D support in the Specification Notes.
public final static MKVType               StereoMode = new MKVType(new byte[]{0x53,(byte)0xB8}, EbmlUint.class); 
              // Alpha Video Mode. Presence of this EbmlBase indicates that the BlockAdditional EbmlBase could contain Alpha data.
public final static MKVType               AlphaMode = new MKVType(new byte[]{0x53,(byte)0xC0}, EbmlUint.class); 
public final static MKVType               PixelWidth = new MKVType(new byte[]{(byte)0xB0}, EbmlUint.class);
public final static MKVType               PixelHeight = new MKVType(new byte[]{(byte)0xBA}, EbmlUint.class);
              //  The number of video pixels to remove at the bottom of the image (for HDTV content).
public final static MKVType               PixelCropBottom = new MKVType(new byte[]{0x54,(byte)0xAA}, EbmlUint.class);   
              // The number of video pixels to remove at the top of the image.
public final static MKVType               PixelCropTop = new MKVType(new byte[]{0x54,(byte)0xBB}, EbmlUint.class);  
              // The number of video pixels to remove on the left of the image.
public final static MKVType               PixelCropLeft = new MKVType(new byte[]{0x54,(byte)0xCC}, EbmlUint.class);  
              //  The number of video pixels to remove on the right of the image.
public final static MKVType               PixelCropRight = new MKVType(new byte[]{0x54,(byte)0xDD}, EbmlUint.class);  
public final static MKVType               DisplayWidth = new MKVType(new byte[]{0x54, (byte)0xB0}, EbmlUint.class);
public final static MKVType               DisplayHeight = new MKVType(new byte[]{0x54, (byte)0xBA}, EbmlUint.class);
              //  How DisplayWidth & DisplayHeight should be interpreted (0: pixels, 1: centimeters, 2: inches, 3: Display Aspect Ratio).
public final static MKVType               DisplayUnit = new MKVType(new byte[]{0x54,(byte)0xB2}, EbmlUint.class); 
              //  Specify the possible modifications to the aspect ratio (0: free resizing, 1: keep aspect ratio, 2: fixed).
public final static MKVType               AspectRatioType = new MKVType(new byte[]{0x54,(byte)0xB3}, EbmlUint.class); 
              //  Same value as in AVI (32 bits).
public final static MKVType               ColourSpace = new MKVType(new byte[]{0x2E, (byte)0xB5,0x24}, EbmlBin.class); 
public final static MKVType           Audio = new MKVType(new byte[]{(byte)0xE1}, EbmlMaster.class);
public final static MKVType               SamplingFrequency = new MKVType(new byte[]{(byte)0xB5}, EbmlFloat.class);
public final static MKVType               OutputSamplingFrequency = new MKVType(new byte[]{0x78, (byte)0xB5}, EbmlFloat.class);
public final static MKVType               Channels = new MKVType(new byte[]{(byte)0x9F}, EbmlUint.class);
public final static MKVType               BitDepth = new MKVType(new byte[]{0x62, 0x64}, EbmlUint.class);
          //  Operation that needs to be applied on tracks to create this virtual track. For more details look at the Specification Notes on the subject.
public final static MKVType           TrackOperation = new MKVType(new byte[]{(byte)0xE2}, EbmlMaster.class);  
              // Contains the list of all video plane tracks that need to be combined to create this 3D track
public final static MKVType               TrackCombinePlanes = new MKVType(new byte[]{(byte)0xE3}, EbmlMaster.class); 
                  //  Contains a video plane track that need to be combined to create this 3D track
public final static MKVType                   TrackPlane = new MKVType(new byte[]{(byte)0xE4}, EbmlMaster.class); 
                      //  The trackUID number of the track representing the plane.
public final static MKVType                       TrackPlaneUID = new MKVType(new byte[]{(byte)0xE5}, EbmlUint.class); 
                      //  The kind of plane this track corresponds to (0: left eye, 1: right eye, 2: background).
public final static MKVType                       TrackPlaneType = new MKVType(new byte[]{(byte)0xE6}, EbmlUint.class); 
               // Contains the list of all tracks whose Blocks need to be combined to create this virtual track
public final static MKVType               TrackJoinBlocks = new MKVType(new byte[]{(byte)0xE9}, EbmlMaster.class); 
                  // The trackUID number of a track whose blocks are used to create this virtual track.
public final static MKVType                   TrackJoinUID = new MKVType(new byte[]{(byte)0xED}, EbmlUint.class); 
          // Settings for several content encoding mechanisms like compression or encryption.
public final static MKVType           ContentEncodings = new MKVType(new byte[]{0x6D, (byte)0x80}, EbmlMaster.class); 
              // Settings for one content encoding like compression or encryption.
public final static MKVType               ContentEncoding = new MKVType(new byte[]{0x62, 0x40}, EbmlMaster.class); 
                  // Tells when this modification was used during encoding/muxing starting with 0 and counting upwards. The decoder/demuxer has to start with the highest order number it finds and work its way down. This value has to be unique over all ContentEncodingOrder EbmlBases in the segment.
public final static MKVType                   ContentEncodingOrder = new MKVType(new byte[]{0x50, 0x31}, EbmlUint.class); 
                  //  A bit field that describes which EbmlBases have been modified in this way. Values (big endian) can be OR'ed. Possible values: 1 - all frame contents, 2 - the track's private data, 4 - the next ContentEncoding (next ContentEncodingOrder. Either the data inside ContentCompression and/or ContentEncryption)
public final static MKVType                   ContentEncodingScope = new MKVType(new byte[]{0x50, 0x32}, EbmlUint.class); 
                  //   A value describing what kind of transformation has been done. Possible values: 0 - compression, 1 - encryption
public final static MKVType                   ContentEncodingType = new MKVType(new byte[]{0x50, 0x33}, EbmlUint.class); 
                  // Settings describing the compression used. Must be present if the value of ContentEncodingType is 0 and absent otherwise. Each block must be decompressable even if no previous block is available in order not to prevent seeking.
public final static MKVType                   ContentCompression = new MKVType(new byte[]{0x50, 0x34}, EbmlMaster.class); 
                  //  The compression algorithm used. Algorithms that have been specified so far are: 0 - zlib, 1 - bzlib, 2 - lzo1x, 3 - Header Stripping
public final static MKVType                       ContentCompAlgo = new MKVType(new byte[]{0x42, (byte)0x54}, EbmlUint.class); 
                      //  Settings that might be needed by the decompressor. For Header Stripping (ContentCompAlgo=3); the bytes that were removed from the beggining of each frames of the track.
public final static MKVType                       ContentCompSettings = new MKVType(new byte[]{0x42, 0x55}, EbmlBin.class); 
                      //  Settings describing the encryption used. Must be present if the value of ContentEncodingType is 1 and absent otherwise.
public final static MKVType                   ContentEncryption = new MKVType(new byte[]{0x50, 0x35}, EbmlMaster.class); 
                  //  The encryption algorithm used. The value '0' means that the contents have not been encrypted but only signed. Predefined values: 1 - DES, 2 - 3DES, 3 - Twofish, 4 - Blowfish, 5 - AES
public final static MKVType                       ContentEncAlgo = new MKVType(new byte[]{0x47, (byte)0xE1}, EbmlUint.class); 
                      // For public key algorithms this is the ID of the public key the the data was encrypted with.
public final static MKVType                       ContentEncKeyID = new MKVType(new byte[]{0x47, (byte)0xE2}, EbmlBin.class); 
                      //  A cryptographic signature of the contents.
public final static MKVType                       ContentSignature = new MKVType(new byte[]{0x47, (byte)0xE3}, EbmlBin.class); 
                      //  This is the ID of the private key the data was signed with.
public final static MKVType                       ContentSigKeyID = new MKVType(new byte[]{0x47, (byte)0xE4}, EbmlBin.class); 
                      // The algorithm used for the signature. A value of '0' means that the contents have not been signed but only encrypted. Predefined values: 1 - RSA
public final static MKVType                       ContentSigAlgo = new MKVType(new byte[]{0x47, (byte)0xE5}, EbmlUint.class); 
                      // The hash algorithm used for the signature. A value of '0' means that the contents have not been signed but only encrypted. Predefined values: 1 - SHA1-160, 2 - MD5
public final static MKVType                       ContentSigHashAlgo = new MKVType(new byte[]{0x47, (byte)0xE6}, EbmlUint.class); 
public final static MKVType                     
        Cues = new MKVType(new byte[]{0x1C,0x53,(byte)0xBB,0x6B}, EbmlMaster.class);
public final static MKVType            CuePoint = new MKVType(new byte[]{(byte)0xBB}, EbmlMaster.class);
public final static MKVType                CueTime = new MKVType(new byte[]{(byte)0xB3}, EbmlUint.class);
public final static MKVType                CueTrackPositions = new MKVType(new byte[]{(byte)0xB7}, EbmlMaster.class);
public final static MKVType                    CueTrack = new MKVType(new byte[]{(byte)0xF7}, EbmlUint.class);
public final static MKVType                    CueClusterPosition = new MKVType(new byte[]{(byte)0xF1}, EbmlUint.class);
                   // The relative position of the referenced block inside the cluster with 0 being the first possible position for an EbmlBase inside that cluster.
public final static MKVType                    CueRelativePosition = new MKVType(new byte[]{(byte)0xF0}, EbmlUint.class); 
                   // The duration of the block according to the segment time base. If missing the track's DefaultDuration does not apply and no duration information is available in terms of the cues.
public final static MKVType                    CueDuration = new MKVType(new byte[]{(byte)0xB2}, EbmlUint.class); 
                   //Number of the Block in the specified Cluster.
public final static MKVType                    CueBlockNumber = new MKVType(new byte[]{0x53, 0x78}, EbmlUint.class); 
                   // The position of the Codec State corresponding to this Cue EbmlBase. 0 means that the data is taken from the initial Track Entry.
public final static MKVType                    CueCodecState = new MKVType(new byte[]{(byte)0xEA}, EbmlUint.class); 
                   // The Clusters containing the required referenced Blocks.
public final static MKVType                    CueReference = new MKVType(new byte[]{(byte)0xDB}, EbmlMaster.class); 
                   // Timecode of the referenced Block.
public final static MKVType                        CueRefTime = new MKVType(new byte[]{(byte)0x96}, EbmlUint.class);
public final static MKVType 
      Attachments = new MKVType(new byte[]{0x19, 0x41, (byte)0xA4, 0x69}, EbmlMaster.class);
public final static MKVType         AttachedFile = new MKVType(new byte[]{0x61, (byte)0xA7}, EbmlMaster.class);
public final static MKVType           FileDescription = new MKVType(new byte[]{0x46, (byte)0x7E}, EbmlString.class);
public final static MKVType           FileName = new MKVType(new byte[]{0x46, (byte)0x6E}, EbmlString.class);
public final static MKVType           FileMimeType = new MKVType(new byte[]{0x46, (byte)0x60}, EbmlString.class);
public final static MKVType           FileData = new MKVType(new byte[]{0x46, (byte)0x5C}, EbmlBin.class);
public final static MKVType           FileUID = new MKVType(new byte[]{0x46, (byte)0xAE}, EbmlUint.class);
public final static MKVType 
     Chapters = new MKVType(new byte[]{0x10, (byte)0x43, (byte)0xA7, (byte)0x70}, EbmlMaster.class);
public final static MKVType       EditionEntry = new MKVType(new byte[]{(byte)0x45, (byte)0xB9}, EbmlMaster.class);
public final static MKVType         EditionUID = new MKVType(new byte[]{(byte)0x45, (byte)0xBC}, EbmlUint.class);
public final static MKVType         EditionFlagHidden = new MKVType(new byte[]{(byte)0x45, (byte)0xBD}, EbmlUint.class);
public final static MKVType         EditionFlagDefault = new MKVType(new byte[]{(byte)0x45, (byte)0xDB}, EbmlUint.class);
public final static MKVType         EditionFlagOrdered = new MKVType(new byte[]{(byte)0x45, (byte)0xDD}, EbmlUint.class);
public final static MKVType         ChapterAtom = new MKVType(new byte[]{(byte)0xB6}, EbmlMaster.class);
public final static MKVType           ChapterUID = new MKVType(new byte[]{(byte)0x73, (byte)0xC4}, EbmlUint.class);
            //  A unique string ID to identify the Chapter. Use for WebVTT cue identifier storage.
public final static MKVType           ChapterStringUID = new MKVType(new byte[]{0x56,0x54}, EbmlString.class); 
public final static MKVType           ChapterTimeStart = new MKVType(new byte[]{(byte)0x91}, EbmlUint.class);
public final static MKVType           ChapterTimeEnd = new MKVType(new byte[]{(byte)0x92}, EbmlUint.class);
public final static MKVType           ChapterFlagHidden = new MKVType(new byte[]{(byte)0x98}, EbmlUint.class);
public final static MKVType           ChapterFlagEnabled = new MKVType(new byte[]{(byte)0x45, (byte)0x98}, EbmlUint.class);
          //  A segment to play in place of this chapter. Edition ChapterSegmentEditionUID should be used for this segment, otherwise no edition is used.
public final static MKVType           ChapterSegmentUID = new MKVType(new byte[]{0x6E,0x67}, EbmlBin.class); 
          //  The EditionUID to play from the segment linked in ChapterSegmentUID.
public final static MKVType           ChapterSegmentEditionUID = new MKVType(new byte[]{0x6E,(byte)0xBC}, EbmlUint.class); 
public final static MKVType           ChapterPhysicalEquiv = new MKVType(new byte[]{(byte)0x63, (byte)0xC3}, EbmlUint.class);
public final static MKVType           ChapterTrack = new MKVType(new byte[]{(byte)0x8F}, EbmlMaster.class);
public final static MKVType             ChapterTrackNumber = new MKVType(new byte[]{(byte)0x89}, EbmlUint.class);
public final static MKVType           ChapterDisplay = new MKVType(new byte[]{(byte)0x80}, EbmlMaster.class);
public final static MKVType             ChapString = new MKVType(new byte[]{(byte)0x85}, EbmlString.class);
public final static MKVType             ChapLanguage = new MKVType(new byte[]{(byte)0x43, (byte)0x7C}, EbmlString.class);
public final static MKVType             ChapCountry = new MKVType(new byte[]{(byte)0x43, (byte)0x7E}, EbmlString.class);
            // Contains all the commands associated to the Atom.
public final static MKVType           ChapProcess = new MKVType(new byte[]{0x69, 0x44}, EbmlMaster.class); 
          // Contains the type of the codec used for the processing. A value of 0 means native Matroska processing (to be defined); a value of 1 means the DVD command set is used. More codec IDs can be added later.
public final static MKVType             ChapProcessCodecID = new MKVType(new byte[]{0x69, 0x55}, EbmlUint.class); 
            // Some optional data attached to the ChapProcessCodecID information. For ChapProcessCodecID = 1, it is the "DVD level" equivalent.
public final static MKVType             ChapProcessPrivate = new MKVType(new byte[]{0x45, 0x0D}, EbmlBin.class); 
            // Contains all the commands associated to the Atom.
public final static MKVType             ChapProcessCommand = new MKVType(new byte[]{0x69, 0x11}, EbmlMaster.class); 
            // Defines when the process command should be handled (0: during the whole chapter, 1: before starting playback, 2: after playback of the chapter).
public final static MKVType               ChapProcessTime = new MKVType(new byte[]{0x69, 0x22}, EbmlUint.class); 
              // Contains the command information. The data should be interpreted depending on the ChapProcessCodecID value. For ChapProcessCodecID = 1, the data correspond to the binary DVD cell pre/post commands.
public final static MKVType               ChapProcessData = new MKVType(new byte[]{0x69, 0x33}, EbmlBin.class); 
public final static MKVType               
      Tags = new MKVType(new byte[]{0x12, (byte)0x54, (byte)0xC3, (byte)0x67}, EbmlMaster.class);
public final static MKVType           Tag = new MKVType(new byte[]{0x73, (byte)0x73}, EbmlMaster.class);
public final static MKVType               Targets = new MKVType(new byte[]{0x63, (byte)0xC0}, EbmlMaster.class);
public final static MKVType                   TargetTypeValue = new MKVType(new byte[]{0x68, (byte) 0xCA}, EbmlUint.class);
public final static MKVType                   TargetType = new MKVType(new byte[]{0x63,(byte)0xCA}, EbmlString.class);
public final static MKVType                   TagTrackUID = new MKVType(new byte[]{0x63, (byte)0xC5}, EbmlUint.class);
                  //  A unique ID to identify the EditionEntry(s) the tags belong to. If the value is 0 at this level, the tags apply to all editions in the Segment.
public final static MKVType                   TagEditionUID = new MKVType(new byte[]{0x63,(byte)0xC9}, EbmlUint.class); 
public final static MKVType                   TagChapterUID = new MKVType(new byte[]{0x63, (byte)0xC4}, EbmlUint.class);
public final static MKVType                   TagAttachmentUID = new MKVType(new byte[]{0x63, (byte)0xC6}, EbmlUint.class);
public final static MKVType               SimpleTag = new MKVType(new byte[]{0x67, (byte)0xC8}, EbmlMaster.class);
public final static MKVType                   TagName = new MKVType(new byte[]{0x45, (byte)0xA3}, EbmlString.class);
public final static MKVType                   TagLanguage = new MKVType(new byte[]{0x44, 0x7A}, EbmlString.class);
public final static MKVType                   TagDefault = new MKVType(new byte[]{0x44, (byte)0x84}, EbmlUint.class);
public final static MKVType                   TagString = new MKVType(new byte[]{0x44, (byte)0x87}, EbmlString.class);
public final static MKVType                   TagBinary = new MKVType(new byte[]{0x44, (byte)0x85}, EbmlBin.class);
      
      static public MKVType[] firstLevelHeaders = {SeekHead, Info, Cluster, Tracks, Cues, Attachments, Chapters, Tags, EBMLVersion, EBMLReadVersion, EBMLMaxIDLength, EBMLMaxSizeLength, DocType, DocTypeVersion, DocTypeReadVersion };
      public final byte [] id;
      public final Class<? extends EbmlBase> clazz;

    private MKVType(byte[] id, Class<? extends EbmlBase> clazz) {
        this.id = id;
        this.clazz = clazz;
        _values.add(this);
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
                + toHexString(offset).toUpperCase());
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
          children.put(Info, new HashSet<MKVType>(Arrays.asList(new MKVType[]{SegmentUID, SegmentFilename, PrevUID, PrevFilename, NextUID, NextFilenam, SegmentFamily, ChapterTranslate, TimecodeScale, Duration , DateUTC, Title, MuxingApp, WritingApp})));
          children.put(ChapterTranslate, new HashSet<MKVType>(Arrays.asList(new MKVType[]{ChapterTranslateEditionUID, ChapterTranslateCodec, ChapterTranslateID})));
          
          children.put(Cluster, new HashSet<MKVType>(Arrays.asList(new MKVType[]{Timecode, SilentTracks, Position, PrevSize, SimpleBlock, BlockGroup})));
          children.put(SilentTracks, new HashSet<MKVType>(Arrays.asList(new MKVType[]{SilentTrackNumber})));
          children.put(BlockGroup, new HashSet<MKVType>(Arrays.asList(new MKVType[]{Block, BlockAdditions, BlockDuration, ReferencePriority, ReferenceBlock, CodecState, Slices})));
          children.put(BlockAdditions, new HashSet<MKVType>(Arrays.asList(new MKVType[]{BlockMore})));
          children.put(BlockMore, new HashSet<MKVType>(Arrays.asList(new MKVType[]{BlockAddID, BlockAdditional})));
          children.put(Slices, new HashSet<MKVType>(Arrays.asList(new MKVType[]{TimeSlice})));
          children.put(TimeSlice, new HashSet<MKVType>(Arrays.asList(new MKVType[]{LaceNumber})));

          children.put(Tracks, new HashSet<MKVType>(Arrays.asList(new MKVType[]{TrackEntry})));
          children.put(TrackEntry, new HashSet<MKVType>(Arrays.asList(new MKVType[]{TrackNumber, TrackUID, TrackType, TrackType, FlagDefault, FlagForced, FlagLacing, MinCache, MaxCache, DefaultDuration, MaxBlockAdditionID, Name, Language, CodecID, CodecPrivate, CodecName, AttachmentLink, CodecDecodeAll, TrackOverlay, TrackTranslate, Video, Audio, TrackOperation, ContentEncodings})));
          children.put(TrackTranslate, new HashSet<MKVType>(Arrays.asList(new MKVType[]{TrackTranslateEditionUID, TrackTranslateCodec, TrackTranslateTrackID})));
          children.put(Video, new HashSet<MKVType>(Arrays.asList(new MKVType[]{FlagInterlaced, StereoMode, AlphaMode, PixelWidth, PixelHeight, PixelCropBottom, PixelCropTop, PixelCropLeft, PixelCropRight, DisplayWidth, DisplayHeight, DisplayUnit, AspectRatioType, ColourSpace})));
          children.put(Audio, new HashSet<MKVType>(Arrays.asList(new MKVType[]{SamplingFrequency, OutputSamplingFrequency, Channels, BitDepth})));
          children.put(TrackOperation, new HashSet<MKVType>(Arrays.asList(new MKVType[]{TrackCombinePlanes, TrackJoinBlocks})));
          children.put(TrackCombinePlanes, new HashSet<MKVType>(Arrays.asList(new MKVType[]{TrackPlane})));
          children.put(TrackPlane, new HashSet<MKVType>(Arrays.asList(new MKVType[]{TrackPlaneUID, TrackPlaneType})));
          children.put(TrackJoinBlocks, new HashSet<MKVType>(Arrays.asList(new MKVType[]{TrackJoinUID})));
          children.put(ContentEncodings, new HashSet<MKVType>(Arrays.asList(new MKVType[]{ContentEncoding})));
          children.put(ContentEncoding, new HashSet<MKVType>(Arrays.asList(new MKVType[]{ContentEncodingOrder, ContentEncodingScope, ContentEncodingType, ContentCompression, ContentEncryption})));
          children.put(ContentCompression, new HashSet<MKVType>(Arrays.asList(new MKVType[]{ContentCompAlgo, ContentCompSettings})));
          children.put(ContentEncryption, new HashSet<MKVType>(Arrays.asList(new MKVType[]{ContentEncAlgo, ContentEncKeyID, ContentSignature, ContentSigKeyID, ContentSigAlgo, ContentSigHashAlgo})));
          
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
          children.put(ChapterDisplay, new HashSet<MKVType>(Arrays.asList(new MKVType[]{ChapString, ChapLanguage, ChapCountry})));
          children.put(ChapProcess, new HashSet<MKVType>(Arrays.asList(new MKVType[]{ChapProcessCodecID, ChapProcessPrivate, ChapProcessCommand})));
          children.put(ChapProcessCommand, new HashSet<MKVType>(Arrays.asList(new MKVType[]{ChapProcessTime, ChapProcessData})));
          
          children.put(Tags, new HashSet<MKVType>(Arrays.asList(new MKVType[]{Tag})));
          children.put(Tag, new HashSet<MKVType>(Arrays.asList(new MKVType[]{Targets, SimpleTag})));
          children.put(Targets, new HashSet<MKVType>(Arrays.asList(new MKVType[]{TargetTypeValue, TargetType, TagTrackUID, TagEditionUID, TagChapterUID, TagAttachmentUID})));
          children.put(SimpleTag, new HashSet<MKVType>(Arrays.asList(new MKVType[]{TagName, TagLanguage, TagDefault, TagString, TagBinary})));
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