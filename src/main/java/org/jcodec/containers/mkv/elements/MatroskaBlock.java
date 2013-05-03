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

import org.jcodec.containers.mkv.ebml.BinaryElement;

public class MatroskaBlock extends BinaryElement {
  protected int [] sizes = null;
  protected int headerSize = 0;
  protected int blockTimecode = 0;
  protected int trackNo = 0;
  private boolean keyFrame;



public MatroskaBlock(byte[] type) {
    super(type);
  }

//  //public void readData(DataSource source) {
//  //  parseBlock();
//  //}
//
//  public void parseBlock() {
//    int index = 0;
//    trackNo = (int)Reader.bytesToLong(data);
//    index = Element.getEbmlSize(trackNo);
//    headerSize += index;
//
//    short BlockTimecode1 = (short)(data[index++] & 0xFF);
//    short BlockTimecode2 = (short)(data[index++] & 0xFF);
//    if (BlockTimecode1 != 0 || BlockTimecode2 != 0) {
//      blockTimecode = (BlockTimecode1 << 8) | BlockTimecode2;
//    }
//    
//    
//    int keyFlag = data[index] & 0x80;
//    if(keyFlag > 0)
//    	this.keyFrame = true;
//    else
//    	this.keyFrame = false;
//    
//    int LaceFlag = data[index] & 0x06;
//    index++;
//    // Increase the HeaderSize by the number of bytes we have read
//    headerSize += 3;
//    if (LaceFlag != 0x00) {
//      // We have lacing
//      byte LaceCount = data[index++];
//      headerSize += 1;
//      if (LaceFlag == 0x02) { // Xiph Lacing
//        sizes = readXiphLaceSizes(index, LaceCount);
//
//      } else if (LaceFlag == 0x06) { // EBML Lacing
//        sizes = readEBMLLaceSizes(index, LaceCount);
//
//      } else if (LaceFlag == 0x04) { // Fixed Size Lacing
//        sizes = new int[LaceCount+1];
//        sizes[0] = (int)(this.size - headerSize) / (LaceCount+1);
//        for (int s = 0; s < LaceCount; s++)
//          sizes[s+1] = sizes[0];
//      } else {
//        throw new RuntimeException("Unsupported lacing type flag.");
//      }
//    } 
//    //data = new byte[(int)(this.getSize() - HeaderSize)];
//    //source.read(data, 0, data.length);
//    //this.dataRead = true;
//  }
//
//  public int[] readEBMLLaceSizes(int index, short laceCount) {
//    int [] laceSizes = new int[laceCount+1];
//    laceSizes[laceCount] = (int)this.size;
//
//    // This uses the DataSource.getBytePosition() for finding the header size
//    // because of the trouble of finding the byte size of sized ebml coded integers
//    //long ByteStartPos = source.getFilePointer();
//    int startIndex = index;
//
//    laceSizes[0] = (int)Reader.getEbmlVInt(data, index);
//    index += Element.getEbmlSize(laceSizes[0]);
//    laceSizes[laceCount] -= laceSizes[0];
//
//    long FirstEBMLSize = laceSizes[0];
//    long LastEBMLSize = 0;
//    for (int l = 0; l < laceCount-1; l++) {
//      LastEBMLSize = Reader.getSignedEbmlVInt(data, index);
//      index += Element.getEbmlSize(LastEBMLSize);
//
//      FirstEBMLSize += LastEBMLSize;
//      laceSizes[l+1] = (int)FirstEBMLSize;
//
//      // Update the size of the last block
//      laceSizes[laceCount] -= laceSizes[l+1];
//    }
//    //long ByteEndPos = source.getFilePointer();
//
//    //HeaderSize = HeaderSize + (int)(ByteEndPos - ByteStartPos);
//    headerSize = headerSize + (int)(index - startIndex);
//    laceSizes[laceCount] -= headerSize;
//
//    return laceSizes;
//  }
//
//  public int[] readXiphLaceSizes(int index, short LaceCount) {
//    int [] LaceSizes = new int[LaceCount+1];
//    LaceSizes[LaceCount] = (int)this.size;
//
//    //long ByteStartPos = source.getFilePointer();
//
//    for (int l = 0; l < LaceCount; l++) {
//      short LaceSizeByte = 255;
//      while (LaceSizeByte == 255) {
//        LaceSizeByte = (short)(data[index++] & 0xFF);
//        headerSize += 1;
//        LaceSizes[l] += LaceSizeByte;
//      }
//      // Update the size of the last block
//      LaceSizes[LaceCount] -= LaceSizes[l];
//    }
//    //long ByteEndPos = source.getFilePointer();
//
//    LaceSizes[LaceCount] -= headerSize;
//
//    return LaceSizes;
//  }
//
//  public int getFrameCount() {
//    if (sizes == null) {
//      return 1;
//    }
//    return sizes.length;
//  }
//
//  public byte [] getFrame(int frame) {
//    if (sizes == null) {
//      if (frame != 0) {
//        throw new IllegalArgumentException("Tried to read laced frame on non-laced Block. MatroskaBlock.getFrame(frame > 0)");
//      }
//      byte [] FrameData = new byte[data.length-headerSize];
//      ArrayCopy.arraycopy(data, headerSize, FrameData, 0, FrameData.length);
//      
//      return FrameData;
//    }
//    byte [] FrameData = new byte[sizes[frame]];
//
//    // Calc the frame data offset
//    int StartOffset = headerSize;
//    for (int s = 0; s < frame; s++) {
//      StartOffset += sizes[s];
//    }
//
//    // Copy the frame data
//    ArrayCopy.arraycopy(data, StartOffset, FrameData, 0, FrameData.length);
//
//    return FrameData;
//  }
//
//  public long getAdjustedBlockTimecode(long ClusterTimecode, long TimecodeScale) {
//    return ClusterTimecode + (blockTimecode);// * TimecodeScale);
//  }
//
//  public int getTrackNo() {
//    return trackNo;
//  }
//
//  public int getBlockTimecode() {
//    return blockTimecode;
//  }
//
//  public void setFrameData(short trackNo, int timecode, byte [] data) 
//  {
//
//  }
//  public boolean isKeyFrame() {
//    return keyFrame;
//  }
}
