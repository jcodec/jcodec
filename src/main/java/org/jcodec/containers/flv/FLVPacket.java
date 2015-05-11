package org.jcodec.containers.flv;

import java.nio.ByteBuffer;

import org.jcodec.common.AudioFormat;
import org.jcodec.common.Codec;
import org.jcodec.common.model.Packet;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * FLV packet
 * 
 * @author Stan Vitvitskyy
 * 
 */
public class FLVPacket extends Packet {
    private Type type;
    private byte[] metadata;
    private long position;
    private TagHeader tagHeader;

    public static enum Type {
        VIDEO, AUDIO
    };

    public FLVPacket(Type type, ByteBuffer data, long pts, long duration, long frameNo, boolean keyFrame,
            byte[] metadata, long position, TagHeader tagHeader) {
        super(data, pts, 1000, duration, frameNo, keyFrame, null);
        this.type = type;
        this.metadata = metadata;
        this.position = position;
        this.tagHeader = tagHeader;
    }

    public Type getType() {
        return type;
    }

    public byte[] getMetadata() {
        return metadata;
    }

    public long getPosition() {
        return position;
    }

    public TagHeader getTagHeader() {
        return tagHeader;
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
