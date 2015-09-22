package org.jcodec.containers.flv;

import java.nio.ByteBuffer;

import org.jcodec.common.AudioFormat;
import org.jcodec.common.Codec;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * FLV packet
 * 
 * @author Stan Vitvitskyy
 * 
 */
public class FLVTag {
    private Type type;
    private long position;
    private TagHeader tagHeader;
    private int pts;
    private ByteBuffer data;
    private boolean keyFrame;
    private long frameNo;
    private int streamId;
    private int prevPacketSize;

    public FLVTag(Type type, long position, TagHeader tagHeader, int pts, ByteBuffer data, boolean keyFrame,
            long frameNo, int streamId, int prevPacketSize) {
        this.type = type;
        this.position = position;
        this.tagHeader = tagHeader;
        this.pts = pts;
        this.data = data;
        this.keyFrame = keyFrame;
        this.frameNo = frameNo;
        this.streamId = streamId;
        this.prevPacketSize = prevPacketSize;
    }

    public static enum Type {
        VIDEO, AUDIO, SCRIPT
    };

    public Type getType() {
        return type;
    }

    public long getPosition() {
        return position;
    }

    public TagHeader getTagHeader() {
        return tagHeader;
    }

    public int getPts() {
        return pts;
    }

    public void setPts(int pts) {
        this.pts = pts;
    }

    public int getStreamId() {
        return streamId;
    }

    public void setStreamId(int streamId) {
        this.streamId = streamId;
    }

    public int getPrevPacketSize() {
        return prevPacketSize;
    }

    public void setPrevPacketSize(int prevPacketSize) {
        this.prevPacketSize = prevPacketSize;
    }

    public ByteBuffer getData() {
        return data;
    }

    public double getPtsD() {
        return ((double) pts) / 1000;
    }

    public boolean isKeyFrame() {
        return keyFrame;
    }

    public long getFrameNo() {
        return frameNo;
    }

    public static class TagHeader {
        private Codec codec;

        public TagHeader(Codec codec) {
            super();
            this.codec = codec;
        }

        public Codec getCodec() {
            return codec;
        }
    }

    public static class VideoTagHeader extends TagHeader {

        private int frameType;

        public VideoTagHeader(Codec codec, int frameType) {
            super(codec);
            this.frameType = frameType;
        }

        public int getFrameType() {
            return frameType;
        }
    }

    public static class AvcVideoTagHeader extends VideoTagHeader {

        private int compOffset;
        private byte avcPacketType;

        public AvcVideoTagHeader(Codec codec, int frameType, byte avcPacketType, int compOffset) {
            super(codec, frameType);
            this.avcPacketType = avcPacketType;
            this.compOffset = compOffset;
        }

        public int getCompOffset() {
            return compOffset;
        }

        public byte getAvcPacketType() {
            return avcPacketType;
        }
    }

    public static class AudioTagHeader extends TagHeader {

        private AudioFormat audioFormat;

        public AudioTagHeader(Codec codec, AudioFormat audioFormat) {
            super(codec);
            this.audioFormat = audioFormat;
        }

        public AudioFormat getAudioFormat() {
            return audioFormat;
        }
    }

    public static class AacAudioTagHeader extends AudioTagHeader {

        private int packetType;

        public AacAudioTagHeader(Codec codec, AudioFormat audioFormat, int packetType) {
            super(codec, audioFormat);
            this.packetType = packetType;
        }

        public int getPacketType() {
            return packetType;
        }
    }
}
