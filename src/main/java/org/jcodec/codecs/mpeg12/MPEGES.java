package org.jcodec.codecs.mpeg12;

import static org.jcodec.codecs.mpeg12.MPEGConst.EXTENSION_START_CODE;
import static org.jcodec.codecs.mpeg12.MPEGConst.GROUP_START_CODE;
import static org.jcodec.codecs.mpeg12.MPEGConst.PICTURE_START_CODE;
import static org.jcodec.codecs.mpeg12.MPEGConst.SEQUENCE_END_CODE;
import static org.jcodec.codecs.mpeg12.MPEGConst.SEQUENCE_ERROR_CODE;
import static org.jcodec.codecs.mpeg12.MPEGConst.SEQUENCE_HEADER_CODE;
import static org.jcodec.codecs.mpeg12.bitstream.SequenceHeader.Sequence_Display_Extension;
import static org.jcodec.codecs.mpeg12.bitstream.SequenceHeader.Sequence_Extension;
import static org.jcodec.codecs.mpeg12.bitstream.SequenceHeader.Sequence_Scalable_Extension;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jcodec.codecs.mpeg12.bitstream.GOPHeader;
import org.jcodec.codecs.mpeg12.bitstream.SequenceHeader;
import org.jcodec.common.io.Buffer;
import org.jcodec.common.model.Packet;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class MPEGES {

    public static final int BUF_SIZE = 4096;

    private GOPHeader gh;
    private SequenceHeader sh;
    private FileInputStream is;
    private Buffer buf;
    private int frameNo;

    private List<Buffer> debug = new ArrayList<Buffer>();

    public long curPts;

    protected MPEGES() throws IOException {
        buf = fetchBuffer();
    }

    public MPEGES(File file) throws IOException {
        is = new FileInputStream(file);
        buf = fetchBuffer();
    }

    protected Buffer fetchBuffer() throws IOException {
        return Buffer.fetchFrom(is, BUF_SIZE);
    }

    public SequenceHeader getSequenceHeader() {
        return sh;
    }

    public GOPHeader getGroupheader() {
        return gh;
    }

    public Packet getFrame() throws IOException {
        List<Buffer> picture = null;
        while (true) {
            if (!nextStartCode()) {
                return (picture == null || picture.size() == 0) ? null : new Packet(Buffer.combine(picture), curPts,
                        90000, 0, frameNo++, true, null);
            }

            if (buf.get(3) == PICTURE_START_CODE) {
                if (picture == null)
                    picture = new ArrayList<Buffer>();
                else {
                    // printDebug();
                    return new Packet(Buffer.combine(picture), curPts, 90000, 0, frameNo++, true, null);
                }
            }

            Buffer nextSegment = getNextSegment(buf);
            if (nextSegment == null) {
                return (picture == null || picture.size() == 0) ? null : new Packet(Buffer.combine(picture), curPts,
                        90000, 0, frameNo++, true, null);
            }
            int startCode = nextSegment.get(3);
            if (startCode == SEQUENCE_HEADER_CODE) {
                SequenceHeader newSh = SequenceHeader.read(nextSegment.from(4));
                if (sh != null) {
                    newSh.copyExtensions(sh);
                }
                sh = newSh;
            } else if (startCode == GROUP_START_CODE) {
                gh = GOPHeader.read(nextSegment.from(4));
            } else if (startCode == SEQUENCE_END_CODE || startCode == SEQUENCE_ERROR_CODE) {
                buf = buf.from(4);
            } else if (startCode == EXTENSION_START_CODE) {
                int extType = nextSegment.get(4) >> 4;
                if (extType == Sequence_Extension || extType == Sequence_Scalable_Extension
                        || extType == Sequence_Display_Extension)
                    SequenceHeader.readExtension(nextSegment.from(4), sh);
                else if (picture != null)
                    picture.add(nextSegment);
                else
                    buf = buf.from(4);
            } else if (startCode >= 0xB9) {
                throw new RuntimeException("Unsupported start code: " + startCode);
            } else {
                if (picture != null) {
                    picture.add(nextSegment);
                } else {
                    buf = buf.from(4);
                }
            }
        }
    }

    private boolean nextStartCode() throws IOException {
        do {
            int ind = buf.search(0, 0, 1);
            if (ind != -1)
                buf = buf.from(ind);
            if (buf.remaining() < 4 || ind == -1) {
                Buffer b = fetchBuffer();
                if (b == null)
                    return false;
                buf.extendWith(b);
            }
        } while (buf.get(0) != 0 || buf.get(1) != 0 || buf.get(2) != 1);
        return true;
    }

    public Buffer getNextSegment(Buffer buf) throws IOException {
        int idx = buf.from(4).search(0, 0, 1);
        while (idx == -1) {
            Buffer buffer = fetchBuffer();
            if (buffer == null)
                return buf.remaining() == 0 ? null : buf.read(buf.remaining());
            buf.extendWith(buffer);
            idx = buf.from(4).search(0, 0, 1);
        }

        Buffer read = buf.read(idx + 4);
        debug.add(read);
        return read;
    }

    void printDebug() {
        Buffer combine = Buffer.combine(debug);
        byte[] data = combine.buffer;
        for (int i = 0; i < data.length; i++) {
            System.out.print((data[i] & 0xff) + ", ");
            if (i % 100 == 99)
                System.out.println();
        }
    }
}