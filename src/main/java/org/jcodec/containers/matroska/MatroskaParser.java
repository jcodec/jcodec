package org.jcodec.containers.matroska;

import java.io.*;
import java.util.Stack;
import java.util.LinkedList;

import org.jcodec.containers.matroska.ebml.*;

/**
 * This class is part of JCodec ( www.jcodec.org )
 * This software is distributed under FreeBSD License

 * Parses a Matroska Document
 *
 * Provides methods to extract tracks.
 * 
 * Based on work by Matroska.org and written by
 * John Cannon (c) 2002 <spyder@matroska.org>
 * Jory Stone  (c) 2004 <jebml@jory.info>
 *
 * @author George Gardiner <this.george@googlemail.com>
 *
 */

public class MatroskaParser
{
	private InputStream in;
	private MatroskaBlock lastBlockRead;
	private int framesLeft = 0;
	private Stack<RootTag> tagStack;
	private Stack<Track> tracks;
	private boolean hasTracks = false;
	
	private long referenceTimecode = 0;
	
	private String V_CODEC;
	private String A_CODEC;
	
	public MatroskaParser(InputStream in) throws IOException, EBMLException
	{
		setInputSource(in);
		tagStack = new Stack<RootTag>();
		tracks = new Stack<Track>();
		syncDocument(MatroskaDocument.getTagType("EBMLHeader"));
		readHeaders();
	}

	public MatroskaParser(InputStream in, TagType syncOn) throws IOException, EBMLException
	{
		setInputSource(in);
		tagStack = new Stack<RootTag>();
		tracks = new Stack<Track>();
		syncDocument(syncOn);
	}

	public void setInputSource(InputStream in)
	{
		this.in = in;
	}
	
	public Track getFirstTrack(int trackType)
	{
		for (Track t : tracks)
		{
			if(t.trackType == trackType) return t;
		}
		return null;
	}
	
	public void readHeaders() throws IOException, EBMLException
	{
		while(!hasTracks) parse();
	}
	
	public synchronized void parse() throws IOException, EBMLException
	{
		if(!tagStack.isEmpty())
		{
			Tag nextTag = null;
			nextTag = tagStack.peek().read();
			while(nextTag == null)
			{
				tagStack.pop();
				if(!tagStack.isEmpty())
				{
					nextTag = tagStack.peek().read();
				}
				else
				{
					break;
				}
			}
			if(nextTag != null)
			{
				if(nextTag.getType() == TagType.TYPE_ROOT)
				{
					RootTag rt = (RootTag)nextTag;
					if(tagStack.size() < 5) tagStack.push(rt);
					if(MatroskaDocument.getTagName(rt.id) == "TrackEntry")
					{
						tracks.push(new Track(this));
					}
					
					if(MatroskaDocument.getTagName(rt.id) == "TrackVideo")
					{
					}
					
					if(MatroskaDocument.getTagName(rt.id) == "TrackAudio")
					{
					}
				}
				
				if(nextTag.getType() == TagType.TYPE_BINARY)
				{
					BinaryTag bt = (BinaryTag)nextTag;
					if(MatroskaDocument.getTagName(bt.id) == "ClusterBlock" || MatroskaDocument.getTagName(bt.id) == "ClusterSimpleBlock")
					{
						hasTracks = true;
						MatroskaBlock mb = new MatroskaBlock(bt.getValue());
						for (Track t : tracks)
						{
							if(t.trackNumber == mb.getTrackNo())
							{
								t.addBlock(mb, referenceTimecode);
							}
						}
					}
					
					if(MatroskaDocument.getTagName(bt.id) == "TrackCodecPrivate")
					{
						tracks.peek().addCodecPrivate(bt.getValue());
					}
				}
				
				if(nextTag.getType() == TagType.TYPE_UINTEGER)
				{
					UIntTag uit = (UIntTag)nextTag;
					if(MatroskaDocument.getTagName(uit.id) == "ClusterTimecode")
					{
						referenceTimecode = uit.getValue();
					}
					if(MatroskaDocument.getTagName(uit.id) == "TrackNumber")
					{
						tracks.peek().trackNumber = (int)uit.getValue();
					}
					if(MatroskaDocument.getTagName(uit.id) == "TrackUID")
					{
						tracks.peek().trackUID = (int)uit.getValue();
					}
					if(MatroskaDocument.getTagName(uit.id) == "TrackType")
					{
						tracks.peek().trackType = (int)uit.getValue();
					}
					if(MatroskaDocument.getTagName(uit.id) == "TrackDefaultDuration")
					{
						tracks.peek().defaultTrackDuration = (int)uit.getValue();
					}
					if(MatroskaDocument.getTagName(uit.id) == "PixelWidth")
					{
						tracks.peek().videoTrackPixelWidth = (int)uit.getValue();
					}
					if(MatroskaDocument.getTagName(uit.id) == "PixelHeight")
					{
						tracks.peek().videoTrackPixelHeight = (int)uit.getValue();
					}
					if(MatroskaDocument.getTagName(uit.id) == "DisplayWidth")
					{
						tracks.peek().videoTrackPixelWidth = (int)uit.getValue();
					}	
					if(MatroskaDocument.getTagName(uit.id) == "DisplayHeight")
					{
						tracks.peek().videoTrackPixelWidth = (int)uit.getValue();
					}
				}
				
				if(nextTag.getType() == TagType.TYPE_STRING || nextTag.getType() == TagType.TYPE_ASCII_STRING)
				{
					StringTag st = (StringTag)nextTag;
					if(MatroskaDocument.getTagName(st.id) == "TrackName")
					{
						tracks.peek().trackName = st.getValue();
					}			
					if(MatroskaDocument.getTagName(st.id) == "TrackLanguage")
					{
						tracks.peek().trackLanguage = st.getValue();
					}			
					if(MatroskaDocument.getTagName(st.id) == "TrackCodecID")
					{
						tracks.peek().trackCodecID = st.getValue();
					}		
				}
			}
		}
	}
	
	public void syncDocument(TagType syncOn) throws IOException, EBMLException
	{
		FixedSizeInputStream fIN = new FixedSizeInputStream(in, -1);
		while(!tagStack.empty()) tagStack.pop();		
		RootTag v = new RootTag(MatroskaDocument.getTagType("VirtualRoot").id, fIN);
		tagStack.push(v);
		tagStack.push((RootTag)v.syncOn(syncOn));
	}
}
