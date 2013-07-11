package org.jcodec.containers.mkv.elements;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import org.jcodec.containers.mkv.Reader;

public class Block {

    private long pos;
    private long size;
    private int headerSize;

    public Block(long pos, long size) {
        this.pos = pos;
        this.size = size;
    }

    public static Block create(long pos, long size) {
        return new Block(pos, size);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Offset: ").append(pos).append("\n");
        sb.append("Size: ").append(size).append("\n");
        return sb.toString();
    }

    public void seekParsePrint(FileChannel source, File parent) throws Exception {
        boolean keyFrame;
        ByteBuffer data = ByteBuffer.allocate((int) this.size);
        source.position(pos);
        source.read(data);
        System.out.println("Offset: " + pos);
        System.out.println("Size: "+size);
        int trackNr = (int) Reader.getEbmlVInt(data);
        
        int index = data.position(); // skip bytes that
                                    // contain Track Nr

        // Read timecode
        int blockTimecode1 = data.get() & 0xFF;
        int blockTimecode2 = data.get() & 0xFF;
        int timecode = (blockTimecode1 << 8) | blockTimecode2;

        // Read flags
        byte flags = data.get();
        int keyFlag = flags & 0x80;
        if (keyFlag > 0)
            keyFrame = true;
        else
            keyFrame = false;

        int laceFlags = flags & 0x06;
        // Increase the HeaderSize by the number of bytes we have read
        if (laceFlags != 0x00) {
            long[] sizes = null;
            // We have lacing
            byte lacesCount = data.get();
            // HeaderSize += 1;
            if (laceFlags == 0x02) { // Xiph Lacing
                System.out.println("Lacing: Xiph");
                sizes = readXiphLaceSizes(data, lacesCount);
                
                int currentFramePos = headerSize;
                for (int i = 0; i < sizes.length; i++) {
                    int s = (int) sizes[i];
                    System.out.println("Lace Size: " + s);
                    byte[] frameData = new byte[s];
                    if (frameData.length + currentFramePos > data.limit()) {
                        System.err.println("file offset [" + pos + "] laced frame [" + i + "] position+size ("
                                + (frameData.length + currentFramePos) + ") exceeds block... skipping read");
                        return;
                    }
                    System.arraycopy(data, currentFramePos, frameData, 0, frameData.length);
                    currentFramePos += s;
                }

            } else if (laceFlags == 0x06) { // EBML Lacing
                System.out.println("Lacing: EBML");
                sizes = readEBMLLaceSizes(data, lacesCount);
                System.out.println("Header Size: " + headerSize);
                
                int currentFramePos = headerSize;
                for (int i = 0; i < sizes.length; i++) {
                    int s = (int) sizes[i];
                    System.out.println("Lace Size: " + s);
                    byte[] frameData = new byte[s];
                    if (frameData.length + currentFramePos > data.limit()) {
                        System.err.println("file offset [" + pos + "] laced frame [" + i + "] position+size ("
                                + (frameData.length + currentFramePos) + ") exceeds block... skipping read");
                        return;
                    }
                    System.arraycopy(data, currentFramePos, frameData, 0, frameData.length);
                    currentFramePos += s;
                }

            } else if (laceFlags == 0x04) { // Fixed Size Lacing
               this.headerSize = index;
               int laceSize = (int)((this.size - this.headerSize) / lacesCount);
               System.out.println("Lace Size: " + laceSize);
               System.out.println("Lace Count: " + lacesCount);
               
               for (int i = 0; i < lacesCount; i++) {
                   
                   byte[] frameData = new byte[laceSize];
                   if ((frameData.length*(i+1) + headerSize) > data.limit()) {
                       System.err.println("file offset [" + pos + "] laced frame [" + i + "] position+size ("
                               + ((frameData.length*(i+1) + headerSize)) + ") exceeds block... skipping read");
                       return;
                   }
                   System.arraycopy(data, index, frameData, 0, frameData.length);
                   index += laceSize;
               }
            } else {
                throw new RuntimeException("Unsupported lacing type flag.");
            }
        } else {
            System.out.println("Lacing: n/a");
        }
        System.out.println("Timecode: " + timecode);
        System.out.println("Track Nr: " + trackNr);
        System.out.println("KeyFrame: " + keyFrame);
        byte[] frameData = new byte[data.limit() - index];
        System.arraycopy(data, index, frameData, 0, frameData.length);

    }

    private long[] readXiphLaceSizes(ByteBuffer data, byte count) {
            long [] sizes = new long[count+1];
            sizes[count] = (int)this.size;

            for (int l = 0; l < count; l++) {
              short laceSize = 255;
              while (laceSize == 255) {
                laceSize = (short)(data.get() & 0xFF);
                sizes[l] += laceSize;
              }
              // Update the size of the last block
              sizes[count] -= sizes[l];
            }
            
            headerSize = data.position();
            sizes[count] -= headerSize;

            return sizes;
    }

    public long[] readEBMLLaceSizes(ByteBuffer data, short count) {
        long[] sizes = new long[count + 1];
        sizes[count] = (int) this.size;

        // This uses the DataSource.getBytePosition() for finding the header
        // size
        // because of the trouble of finding the byte size of sized ebml coded
        // integers
        sizes[0] = Reader.getEbmlVInt(data);
        

        sizes[count] -= sizes[0];

        long laceSize = sizes[0];
        long laceSizeDiff = 0;
        for (int l = 1; l < count; l++) {
            laceSizeDiff = Reader.getSignedEbmlVInt(data);
            

            laceSize += laceSizeDiff;
            sizes[l] = laceSize;

            // Update the size of the last block
            sizes[count] -= sizes[l];
        }

        headerSize = data.position();
        sizes[count] -= headerSize;
        return sizes;
    }
    //
    // public int[] readXiphLaceSizes(int index, short LaceCount) {
    // int [] LaceSizes = new int[LaceCount+1];
    // LaceSizes[LaceCount] = (int)this.getSize();
    //
    // //long ByteStartPos = source.getFilePointer();
    //
    // for (int l = 0; l < LaceCount; l++) {
    // short LaceSizeByte = 255;
    // while (LaceSizeByte == 255) {
    // LaceSizeByte = (short)(data[index++] & 0xFF);
    // HeaderSize += 1;
    // LaceSizes[l] += LaceSizeByte;
    // }
    // // Update the size of the last block
    // LaceSizes[LaceCount] -= LaceSizes[l];
    // }
    // //long ByteEndPos = source.getFilePointer();
    //
    // LaceSizes[LaceCount] -= HeaderSize;
    //
    // return LaceSizes;
    // }

    public byte[] readData(FileChannel ds) throws IOException {
        ByteBuffer buff = ByteBuffer.allocate((int)this.size);
        ds.position(this.pos);
        ds.read(buff);
        return buff.array();
    }

}
