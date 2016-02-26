package org.jcodec.containers.mps;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.jcodec.common.IntArrayList;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.Rational;
import org.jcodec.containers.mps.MPSDemuxer.PESPacket;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Demuxer for MPEG Program Stream format
 * 
 * @author The JCodec project
 * 
 */
public class MPSUtils {

    public static final int VIDEO_MIN = 0x1E0;
    public static final int VIDEO_MAX = 0x1EF;

    public static final int AUDIO_MIN = 0x1C0;
    public static final int AUDIO_MAX = 0x1DF;

    public static final int PACK = 0x1ba;
    public static final int SYSTEM = 0x1bb;
    public static final int PSM = 0x1bc;
    public static final int PRIVATE_1 = 0x1bd;
    public static final int PRIVATE_2 = 0x1bf;

    public static final boolean mediaStream(int streamId) {
        return (streamId >= $(AUDIO_MIN) && streamId <= $(VIDEO_MAX) || streamId == $(PRIVATE_1)
                || streamId == $(PRIVATE_2));
    }

    public static final boolean mediaMarker(int marker) {
        return (marker >= AUDIO_MIN && marker <= VIDEO_MAX || marker == PRIVATE_1 || marker == PRIVATE_2);
    }

    public static final boolean psMarker(int marker) {
        return marker >= PRIVATE_1 && marker <= VIDEO_MAX;
    }

    public static boolean videoMarker(int marker) {
        return marker >= VIDEO_MIN && marker <= VIDEO_MAX;
    }

    public static final boolean videoStream(int streamId) {
        return streamId >= $(VIDEO_MIN) && streamId <= $(VIDEO_MAX);
    }

    public static boolean audioStream(int streamId) {
        return streamId >= $(AUDIO_MIN) && streamId <= $(AUDIO_MAX) || streamId == $(PRIVATE_1)
                || streamId == $(PRIVATE_2);
    }

    static int $(int marker) {
        return marker & 0xff;
    }

    public static abstract class PESReader {

        private int marker = -1;
        private int lenFieldLeft;
        private int pesLen;
        private long pesFileStart = -1;
        private int stream;
        private boolean _pes;
        private int pesLeft;

        private ByteBuffer pesBuffer;
            
        public PESReader() {
            this.pesBuffer = ByteBuffer.allocate(1 << 21);
        }
        
        protected abstract void pes(ByteBuffer pesBuffer, long start, int pesLen, int stream);

        public void analyseBuffer(ByteBuffer buf, long pos) {
            int init = buf.position();
            while (buf.hasRemaining()) {
                if (pesLeft > 0) {
                    int toRead = Math.min(buf.remaining(), pesLeft);
                    pesBuffer.put(NIOUtils.read(buf, toRead));
                    pesLeft -= toRead;

                    if (pesLeft == 0) {
                        long filePos = pos + buf.position() - init;
                        pes1(pesBuffer, pesFileStart, (int) (filePos - pesFileStart), stream);
                        pesFileStart = -1;
                        _pes = false;
                        stream = -1;
                    }
                    continue;
                }
                int bt = buf.get() & 0xff;
                if (_pes)
                    pesBuffer.put((byte) (marker >>> 24));
                marker = (marker << 8) | bt;
                if (marker >= SYSTEM && marker <= VIDEO_MAX) {
                    long filePos = pos + buf.position() - init - 4;
                    if (_pes)
                        pes1(pesBuffer, pesFileStart, (int) (filePos - pesFileStart), stream);
                    pesFileStart = filePos;

                    _pes = true;
                    stream = marker & 0xff;
                    lenFieldLeft = 2;
                    pesLen = 0;
                } else if (marker >= 0x1b9 && marker <= 0x1ff) {
                    if (_pes) {
                        long filePos = pos + buf.position() - init - 4;
                        pes1(pesBuffer, pesFileStart, (int) (filePos - pesFileStart), stream);
                    }
                    pesFileStart = -1;
                    _pes = false;
                    stream = -1;
                } else if (lenFieldLeft > 0) {
                    pesLen = (pesLen << 8) | bt;
                    lenFieldLeft--;
                    if (lenFieldLeft == 0) {
                        pesLeft = pesLen;
                        if (pesLen != 0) {
                            flushMarker();
                            marker = -1;
                        }
                    }
                }
            }
        }

        private void flushMarker() {
            pesBuffer.put((byte) (marker >>> 24));
            pesBuffer.put((byte) ((marker >>> 16) & 0xff));
            pesBuffer.put((byte) ((marker >>> 8) & 0xff));
            pesBuffer.put((byte) (marker & 0xff));
        }

        private void pes1(ByteBuffer pesBuffer, long start, int pesLen, int stream) {
            pesBuffer.flip();
            pes(pesBuffer, start, pesLen, stream);
            pesBuffer.clear();
        }

        public void finishRead() {
            if (pesLeft <= 4) {
                flushMarker();
                pes1(pesBuffer, pesFileStart, pesBuffer.position(), stream);
            }
        }
    }

    public static PESPacket readPESHeader(ByteBuffer iss, long pos) {
        int streamId = iss.getInt() & 0xff;
        int len = iss.getShort() & 0xffff;
        if (streamId != 0xbf) {
            int b0 = iss.get() & 0xff;
            if ((b0 & 0xc0) == 0x80)
                return mpeg2Pes(b0, len, streamId, iss, pos);
            else
                return mpeg1Pes(b0, len, streamId, iss, pos);
        }
        return new PESPacket(null, -1, streamId, len, pos, -1);
    }

    public static PESPacket mpeg1Pes(int b0, int len, int streamId, ByteBuffer is, long pos) {
        int c = b0;
        while (c == 0xff) {
            c = is.get() & 0xff;
        }

        if ((c & 0xc0) == 0x40) {
            is.get();
            c = is.get() & 0xff;
        }
        long pts = -1, dts = -1;
        if ((c & 0xf0) == 0x20) {
            pts = readTs(is, c);
        } else if ((c & 0xf0) == 0x30) {
            pts = readTs(is, c);
            dts = readTs(is);
        } else {
            if (c != 0x0f)
                throw new RuntimeException("Invalid data");
        }

        return new PESPacket(null, pts, streamId, len, pos, dts);
    }

    public static long readTs(ByteBuffer is, int c) {
        return (((long) c & 0x0e) << 29) | ((is.get() & 0xff) << 22) | (((is.get() & 0xff) >> 1) << 15)
                | ((is.get() & 0xff) << 7) | ((is.get() & 0xff) >> 1);
    }

    public static PESPacket mpeg2Pes(int b0, int len, int streamId, ByteBuffer is, long pos) {
        int flags1 = b0;
        int flags2 = is.get() & 0xff;
        int header_len = is.get() & 0xff;

        long pts = -1, dts = -1;
        if ((flags2 & 0xc0) == 0x80) {
            pts = readTs(is);
            NIOUtils.skip(is, header_len - 5);
        } else if ((flags2 & 0xc0) == 0xc0) {
            pts = readTs(is);
            dts = readTs(is);
            NIOUtils.skip(is, header_len - 10);
        } else
            NIOUtils.skip(is, header_len);

        return new PESPacket(null, pts, streamId, len, pos, dts);
    }

    public static long readTs(ByteBuffer is) {
        return (((long) is.get() & 0x0e) << 29) | ((is.get() & 0xff) << 22) | (((is.get() & 0xff) >> 1) << 15)
                | ((is.get() & 0xff) << 7) | ((is.get() & 0xff) >> 1);
    }

    public static void writeTs(ByteBuffer is, long ts) {
        is.put((byte) ((ts >> 29) << 1));
        is.put((byte) (ts >> 22));
        is.put((byte) ((ts >> 15) << 1));
        is.put((byte) (ts >> 7));
        is.put((byte) (ts >> 1));
    }

    public static class MPEGMediaDescriptor {
        private int tag;
        private int len;

        public void parse(ByteBuffer buf) {
            tag = buf.get() & 0xff;
            len = buf.get() & 0xff;
        }

        public int getTag() {
            return tag;
        }

        public int getLen() {
            return len;
        }
    }

    public static class VideoStreamDescriptor extends MPEGMediaDescriptor {

        private int multipleFrameRate;
        private int frameRateCode;
        private boolean mpeg1Only;
        private int constrainedParameter;
        private int stillPicture;
        private int profileAndLevel;
        private int chromaFormat;
        private int frameRateExtension;
        Rational[] frameRates;

        public VideoStreamDescriptor() {
            this.frameRates = new Rational[] { null, new Rational(24000, 1001), new Rational(24, 1),
                    new Rational(25, 1), new Rational(30000, 1001), new Rational(30, 1), new Rational(50, 1),
                    new Rational(60000, 1001), new Rational(60, 1), null, null, null, null, null, null, null};
        }
            
        @Override
        public void parse(ByteBuffer buf) {
            super.parse(buf);
            int b0 = buf.get() & 0xff;
            multipleFrameRate = (b0 >> 7) & 1;
            frameRateCode = (b0 >> 3) & 0xf;
            mpeg1Only = ((b0 >> 2) & 1) == 0;
            constrainedParameter = (b0 >> 1) & 1;
            stillPicture = b0 & 1;
            if (!mpeg1Only) {
                profileAndLevel = buf.get() & 0xff;
                int b1 = buf.get() & 0xff;
                chromaFormat = b1 >> 6;
                frameRateExtension = (b1 >> 5) & 1;
            }
        }

        public Rational getFrameRate() {
            return frameRates[frameRateCode];
        }

        public int getMultipleFrameRate() {
            return multipleFrameRate;
        }

        public int getFrameRateCode() {
            return frameRateCode;
        }

        public boolean isMpeg1Only() {
            return mpeg1Only;
        }

        public int getConstrainedParameter() {
            return constrainedParameter;
        }

        public int getStillPicture() {
            return stillPicture;
        }

        public int getProfileAndLevel() {
            return profileAndLevel;
        }

        public int getChromaFormat() {
            return chromaFormat;
        }

        public int getFrameRateExtension() {
            return frameRateExtension;
        }
    }

    public static class AudioStreamDescriptor extends MPEGMediaDescriptor {
        private int variableRateAudioIndicator;
        private int freeFormatFlag;
        private int id;
        private int layer;

        @Override
        public void parse(ByteBuffer buf) {
            super.parse(buf);
            int b0 = buf.get() & 0xff;
            freeFormatFlag = (b0 >> 7) & 1;
            id = (b0 >> 6) & 1;
            layer = (b0 >> 5) & 3;
            variableRateAudioIndicator = (b0 >> 3) & 1;
        }

        public int getVariableRateAudioIndicator() {
            return variableRateAudioIndicator;
        }

        public int getFreeFormatFlag() {
            return freeFormatFlag;
        }

        public int getId() {
            return id;
        }

        public int getLayer() {
            return layer;
        }
    }

    public static class ISO639LanguageDescriptor extends MPEGMediaDescriptor {
        private IntArrayList languageCodes = new IntArrayList();
        @Override
        public void parse(ByteBuffer buf) {
            super.parse(buf);
            while (buf.remaining() >= 4) {
                languageCodes.add(buf.getInt());
            }
        }

        public IntArrayList getLanguageCodes() {
            return languageCodes;
        }
    }

    public static class Mpeg4VideoDescriptor extends MPEGMediaDescriptor {
        private int profileLevel;

        @Override
        public void parse(ByteBuffer buf) {
            super.parse(buf);
            profileLevel = buf.get() & 0xff;
        }
    }

    public static class Mpeg4AudioDescriptor extends MPEGMediaDescriptor {

        private int profileLevel;

        @Override
        public void parse(ByteBuffer buf) {
            super.parse(buf);
            profileLevel = buf.get() & 0xff;
        }

        public int getProfileLevel() {
            return profileLevel;
        }
    }

    public static class AVCVideoDescriptor extends MPEGMediaDescriptor {

        private int profileIdc;
        private int flags;
        private int level;

        @Override
        public void parse(ByteBuffer buf) {
            super.parse(buf);
            profileIdc = buf.get() & 0xff;
            flags = buf.get() & 0xff;
            level = buf.get() & 0xff;
        }

        public int getProfileIdc() {
            return profileIdc;
        }

        public int getFlags() {
            return flags;
        }

        public int getLevel() {
            return level;
        }
    }

    public static class AACAudioDescriptor extends MPEGMediaDescriptor {
        private int profile;
        private int channel;
        private int flags;

        @Override
        public void parse(ByteBuffer buf) {
            super.parse(buf);
            profile = buf.get() & 0xff;
            channel = buf.get() & 0xff;
            flags = buf.get() & 0xff;
        }

        public int getProfile() {
            return profile;
        }

        public int getChannel() {
            return channel;
        }

        public int getFlags() {
            return flags;
        }
    }

    public static class DataStreamAlignmentDescriptor extends MPEGMediaDescriptor {
        private int alignmentType;
        
        @Override
        public void parse(ByteBuffer buf) {
            super.parse(buf);
            alignmentType = buf.get() & 0xff;
        }

        public int getAlignmentType() {
            return alignmentType;
        }
    }
    
    public static class RegistrationDescriptor extends MPEGMediaDescriptor {
        private int formatIdentifier;
        private IntArrayList additionalFormatIdentifiers = new IntArrayList();
        
        @Override
        public void parse(ByteBuffer buf) {
            super.parse(buf);
            formatIdentifier = buf.getInt();
            while(buf.hasRemaining()) {
                additionalFormatIdentifiers.add(buf.get() & 0xff);
            }
        }

        public int getFormatIdentifier() {
            return formatIdentifier;
        }

        public IntArrayList getAdditionalFormatIdentifiers() {
            return additionalFormatIdentifiers;
        }
    }

    public static Class<? extends MPEGMediaDescriptor>[] dMapping = new Class[256];

    static {
        dMapping[2] = VideoStreamDescriptor.class;
        dMapping[3] = AudioStreamDescriptor.class;
        dMapping[6] = DataStreamAlignmentDescriptor.class;
        dMapping[5] = RegistrationDescriptor.class;
        dMapping[10] = ISO639LanguageDescriptor.class;
        dMapping[27] = Mpeg4VideoDescriptor.class;
        dMapping[28] = Mpeg4AudioDescriptor.class;
        dMapping[40] = AVCVideoDescriptor.class;
        dMapping[43] = AACAudioDescriptor.class;
    }

    public static List<MPEGMediaDescriptor> parseDescriptors(ByteBuffer bb) {
        List<MPEGMediaDescriptor> result = new ArrayList<MPEGMediaDescriptor>();
        while (bb.remaining() >= 2) {
            ByteBuffer dup = bb.duplicate();
            int tag = dup.get() & 0xff;
            int len = dup.get() & 0xff;
            ByteBuffer descriptorBuffer = NIOUtils.read(bb, len + 2);
            if (dMapping[tag] != null)
                try {
                    MPEGMediaDescriptor descriptor = dMapping[tag].newInstance();
                    descriptor.parse(descriptorBuffer);
                    result.add(descriptor);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
        }
        return result;
    }
}