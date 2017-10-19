package org.jcodec.containers.dpx;

import static java.nio.ByteBuffer.allocate;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ReadableByteChannel;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.jcodec.common.io.IOUtils;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;

public class DPXReader {
    private static final int READ_BUFFER_SIZE = 2048 + 1024;

    static final int IMAGEINFO_OFFSET = 768;
    static final int IMAGESOURCE_OFFSET = 1408;
    static final int FILM_OFFSET = 1664;
    static final int TVINFO_OFFSET = 1920;

    public static final int SDPX = 0x53445058;
    private final ByteBuffer readBuf;
    private final int magic;
    private boolean eof;


    public DPXReader(SeekableByteChannel ch) throws IOException {
        this.readBuf = allocate(READ_BUFFER_SIZE);
        initialRead(ch);
        this.magic = readBuf.getInt();

        /*
        SMPTE 268M spec. Page 3. From Magic number definition:
        Programs reading DPX files should use the first four bytes to determine the byte order of the file.
        The first four bytes will be S, D, P, X if the byte order is most significant byte first,
        or X, P, D, S if the byte order is least significant byte first.
         */
        if (magic == SDPX) {
            readBuf.order(ByteOrder.BIG_ENDIAN);
        } else {
            readBuf.order(ByteOrder.LITTLE_ENDIAN);
        }
    }

    public DPXMetadata parseMetadata() {
        // File information header
        DPXMetadata dpx = new DPXMetadata();
        dpx.fileInfo = readFileInfo(readBuf);
        dpx.fileInfo.magic = magic;

        // Image information header
        readBuf.position(IMAGEINFO_OFFSET);
        dpx.imageInfo = readImageInfoHeader(readBuf);

        //  Image source information header
        readBuf.position(IMAGESOURCE_OFFSET);
        dpx.imageSource = readImageSourceHeader(readBuf);

        // Motion-picture film information header
        readBuf.position(FILM_OFFSET);
        dpx.filmInfo = readFilmInformationHeader(readBuf);

        // Television information header
        readBuf.position(TVINFO_OFFSET);
        dpx.tvInfo = readTelevisionInfoHeader(readBuf);

        dpx.userId = readNullTermString(readBuf, 32);
        return dpx;
    }

    private void initialRead(ReadableByteChannel ch) throws IOException {
        readBuf.clear();
        if (ch.read(readBuf) == -1)
            eof = true;
        readBuf.flip();
    }

    private static FileInfoHeader readFileInfo(ByteBuffer bb) {
        FileInfoHeader h = new FileInfoHeader();
        h.imageOffset = bb.getInt();
        h.version = readNullTermString(bb, 8);
        h.filesize = bb.getInt();
        h.ditto = bb.getInt();

        h.genericHeaderLength = bb.getInt();
        h.industryHeaderLength = bb.getInt();
        h.userHeaderLength = bb.getInt();

        h.filename = readNullTermString(bb, 100);
        try {
            h.created = readDPXDate(bb, "yyyy:MM:dd:HH:mm:ss:zzz");
        } catch (ParseException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        h.creator = readNullTermString(bb, 100);
        h.projectName = readNullTermString(bb, 200);
        h.copyright = readNullTermString(bb, 200);
        h.encKey = bb.getInt();
        return h;
    }

    private static String readNullTermString(ByteBuffer bb, int length) {
        ByteBuffer b = ByteBuffer.allocate(length);
        bb.get(b.array(), 0, length);
        return NIOUtils.readNullTermString(b);
    }

    public static DPXReader readFile(File file) throws IOException {
        SeekableByteChannel _in = NIOUtils.readableChannel(file);

        try {
            return new DPXReader(_in);
        } finally {
            IOUtils.closeQuietly(_in);
        }
    }

    private static TelevisionInfoHeader readTelevisionInfoHeader(ByteBuffer r) {
        TelevisionInfoHeader h = new TelevisionInfoHeader();
        h.timecode = r.getInt();
        h.userBits = r.getInt();
        h.interlace = r.get();
        h.filedNumber = r.get();
        h.videoSignalStarted = r.get();
        h.zero = r.get();
        h.horSamplingRateHz = r.getInt();
        h.vertSampleRateHz = r.getInt();
        h.frameRate = r.getInt();
        h.timeOffset = r.getInt();
        h.gamma = r.getInt();
        h.blackLevel = r.getInt();
        h.blackGain = r.getInt();
        h.breakpoint = r.getInt();
        h.referenceWhiteLevel = r.getInt();
        h.integrationTime = r.getInt();
//        h.reserved = readBB(r, 76).array();
        return h;
    }

    private static FilmInformationHeader readFilmInformationHeader(ByteBuffer r) {
        FilmInformationHeader h = new FilmInformationHeader();
        h.idCode = readNullTermString(r, 2);
        h.type = readNullTermString(r, 2);
        h.offset = readNullTermString(r, 2);
        h.prefix = readNullTermString(r, 6); // Prefix (6 digits from film edge code)
        h.count = readNullTermString(r, 4); // Count (4 digits from film edge code)
        h.format = readNullTermString(r, 32);
//        h.reserved = readBB(r, 104).array(); // Reserved for future use
        return h;
    }

    private static ImageSourceInfoHeader readImageSourceHeader(ByteBuffer r) {
        ImageSourceInfoHeader h = new ImageSourceInfoHeader();
        h.xOffset = r.getInt();
        h.yOffset = r.getInt();
        h.xCenter = r.getFloat();
        h.yCenter = r.getFloat();
        h.xOriginal = r.getInt();
        h.yOriginal = r.getInt();
        h.sourceImageFilename = readNullTermString(r, 100);

        try {
            h.sourceImageDate = readDPXDate(r, "yyyy:MM:dd:HH:mm:ss:zzz");
        } catch (ParseException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        h.deviceName = readNullTermString(r, 32);
        h.deviceSerial = readNullTermString(r, 32);
        h.borderValidity = new short[] { r.getShort(), r.getShort(), r.getShort(), r.getShort() };
        h.aspectRatio = new int[] { r.getInt(), r.getInt() };
        return h;
    }


    static int bcd2uint(int bcd) {
        int low = bcd & 0xf;
        int high = bcd >> 4;
        if (low > 9 || high > 9)
            return 0;
        return low + 10 * high;
    }

    private static Date readDPXDate(ByteBuffer bb, String pattern) throws ParseException {
        String text = readNullTermString(bb, pattern.length());
        SimpleDateFormat format = new SimpleDateFormat(pattern);
        return format.parse(text);
    }

    private static ImageInformationHeader readImageInfoHeader(ByteBuffer r) {
        ImageInformationHeader h = new ImageInformationHeader();
        // offset = 768
        h.orientation = r.getShort();
        h.numberOfImageElements = r.getShort();
        h.pixelsPerLine = r.getInt();
        h.linesPerImageElement = r.getInt();
        h.imageElement1 = new ImageElement();

        // offset = 780
        h.imageElement1.dataSign = r.getInt();
        return h;
    }

}
