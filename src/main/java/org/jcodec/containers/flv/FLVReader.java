package org.jcodec.containers.flv;
import org.jcodec.common.AudioFormat;
import org.jcodec.common.Codec;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.logging.Logger;
import org.jcodec.common.tools.MathUtil;
import org.jcodec.containers.flv.FLVTag.AacAudioTagHeader;
import org.jcodec.containers.flv.FLVTag.AudioTagHeader;
import org.jcodec.containers.flv.FLVTag.AvcVideoTagHeader;
import org.jcodec.containers.flv.FLVTag.TagHeader;
import org.jcodec.containers.flv.FLVTag.Type;
import org.jcodec.containers.flv.FLVTag.VideoTagHeader;
import org.jcodec.platform.Platform;

import js.io.IOException;
import js.lang.System;
import js.nio.ByteBuffer;
import js.nio.ByteOrder;
import js.nio.channels.ReadableByteChannel;
import js.nio.charset.Charset;
import js.util.Date;
import js.util.HashMap;
import js.util.Map;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * FLV ( Flash Media Video ) demuxer
 * 
 * @author Stan Vitvitskyy
 * 
 */
public class FLVReader {

    private static final int REPOSITION_BUFFER_READS = 10;
    private static final int TAG_HEADER_SIZE = 15;
    // Read buffer, 1M
    private static final int READ_BUFFER_SIZE = 1 << 10;
    private int frameNo;
    private ByteBuffer readBuf;
    private SeekableByteChannel ch;
    private boolean eof;

    private static boolean platformBigEndian = ByteBuffer.allocate(0).getOrder() == ByteOrder.BIG_ENDIAN;

    public static Codec[] audioCodecMapping = new Codec[] { Codec.PCM, Codec.ADPCM, Codec.MP3, Codec.PCM,
            Codec.NELLYMOSER, Codec.NELLYMOSER, Codec.NELLYMOSER, Codec.G711, Codec.G711, null, Codec.AAC, Codec.SPEEX,
            Codec.MP3, null };
    public static Codec[] videoCodecMapping = new Codec[] { null, null, Codec.SORENSON, Codec.FLASH_SCREEN_VIDEO,
            Codec.VP6, Codec.VP6, Codec.FLASH_SCREEN_V2, Codec.H264 };

    public static int[] sampleRates = new int[] { 5500, 11000, 22000, 44100 };

    public FLVReader(SeekableByteChannel ch) throws IOException {
        this.ch = ch;
        readBuf = ByteBuffer.allocate(READ_BUFFER_SIZE);
        readBuf.order(ByteOrder.BIG_ENDIAN);

        initialRead(ch);

        if (!readHeader(readBuf)) {
            // This file doesn't have an FLV header, maybe it's a portion of an
            // FLV file and we can position at the tag start?
            readBuf.setPosition(0);
            if (!repositionFile())
                throw new RuntimeException("Invalid FLV file");
            else {
                Logger.warn(String.format("Parsing a corrupt FLV file, first tag found at %d. %s", readBuf.position(),
                        readBuf.position() == 0 ? "Did you forget the FLV 9-byte header?" : ""));
            }
        }
    }

    private void initialRead(ReadableByteChannel ch) throws IOException {
        readBuf.clear();
        if (ch.read(readBuf) == -1)
            eof = true;
        readBuf.flip();
    }

    public FLVTag readNextPacket() throws IOException {
        if (eof)
            return null;

        FLVTag pkt = parsePacket(readBuf);
        // No more pakets fit into the buffer, reading more data
        if (pkt == null && !eof) {
            moveRemainderToTheStart(readBuf);
            if (ch.read(readBuf) == -1) {
                eof = true;
                return null;
            }

            while (MathUtil.log2(readBuf.capacity()) <= 22) {
                readBuf.flip();
                pkt = parsePacket(readBuf);
                if (pkt != null || readBuf.position() > 0)
                    break;
                // The buffer is too small, getting a bigger one
                ByteBuffer newBuf = ByteBuffer.allocate(readBuf.capacity() << 2);
                newBuf.putBuf(readBuf);
                readBuf = newBuf;
                if (ch.read(readBuf) == -1) {
                    eof = true;
                    return null;
                }
            }
        }

        return pkt;
    }

    public FLVTag readPrevPacket() throws IOException {
        int startOfLastPacket = readBuf.getInt();
        readBuf.setPosition(readBuf.position() - 4);
        if (readBuf.position() > startOfLastPacket) {
            // The previous frame is still in the buffer, so no need to fetch
            readBuf.setPosition(readBuf.position() - startOfLastPacket);
            return parsePacket(readBuf);
        } else {
            // Now we need to fetch the new buffer, because we are unsure of the
            // access pattern we are going to fetch only half of the buffer from
            // the left side and the other half from the right side of the
            // current position
            long oldPos = ch.position() - readBuf.remaining();
            if (oldPos <= 9) {
                // We are at the first frame, there's nowhere to seek
                return null;
            }
            long newPos = Math.max(0, oldPos - readBuf.capacity() / 2);

            ch.setPosition(newPos);
            readBuf.clear();
            ch.read(readBuf);
            readBuf.flip();
            readBuf.setPosition((int) (oldPos - newPos));
            return readPrevPacket();
        }
    }

    private static void moveRemainderToTheStart(ByteBuffer readBuf) {
        int rem = readBuf.remaining();
        for (int i = 0; i < rem; i++) {
            readBuf.putAt(i, readBuf.get());
        }
        readBuf.clear();
        readBuf.setPosition(rem);
    }

    public FLVTag parsePacket(ByteBuffer readBuf) throws IOException {
        for (;;) {
            if (readBuf.remaining() < TAG_HEADER_SIZE) {
                return null;
            }

            int pos = readBuf.position();
            long packetPos = ch.position() - readBuf.remaining();
            int prevPacketSize = readBuf.getInt();
            int packetType = readBuf.get() & 0xff;
            int payloadSize = ((readBuf.getShort() & 0xffff) << 8) | (readBuf.get() & 0xff);
            int timestamp = ((readBuf.getShort() & 0xffff) << 8) | (readBuf.get() & 0xff)
                    | ((readBuf.get() & 0xff) << 24);
            int streamId = ((readBuf.getShort() & 0xffff) << 8) | (readBuf.get() & 0xff);

            // Sanity check and reposition
            if (readBuf.remaining() >= payloadSize + 4) {
                int thisPacketSize = readBuf.getIntAt(readBuf.position() + payloadSize);
                if (thisPacketSize != payloadSize + 11) {
                    readBuf.setPosition(readBuf.position() - TAG_HEADER_SIZE);
                    if (!repositionFile()) {
                        Logger.error(String.format("Corrupt FLV stream at %d, failed to reposition!", packetPos));
                        ch.setPosition(ch.size());
                        eof = true;
                        return null;
                    }
                    Logger.warn(String.format("Corrupt FLV stream at %d, repositioned to %d.", packetPos, ch.position()
                            - readBuf.remaining()));
                    continue;
                }
            }

            if (readBuf.remaining() < payloadSize) {
                readBuf.setPosition(pos);
                return null;
            }

            if (packetType != 0x8 && packetType != 0x9 && packetType != 0x12) {
                NIOUtils.skip(readBuf, payloadSize);
                continue;
            }

            ByteBuffer payload = NIOUtils.clone(NIOUtils.read(readBuf, payloadSize));

            Type type;
            TagHeader tagHeader;
            if (packetType == 0x8) {
                type = Type.AUDIO;
                tagHeader = parseAudioTagHeader(payload.duplicate());
            } else if (packetType == 0x9) {
                type = Type.VIDEO;
                tagHeader = parseVideoTagHeader(payload.duplicate());
            } else if (packetType == 0x12) {
                type = Type.SCRIPT;
                tagHeader = null;
            } else {
                System.out.println("NON AV packet");
                continue;
            }
            boolean keyFrame = packetType == 0x8 || packetType == 9 && ((VideoTagHeader) tagHeader).getFrameType() == 1;

            return new FLVTag(type, packetPos, tagHeader, timestamp, payload, keyFrame, frameNo++, streamId,
                    prevPacketSize);
        }
    }

    public static boolean readHeader(ByteBuffer readBuf) {
        if (readBuf.remaining() < 9 || readBuf.get() != 'F' || readBuf.get() != 'L' || readBuf.get() != 'V'
                || readBuf.get() != 1 || (readBuf.get() & 0x5) == 0 || readBuf.getInt() != 9) {
            return false;
        }
        return true;
    }

    public static FLVMetadata parseMetadata(ByteBuffer bb) {
        if ("onMetaData".equals(readAMFData(bb, -1)))
            return new FLVMetadata((Map<String, Object>) readAMFData(bb, -1));
        return null;
    }

    private static Object readAMFData(ByteBuffer input, int type) {
        if (type == -1) {
            type = input.get() & 0xff;
        }
        switch (type) {
        case 0:
            return input.getDouble();
        case 1:
            return input.get() == 1;
        case 2:
            return readAMFString(input);
        case 3:
            return readAMFObject(input);
        case 8:
            return readAMFEcmaArray(input);
        case 10:
            return readAMFStrictArray(input);
        case 11:
            final Date date = new Date((long) input.getDouble());
            input.getShort(); // time zone
            return date;
        case 13:
            return "UNDEFINED";
        default:
            return null;
        }
    }

    private static Object readAMFStrictArray(ByteBuffer input) {
        int count = input.getInt();
        Object[] result = new Object[count];
        for (int i = 0; i < count; i++) {
            result[i] = readAMFData(input, -1);
        }
        return result;
    }

    private static String readAMFString(ByteBuffer input) {
        int size = input.getShort() & 0xffff;
        return Platform.stringFromCharset(NIOUtils.toArray(NIOUtils.read(input, size)), Charset.forName("UTF-8"));
    }

    private static Object readAMFObject(ByteBuffer input) {
        Map<String, Object> array = new HashMap<String, Object>();
        while (true) {
            String key = readAMFString(input);
            int dataType = input.get() & 0xff;
            if (dataType == 9) { // object end marker
                break;
            }
            array.put(key, readAMFData(input, dataType));
        }
        return array;
    }

    private static Object readAMFEcmaArray(ByteBuffer input) {
        long size = input.getInt();
        Map<String, Object> array = new HashMap<String, Object>();
        for (int i = 0; i < size; i++) {
            String key = readAMFString(input);
            int dataType = input.get() & 0xff;
            array.put(key, readAMFData(input, dataType));
        }
        return array;
    }

    public static VideoTagHeader parseVideoTagHeader(ByteBuffer dup) {
        byte b0 = dup.get();
        int frameType = (b0 & 0xff) >> 4;
        int codecId = (b0 & 0xf);
        Codec codec = videoCodecMapping[codecId];

        if (codecId == 7) {
            byte avcPacketType = dup.get();
            int compOffset = (dup.getShort() << 8) | (dup.get() & 0xff);
            return new AvcVideoTagHeader(codec, frameType, avcPacketType, compOffset);
        }

        return new VideoTagHeader(codec, frameType);
    }

    public static TagHeader parseAudioTagHeader(ByteBuffer dup) {
        byte b = dup.get();

        int codecId = (b & 0xff) >> 4;
        int sampleRate = sampleRates[(b >> 2) & 0x3];
        if (codecId == 4 || codecId == 11)
            sampleRate = 16000;
        if (codecId == 5 || codecId == 14)
            sampleRate = 8000;

        int sampleSizeInBits = (b & 0x2) == 0 ? 8 : 16;
        boolean signed = codecId != 3 && codecId != 0 || sampleSizeInBits == 16;
        int channelCount = 1 + (b & 1);
        if (codecId == 11)
            channelCount = 1;

        AudioFormat audioFormat = new AudioFormat(sampleRate, sampleSizeInBits, channelCount, signed,
                codecId == 3 ? false : platformBigEndian);
        Codec codec = audioCodecMapping[codecId];
        if (codecId == 10) {
            byte packetType = dup.get();
            return new AacAudioTagHeader(codec, audioFormat, packetType);
        }

        return new AudioTagHeader(codec, audioFormat);
    }

    public static int probe(ByteBuffer buf) {
        try {
            readHeader(buf);
            return 100;
        } catch (RuntimeException e) {
            return 0;
        }
    }

    public void reset() throws IOException {
        initialRead(ch);
    }

    public void reposition() throws IOException {
        reset();

        if (!positionAtPacket(readBuf)) {
            throw new RuntimeException("Could not find at FLV tag start");
        }
    }

    public static boolean positionAtPacket(ByteBuffer readBuf) {
        // We will be using the fact that <payload size> = <start of last
        // packet> - 15
        ByteBuffer dup = readBuf.duplicate();
        int payloadSize = 0;
        NIOUtils.skip(dup, 5);
        while (dup.hasRemaining()) {
            payloadSize = ((payloadSize & 0xffff) << 8) | (dup.get() & 0xff);
            int pointerPos = dup.position() + 7 + payloadSize;
            if (dup.position() >= 8 && pointerPos < dup.limit() - 4 && dup.getIntAt(pointerPos) - payloadSize == 11) {
                readBuf.setPosition(dup.position() - 8);
                return true;
            }
        }
        return false;
    }

    /**
     * Searching for the next tag in a file after corrupt segment
     * 
     * @return
     * @throws IOException
     */
    public boolean repositionFile() throws IOException {
        int payloadSize = 0;
        for (int i = 0; i < REPOSITION_BUFFER_READS; i++) {
            while (readBuf.hasRemaining()) {
                payloadSize = ((payloadSize & 0xffff) << 8) | (readBuf.get() & 0xff);
                int pointerPos = readBuf.position() + 7 + payloadSize;
                if (readBuf.position() >= 8 && pointerPos < readBuf.limit() - 4
                        && readBuf.getIntAt(pointerPos) - payloadSize == 11) {
                    readBuf.setPosition(readBuf.position() - 8);
                    return true;
                }
            }
            initialRead(ch);
            if (!readBuf.hasRemaining())
                break;
        }
        return false;
    }
}