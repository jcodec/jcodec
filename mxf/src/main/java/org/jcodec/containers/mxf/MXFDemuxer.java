package org.jcodec.containers.mxf;

import java.awt.Dimension;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import org.jcodec.common.DemuxerTrack;
import org.jcodec.common.SeekableByteChannel;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Rational;

import com.vg.mxf.KLV;
import com.vg.mxf.MxfStructure;
import com.vg.mxf.Registry;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * MXF demuxer
 * 
 * @author The JCodec project
 * 
 */
public class MXFDemuxer {

    public List<MXFPackage> getPackages() {
        return Arrays.asList(new MXFPackage[] { new MXFPackage() });
    }

    public class MXFPackage {

        public List<MXFTrack> getTracks() {
            return Arrays.asList(new MXFTrack[] { new MXFTrack() });
        }
    }

    public class MXFTrack implements DemuxerTrack {
        @Override
        public Packet nextFrame() throws IOException {
            try {
                do {
                    KLV k = KLV.readKL(in);
                    if (Registry.JPEG2000FrameWrappedPictureElement.matches(k.key)) {
                        // Time time = new Time(fn, frameRate.getNum(), fn *
                        // frameRate.getDen(), mm.getFrameDuration());
                        // Frame f = new Frame(time, null);
                        // ByteBuffer buffer = pool.take();
                        ByteBuffer buffer = ByteBuffer.allocate((int) k.len);
                        // buffer.limit((int) k.len);
                        // Assert.assertNotNull(buffer);
                        ch.read(buffer);
                        buffer.flip();
                        // f.setEncoded(buffer);
                        fn++;
                        return new Packet(buffer, 0, 0, 0, 0, true, null);
                    } else {
                        if (ch.position() + k.len < length) {
                            ch.position(ch.position() + k.len);
                        } else {
                            break;
                        }

                    }
                } while (ch.position() < length);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return null;
        }

        @Override
        public void seek(double second) {
            // TODO Auto-generated method stub

        }

        @Override
        public boolean gotoFrame(long i) {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public long getCurFrame() {
            // TODO Auto-generated method stub
            return 0;
        }
    }

    private MxfStructure mxf;
    private Dimension dimension;
    private Rational frameRate;
    private long duration;
    int fn = 0;
    private MxfMeta mm;
    private long length;
    private SeekableProxy in;
    private SeekableByteChannel ch;

    public MXFDemuxer(SeekableByteChannel ch) throws IOException {
        ch.position(0);
        in = new SeekableProxy(ch);
        this.ch = ch;
        mxf = MxfStructure.readStructure(in);
        KLV key = mxf.headerKLVs.lastEntry().getKey();
        long bodyOffset = key.dataOffset + key.len;
        ch.position(bodyOffset);
        mm = MxfMeta.fromMxfStructure(mxf);
        dimension = mm.getStoredDimension();
        frameRate = mm.editRate;
        duration = mm.getMediaDuration();
        // pool.setSize(dimension.width * dimension.height * 2 * 2);
        length = ch.size();
    }

    // @Override
    // public Interlacing getInterlaceType() {
    // int fieldDominance =
    // mxf.getPictureEssenceDescriptor().getFieldDominance();
    // if (mxf.getPictureEssenceDescriptor().getFrameLayout() != 0) {
    // switch (fieldDominance) {
    // case 1:
    // return Interlacing.TopFieldFirst;
    // case 2:
    // return Interlacing.BottomFieldFirst;
    // }
    //
    // }
    // return Interlacing.Progressive;
    // }
}
