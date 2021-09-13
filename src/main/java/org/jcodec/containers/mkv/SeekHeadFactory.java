package org.jcodec.containers.mkv;

import static org.jcodec.containers.mkv.MKVType.Seek;
import static org.jcodec.containers.mkv.MKVType.SeekHead;
import static org.jcodec.containers.mkv.MKVType.SeekID;
import static org.jcodec.containers.mkv.MKVType.SeekPosition;
import static org.jcodec.containers.mkv.MKVType.createByType;
import static org.jcodec.containers.mkv.boxes.EbmlUint.calculatePayloadSize;

import org.jcodec.containers.mkv.boxes.EbmlBase;
import org.jcodec.containers.mkv.boxes.EbmlBin;
import org.jcodec.containers.mkv.boxes.EbmlMaster;
import org.jcodec.containers.mkv.boxes.EbmlUint;
import org.jcodec.containers.mkv.muxer.MKVMuxer;
import org.jcodec.containers.mkv.util.EbmlUtil;

import java.lang.System;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * EBML IO implementation
 * 
 * @author The JCodec project
 * 
 */
public class SeekHeadFactory {
    List<SeekHeadFactory.SeekMock> a;
    long currentDataOffset = 0;
    final long cuesPos;

    public SeekHeadFactory(long cuesPos) {
        this.cuesPos = cuesPos;
        this.a = new ArrayList<SeekHeadFactory.SeekMock>();
    }

    public void add(EbmlBase e) {
        SeekHeadFactory.SeekMock z = SeekMock.make(e);
        z.dataOffset = currentDataOffset;
        z.seekPointerSize = calculatePayloadSize(
                Arrays.equals(e.id, MKVType.Cues.id) ? cuesPos < 0 ? z.dataOffset : cuesPos : z.dataOffset);
        currentDataOffset += z.size;
//        System.out.println("Added id:"+Reader.printAsHex(z.id)+" offset:"+z.dataOffset+" size:"+z.size+" seekpointer size:"+z.seekPointerSize);
        a.add(z);
    }

    public EbmlMaster indexSeekHead() {
        int seekHeadSize = computeSeekHeadSize();

        EbmlMaster seekHead = createByType(SeekHead);
        for (SeekHeadFactory.SeekMock z : a) {
            EbmlMaster seek = createByType(Seek);

            EbmlBin seekId = createByType(SeekID);
            seekId.setBuf(ByteBuffer.wrap(z.id));
            seek.add(seekId);

            EbmlUint seekPosition = createByType(SeekPosition);
            seekPosition.setUint(getSeekPos(z, seekHeadSize));
            if (seekPosition.data.limit() != z.seekPointerSize)
                System.err.println("estimated size of seekPosition differs from the one actually used. ElementId: "
                        + EbmlUtil.toHexString(z.id) + " " + seekPosition.getData().limit() + " vs "
                        + z.seekPointerSize);
            seek.add(seekPosition);

            seekHead.add(seek);
        }
        ByteBuffer mux = seekHead.getData();
        if (mux.limit() != seekHeadSize)
            System.err.println("estimated size of seekHead differs from the one actually used. " + mux.limit() + " vs "
                    + seekHeadSize);

        return seekHead;
    }

    private long getSeekPos(SeekMock z, int seekHeadSize) {
        long currSeekPos = z.dataOffset + seekHeadSize;
        if (Arrays.equals(z.id, MKVType.Cues.id) && cuesPos > 0) {
            currSeekPos = cuesPos;
        }
        return currSeekPos;
    }

    public int computeSeekHeadSize() {
        int seekHeadSize = estimateSize();
        boolean reindex = false;
        do {
            reindex = false;
            for (SeekMock z : a) {
                int minSize = calculatePayloadSize(getSeekPos(z, seekHeadSize));
                if (minSize > z.seekPointerSize) {
                    System.out.println("Size " + seekHeadSize + " seems too small for element "
                            + EbmlUtil.toHexString(z.id) + " increasing size by one.");
                    z.seekPointerSize += 1;
                    seekHeadSize += 1;
                    reindex = true;
                    break;
                } else if (minSize < z.seekPointerSize) {
                    throw new RuntimeException("Downsizing the index is not well thought through.");
                }
            }
        } while (reindex);
        return seekHeadSize;
    }

    int estimateSize() {
        int s = SeekHead.id.length + 1;
        s += estimeteSeekSize(a.get(0).id.length, 1);
        for (int i = 1; i < a.size(); i++) {
            s += estimeteSeekSize(a.get(i).id.length, a.get(i).seekPointerSize);
        }
        return s;
    }

    public static int estimeteSeekSize(int idLength, int offsetSizeInBytes) {
        int seekIdSize = SeekID.id.length + EbmlUtil.ebmlLength(idLength) + idLength;
        int seekPositionSize = SeekPosition.id.length + EbmlUtil.ebmlLength(offsetSizeInBytes) + offsetSizeInBytes;
        int seekSize = Seek.id.length + EbmlUtil.ebmlLength(seekIdSize + seekPositionSize) + seekIdSize
                + seekPositionSize;
        return seekSize;
    }

    public static class SeekMock {
        public long dataOffset;
        byte[] id;
        int size;
        int seekPointerSize;
        int fixOff;

        public static SeekHeadFactory.SeekMock make(EbmlBase e) {
            SeekHeadFactory.SeekMock z = new SeekMock();
            z.id = e.id;
            z.size = (int) e.size();
            return z;
        }
    }
}