package org.jcodec.containers.matroska;

import java.util.*;
import org.jcodec.containers.matroska.ebml.*;

/**
 * This class is part of JCodec ( www.jcodec.org )
 * This software is distributed under FreeBSD License
 * 
 * Defines the tags we will see in a Matroska documents.
 *
 * Provides some static methods to identify tags from
 * their IDs.
 * 
 * Based on work by Matroska.org and written by
 * John Cannon (c) 2002 <spyder@matroska.org>
 * Jory Stone  (c) 2004 <jebml@jory.info>
 *
 * @author George Gardiner <this.george@googlemail.com>
 *
 */
 
public class MatroskaDocument
{
	private static TagType[] matroska =
	{
		new TagType("VirtualRoot",						TagType.TYPE_ROOT,			new byte[]	{(byte)0xFF}								),
		new TagType("Void", 							TagType.TYPE_BINARY, 		new byte[]	{(byte)0xEC}								),
		new TagType("EBMLHeader",	 					TagType.TYPE_ROOT,			new byte[]	{0x1A, 0x45, (byte)0xDF, (byte)0xA3}		),
		new TagType("EBMLVersion", 						TagType.TYPE_UINTEGER, 		new byte[]	{0x42, (byte)0x86}							),		
		new TagType("DocTypeReadVersion",				TagType.TYPE_UINTEGER, 		new byte[]	{0x42, (byte)0x85}							),		
		new TagType("EBMLReadVersion", 					TagType.TYPE_UINTEGER, 		new byte[]	{0x42, (byte)0xF7}							),		
		new TagType("EBMLMaxIDLength", 					TagType.TYPE_UINTEGER, 		new byte[]	{0x42, (byte)0xF2}							),		
		new TagType("EBMLMaxSizeLength", 				TagType.TYPE_STRING, 		new byte[]	{0x42, (byte)0xF3}							),		
		new TagType("DocType", 							TagType.TYPE_UINTEGER, 		new byte[]	{0x42, (byte)0x82}							),		
		new TagType("DocTypeVersion", 					TagType.TYPE_UINTEGER, 		new byte[]	{0x42, (byte)0x87}							),		
		new TagType("Segment", 							TagType.TYPE_ROOT, 			new byte[]	{0x18, 0x53, (byte)0x80, 0x67}				),		
		new TagType("SeekHead", 						TagType.TYPE_ROOT, 			new byte[]	{0x11, 0x4D, (byte)0x9B, 0x74}				),		
		new TagType("SeekEntry", 						TagType.TYPE_ROOT, 			new byte[]	{0x4D, (byte)0xBB}							),		
		new TagType("SeekID", 							TagType.TYPE_BINARY, 		new byte[]	{0x53, (byte)0xAB}							),		
		new TagType("SeekPosition", 					TagType.TYPE_UINTEGER, 		new byte[]	{0x53, (byte)0xAC}							),		
		new TagType("SegmentInfo", 						TagType.TYPE_ROOT, 			new byte[]	{0x15, (byte)0x49, (byte)0xA9, (byte)0x66}	),		
		new TagType("SegmentUID", 						TagType.TYPE_BINARY, 		new byte[]	{0x73, (byte)0xA4}							),		
		new TagType("SegmentFilename", 					TagType.TYPE_STRING, 		new byte[]	{0x73, (byte)0x84}							),		
		new TagType("TimecodeScale", 					TagType.TYPE_UINTEGER, 		new byte[]	{0x2A, (byte)0xD7, (byte)0xB1}				),		
		new TagType("Duration", 						TagType.TYPE_FLOAT, 		new byte[]	{0x44, (byte)0x89}							),		
		new TagType("DateUTC", 							TagType.TYPE_DATE, 			new byte[]	{0x44, (byte)0x61}							),		
		new TagType("Title", 							TagType.TYPE_STRING, 		new byte[]	{0x7B, (byte)0xA9}							),		
		new TagType("MuxingApp", 						TagType.TYPE_STRING, 		new byte[]	{0x4D, (byte)0x80}							),		
		new TagType("WritingApp", 						TagType.TYPE_STRING, 		new byte[]	{0x57, 0x41}								),		
		new TagType("Tracks", 							TagType.TYPE_ROOT, 			new byte[]	{0x16, (byte)0x54, (byte)0xAE, (byte)0x6B}	),		
		new TagType("TrackEntry", 						TagType.TYPE_ROOT, 			new byte[]	{(byte)0xAE}								),		
		new TagType("TrackNumber", 						TagType.TYPE_UINTEGER, 		new byte[]	{(byte)0xD7}								),		
		new TagType("TrackUID", 						TagType.TYPE_UINTEGER, 		new byte[]	{0x73, (byte)0xC5}							),		
		new TagType("TrackType", 						TagType.TYPE_UINTEGER, 		new byte[]	{(byte)0x83}								),		
		new TagType("TrackDefaultDuration", 			TagType.TYPE_UINTEGER, 		new byte[]	{0x23, (byte)0xE3, (byte)0x83}				),		
		new TagType("TrackName", 						TagType.TYPE_STRING, 		new byte[]	{0x53, 0x6E}								),		
		new TagType("TrackLanguage", 					TagType.TYPE_ASCII_STRING, 	new byte[]	{0x22, (byte)0xB5, (byte)0x9C}				),		
		new TagType("TrackCodecID", 					TagType.TYPE_ASCII_STRING, 	new byte[]	{(byte)0x86}								),		
		new TagType("TrackCodecPrivate", 				TagType.TYPE_BINARY, 		new byte[]	{(byte)0x63, (byte)0xA2}					),		
		new TagType("TrackVideo", 						TagType.TYPE_ROOT, 			new byte[]	{(byte)0xE0}								),		
		new TagType("PixelWidth", 						TagType.TYPE_UINTEGER, 		new byte[]	{(byte)0xB0}								),		
		new TagType("PixelHeight", 						TagType.TYPE_UINTEGER, 		new byte[]	{(byte)0xBA}								),		
		new TagType("DisplayWidth", 					TagType.TYPE_UINTEGER, 		new byte[]	{0x54, (byte)0xB0}							),		
		new TagType("DisplayHeight", 					TagType.TYPE_UINTEGER, 		new byte[]	{0x54, (byte)0xBA}							),		
		new TagType("TrackAudio", 						TagType.TYPE_ROOT, 			new byte[]	{(byte)0xE1}								),		
		new TagType("SamplingFrequency", 				TagType.TYPE_FLOAT, 		new byte[]	{(byte)0xB5}								),		
		new TagType("OutputSamplingFrequency", 			TagType.TYPE_FLOAT, 		new byte[]	{0x78, (byte)0xB5}							),		
		new TagType("Channels", 						TagType.TYPE_UINTEGER, 		new byte[]	{(byte)0x9F}								),		
		new TagType("BitDepth", 						TagType.TYPE_UINTEGER, 		new byte[]	{0x62, 0x64}								),		
		new TagType("Attachments", 						TagType.TYPE_ROOT, 			new byte[]	{0x19, 0x41, (byte)0xA4, 0x69}				),		
		new TagType("AttachedFile", 					TagType.TYPE_ROOT, 			new byte[]	{0x61, (byte)0xA7}							),		
		new TagType("AttachedFileDescription", 			TagType.TYPE_STRING, 		new byte[]	{0x46, (byte)0x7E}							),		
		new TagType("AttachedFileName", 				TagType.TYPE_STRING, 		new byte[]	{0x46, (byte)0x6E}							),		
		new TagType("AttachedFileMimeType", 			TagType.TYPE_ASCII_STRING, 	new byte[]	{0x46, (byte)0x60}							),		
		new TagType("AttachedFileData", 				TagType.TYPE_BINARY, 		new byte[]	{0x46, (byte)0x5C}							),		
		new TagType("AttachedFileUID", 					TagType.TYPE_UINTEGER, 		new byte[]	{0x46, (byte)0xAE}							),		
		new TagType("Tags", 							TagType.TYPE_ROOT, 			new byte[]	{0x12, (byte)0x54, (byte)0xC3, (byte)0x67}	),		
		new TagType("Tag", 								TagType.TYPE_ROOT, 			new byte[]	{0x73, (byte)0x73}							),		
		new TagType("TagTargets", 						TagType.TYPE_ROOT, 			new byte[]	{0x63, (byte)0xC0}							),		
		new TagType("TagTargetTrackUID", 				TagType.TYPE_UINTEGER, 		new byte[]	{0x63, (byte)0xC5}							),		
		new TagType("TagTargetChapterUID", 				TagType.TYPE_UINTEGER, 		new byte[]	{0x63, (byte)0xC4}							),		
		new TagType("TagTargetAttachmentUID",			TagType.TYPE_UINTEGER, 		new byte[]	{0x63, (byte)0xC6}							),		
		new TagType("TagSimpleTag",						TagType.TYPE_ROOT, 			new byte[]	{0x67, (byte)0xC8}							),		
		new TagType("TagSimpleTagName",					TagType.TYPE_STRING, 		new byte[]	{0x45, (byte)0xA3}							),		
		new TagType("TagSimpleTagString",				TagType.TYPE_STRING, 		new byte[]	{0x44, (byte)0x87}							),		
		new TagType("TagSimpleTagBinary",				TagType.TYPE_BINARY, 		new byte[]	{0x44, (byte)0x85}							),		
		new TagType("Cluster",							TagType.TYPE_ROOT, 			new byte[]	{0x1F, (byte)0x43, (byte)0xB6, (byte)0x75}	),		
		new TagType("ClusterTimecode",					TagType.TYPE_UINTEGER, 		new byte[]	{(byte)0xE7}								),		
		new TagType("ClusterBlockGroup",				TagType.TYPE_ROOT, 			new byte[]	{(byte)0xA0}								),		
		new TagType("ClusterBlock",						TagType.TYPE_BINARY,		new byte[]	{(byte)0xA1}								),		
		new TagType("ClusterSimpleBlock",				TagType.TYPE_BINARY,		new byte[]	{(byte)0xA3}								),				
		new TagType("ClusterBlockDuration",				TagType.TYPE_UINTEGER, 		new byte[]	{(byte)0x9B}								),		
		new TagType("ClusterReferenceBlock",			TagType.TYPE_SINTEGER, 		new byte[]	{(byte)0xFB}								),		
		new TagType("Chapters",							TagType.TYPE_ROOT, 			new byte[]	{0x10, (byte)0x43, (byte)0xA7, (byte)0x70}	),		
		new TagType("ChapterEditionEntry",				TagType.TYPE_ROOT, 			new byte[]	{(byte)0x45, (byte)0xB9}					),		
		new TagType("ChapterEditionUID",				TagType.TYPE_UINTEGER, 		new byte[]	{(byte)0x45, (byte)0xBC}					),		
		new TagType("ChapterEditionFlagHidden",			TagType.TYPE_UINTEGER, 		new byte[]	{(byte)0x45, (byte)0xBD}					),		
		new TagType("ChapterEditionFlagDefault",		TagType.TYPE_UINTEGER, 		new byte[]	{(byte)0x45, (byte)0xDB}					),		
		new TagType("ChapterEditionManaged",			TagType.TYPE_UINTEGER, 		new byte[]	{(byte)0x45, (byte)0xDD}					),		
		new TagType("ChapterAtom",						TagType.TYPE_ROOT, 			new byte[]	{(byte)0xB6}								),		
		new TagType("ChapterAtomChapterUID",			TagType.TYPE_UINTEGER, 		new byte[]	{(byte)0x73, (byte)0xC4}					),		
		new TagType("ChapterAtomChapterTimeStart",		TagType.TYPE_UINTEGER, 		new byte[]	{(byte)0x91}								),		
		new TagType("ChapterAtomChapterTimeEnd",		TagType.TYPE_UINTEGER, 		new byte[]	{(byte)0x92}								),		
		new TagType("ChapterAtomChapterFlagHidden",		TagType.TYPE_UINTEGER, 		new byte[]	{(byte)0x98}								),		
		new TagType("ChapterAtomChapterFlagEnabled",	TagType.TYPE_UINTEGER, 		new byte[]	{(byte)0x45, (byte)0x98}					),		
		new TagType("ChapterAtomChapterPhysicalEquiv",	TagType.TYPE_UINTEGER, 		new byte[]	{(byte)0x63, (byte)0xC3}					),		
		new TagType("ChapterAtomChapterTrack",			TagType.TYPE_ROOT, 			new byte[]	{(byte)0x8F}								),		
		new TagType("ChapterAtomChapterTrackNumber",	TagType.TYPE_UINTEGER, 		new byte[]	{(byte)0x89}								),		
		new TagType("ChapterAtomChapterDisplay",		TagType.TYPE_ROOT, 			new byte[]	{(byte)0x80}								),		
		new TagType("ChapterAtomChapString",			TagType.TYPE_STRING, 		new byte[]	{(byte)0x85}								),		
		new TagType("ChapterAtomChapLanguage",			TagType.TYPE_ASCII_STRING, 	new byte[]	{(byte)0x43, (byte)0x7C}					),		
		new TagType("ChapterAtomChapCountry",			TagType.TYPE_ASCII_STRING, 	new byte[]	{(byte)0x43, (byte)0x7E}					)
	};

	public static int getTagIndexFromID(byte[] tagID)
	{
		for(int i=0; i<matroska.length; i++)
		{
			if(Arrays.equals(matroska[i].id, tagID))
			{
				return i;
			}
		}
		return -1;
	}

	public static int getTagIndexFromName(String tagName)
	{
		for(int i=0; i<matroska.length; i++)
		{
			if(matroska[i].name == tagName)
			{
				return i;
			}
		}
		return -1;
	}

	
	public static int getTagType(byte[] tagID)
	{
		for(int i=0; i<matroska.length; i++)
		{
			if(Arrays.equals(matroska[i].id, tagID))
			{
				return matroska[i].type;
			}
		}
		return TagType.TYPE_UNKNOWN;
	}

	
	
	public static String getTagName(byte[] tagID)
	{
		for(int i=0; i<matroska.length; i++)
		{
			if(Arrays.equals(matroska[i].id, tagID))
			{
				return matroska[i].name;
			}
		}
		return "Unknown Tag";
	}
	
	public static TagType getTagType(String tagName)
	{
		for(int i=0; i<matroska.length; i++)
		{
			if(matroska[i].name == tagName)
			{
				return matroska[i];
			}
		}
		return matroska[0];
	}
}