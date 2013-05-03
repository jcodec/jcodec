/**
 * JEBML - Java library to read/write EBML/Matroska elements.
 * Copyright (C) 2004 Jory Stone <jebml@jory.info>
 * Based on Javatroska (C) 2002 John Cannon <spyder@matroska.org>
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jcodec.containers.mkv.elements;

/**
  * Matroska Track Class
  */
public class Track 
{
  public short trackNo;
  public long trackUID;
  public byte trackType;
  public long defaultDuration;
  public String name;
  public String language;
  public String codecID;
  public byte [] codecPrivate;

  public short videoPixelWidth;
  public short videoPixelHeight;
  public short videoDisplayWidth;
  public short videoDisplayHeight;
  public float audioSamplingFrequency;
  public float audioOutputSamplingFrequency;
  public short audioChannels;
  public byte audioBitDepth;

  /**
   * Converts the Track to String form
   * @return String form of MatroskaFileTrack data
   */
  public String toString() 
  {
    String s = new String();

    s += "\t\t" + "TrackNo: " + trackNo + "\n";
    s += "\t\t" + "TrackUID: " + trackUID + "\n";
    s += "\t\t" + "TrackType: " + trackTypeToString(trackType) + "\n";
    s += "\t\t" + "DefaultDuration: " + defaultDuration + "\n";
    s += "\t\t" + "Name: " + name + "\n";
    s += "\t\t" + "Language: " + language + "\n";
    s += "\t\t" + "CodecID: " + codecID + "\n";
    if (codecPrivate != null)
      s += "\t\t" + "CodecPrivate: " + codecPrivate.length + " byte(s)" + "\n";

    if (trackType == track_video) 
    {
      s += "\t\t" + "PixelWidth: " + videoPixelWidth + "\n";
      s += "\t\t" + "PixelHeight: " + videoPixelHeight + "\n";
      s += "\t\t" + "DisplayWidth: " + videoDisplayWidth + "\n";
      s += "\t\t" + "DisplayHeight: " + videoDisplayHeight + "\n";
    }

    if (trackType == track_audio) 
    {
      s += "\t\t" + "SamplingFrequency: " + audioSamplingFrequency + "\n";
      if (audioOutputSamplingFrequency != 0)
        s += "\t\t" + "OutputSamplingFrequency: " + audioOutputSamplingFrequency + "\n";
      s += "\t\t" + "Channels: " + audioChannels + "\n";
      if (audioBitDepth != 0)
        s += "\t\t" + "BitDepth: " + audioBitDepth + "\n";
    }

    return s;
  }
  
  // Track Types
  static public byte track_video       = 0x01; ///< Rectangle-shaped non-transparent pictures aka video
  static public byte track_audio       = 0x02; ///< Anything you can hear
  static public byte track_complex     = 0x03; ///< Audio and video in same track, used by DV
  static public byte track_logo        = 0x10; ///< Overlay-pictures, displayed over video
  static public byte track_subtitle    = 0x11; ///< Text-subtitles. One track contains one language and only one track can be active (player-side configuration)
  static public byte track_control     = 0x20; ///< Control-codes for menus and other stuff

  /**
   * Converts a integer track type to String form.
   *
   * @param trackType Integer Track Type
   * @return String <code>trackType</code> in String form
   */
  static public String trackTypeToString(byte trackType) {
    if (trackType == track_video)
      return "Video";
    if (trackType == track_audio)
      return "Audio";
    if (trackType == track_complex)
      return "Complex";
    if (trackType == track_logo)
      return "Logo";
    if (trackType == track_subtitle)
      return "Subtitle";
    if (trackType == track_control)
      return "Control";

    return "";
  }
}
