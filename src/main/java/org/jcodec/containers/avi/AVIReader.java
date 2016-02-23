package org.jcodec.containers.avi;

import java.io.IOException;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import org.jcodec.api.FormatException;
import org.jcodec.common.io.DataReader;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.logging.Logger;

import static java.lang.System.currentTimeMillis;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * RIFF 'AVI '            Audio/Video Interleaved file
 *  LIST 'hdrl'        Header LIST
 *      'avih'         Main AVI header
 *      LIST 'strl'    Video stream LIST
 *          'strh'     Video stream header
 *          'strf'     Video format
 *          <Optional Open DML Super Index + List of standard indexes >
 *      LIST 'strl'    Audio stream LIST
 *          'strh'     Audio stream header
 *          'strf'     Audio format
 *          <Optional Open DML Super Index + List of standard indexes >
 *  LIST 'movi'        Main data LIST
 *      '01wb'         Audio data
 *      '00dc'         Video frame
 *      ...
 *  'idx1'             Index    <Optional old Avi Index>
 * 
 * @author Owen McGovern
 */
public class AVIReader {
    public final static int FOURCC_RIFF = 0x46464952; // 'RIFF'
    public final static int FOURCC_AVI = 0x20495641; // 'AVI '
    public final static int FOURCC_AVIX = 0x58495641; // 'AVIX' // extended AVI
    public final static int FOURCC_AVIH = 0x68697661; // 'avih'

    public final static int FOURCC_LIST = 0x5453494c; // 'LIST'

    public final static int FOURCC_HDRL = 0x6c726468; // 'hdrl'

    public final static int FOURCC_JUNK = 0x4b4e554a; // 'JUNK'
    public final static int FOURCC_INDX = 0x78646e69; // 'indx' // main index -
                                                      // old style Avi Index
    public final static int FOURCC_IDXL = 0x31786469; // 'idx1' // index of
                                                      // single 'movi' block
    public final static int FOURCC_STRL = 0x6c727473; // 'strl'
    public final static int FOURCC_STRH = 0x68727473; // 'strh'
    public final static int FOURCC_STRF = 0x66727473; // 'strf'
    public final static int FOURCC_MOVI = 0x69766f6d; // 'movi'
    public final static int FOURCC_REC = 0x20636572; // 'rec '
    public final static int FOURCC_SEGM = 0x6D676573; // 'segm' // some padded
                                                      // that requires adding 1
                                                      // byte to the size given
    public final static int FOURCC_ODML = 0x6C6D646F; // 'odml'

    public final static int FOURCC_VIDS = 0x73646976;
    public final static int FOURCC_AUDS = 0x73647561;
    public final static int FOURCC_MIDS = 0x7364696d;
    public final static int FOURCC_TXTS = 0x73747874;

    public final static int FOURCC_strd = 0x64727473;
    public final static int FOURCC_strn = 0x6e727473;

    public final static int AVIF_HASINDEX = 0x00000010;
    public final static int AVIF_MUSTUSEINDEX = 0x00000020;
    public final static int AVIF_ISINTERLEAVED = 0x00000100;
    public final static int AVIF_TRUSTCKTYPE = 0x00000800;
    public final static int AVIF_WASCAPTUREFILE = 0x00010000;
    public final static int AVIF_COPYRIGHTED = 0x00020000;

    public final static int AVIIF_LIST = 0x00000001;
    public final static int AVIIF_KEYFRAME = 0x00000010;
    public final static int AVIIF_FIRSTPART = 0x00000020;
    public final static int AVIIF_LASTPART = 0x00000040;
    public final static int AVIIF_NOTIME = 0x00000100;

    public final static int AUDIO_FORMAT_PCM = 0x0001;
    public final static int AUDIO_FORMAT_MP3 = 0x0055;
    public final static int AUDIO_FORMAT_AC3 = 0x2000;
    public final static int AUDIO_FORMAT_DTS = 0x2001;
    public final static int AUDIO_FORMAT_VORBIS = 0x566F;
    public final static int AUDIO_FORMAT_EXTENSIBLE = 0xFFFE;

    // Open DML Index type codes
    public final int AVI_INDEX_OF_INDEXES = 0x00;
    public final int AVI_INDEX_OF_CHUNKS = 0x01;
    public final int AVI_INDEX_OF_TIMED_CHUNKS = 0x02;
    public final int AVI_INDEX_OF_SUB_2FIELD = 0x03;
    public final int AVI_INDEX_IS_DATA = 0x80;

    public final static int STDINDEXSIZE = 0x4000;

    private final static long SIZE_MASK = 0xffffffffL; // For conversion of
                                                       // sizes from unsigned
                                                       // int to long

    private DataReader raf = null;
    private long fileLeft = 0;

    private AVITag_AVIH aviHeader;
    private AVITag_STRH[] streamHeaders;
    private AVIChunk[] streamFormats;

    private List<AVITag_AviIndex> aviIndexes = new ArrayList<AVITag_AviIndex>();
    private AVITag_AviDmlSuperIndex[] openDmlSuperIndex;

    private PrintStream ps = null;
    private boolean skipFrames = true; // DEBUG MODE ONLY, don't read the
                                       // video/audio byte data, just skip over
                                       // it

    public AVIReader(SeekableByteChannel src) {
        this.raf = new DataReader(src, ByteOrder.LITTLE_ENDIAN);
    }

    public static int fromFourCC(final String str) {
        byte[] strBytes = str.getBytes();
        if (strBytes.length != 4)
            throw new IllegalArgumentException("Expected 4 bytes not " + strBytes.length);

        int fourCCInt = strBytes[3];
        fourCCInt = (fourCCInt <<= 8) | strBytes[2];
        fourCCInt = (fourCCInt <<= 8) | strBytes[1];
        fourCCInt = (fourCCInt <<= 8) | strBytes[0];

        return (fourCCInt);
    }

    /**
     * 
     * @param fourcc
     * @return
     */
    public static String toFourCC(int fourcc) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            int c = fourcc & 0xff;
            sb.append(Character.toString((char) c));
            fourcc >>= 8;
        }
        return sb.toString();
    }

    public long getFileLeft() throws IOException {
        return (this.fileLeft);
    }

    public List<AVITag_AviIndex> getAviIndexes() {
        return (this.aviIndexes);
    }

    public void parse() throws IOException {
        try {
            long t1 = currentTimeMillis();

            long fileSize = raf.size();
            fileLeft = fileSize;

            int numStreams = 0;
            int streamIndex = -1;
            int videoFrameNo = 1;

            // Read the FOURCC tag code

            int dwFourCC = raf.readInt();
            if (dwFourCC != FOURCC_RIFF)
                throw new FormatException("No RIFF header found");

            AVIChunk aviItem = new AVIList();
            aviItem.read(dwFourCC, raf);
            Logger.debug(aviItem.toString());

            int previousStreamType = 0;
            do {
                dwFourCC = raf.readInt();
                String dwFourCCStr = toFourCC(dwFourCC);

                switch (dwFourCC) {
                case FOURCC_RIFF: {
                    aviItem = new AVIList();
                    aviItem.read(dwFourCC, raf);

                    /*
                     * if (((AVIList)aviItem).getListType() == FOURCC_AVIX) {
                     * this.logger.debug("- Extended RIFF AVIX Found"); }
                     */
                    break;
                }

                case FOURCC_LIST: {
                    aviItem = new AVIList();
                    aviItem.read(dwFourCC, raf);

                    if (((AVIList) aviItem).getListType() == FOURCC_MOVI) {
                        // this.logger.debug("- Skipping LIST Movi...");
                        aviItem.skip(raf);
                    }
                    break;
                }

                case FOURCC_STRL: {
                    aviItem = new AVIList();
                    aviItem.read(dwFourCC, raf);
                    break;
                }

                case FOURCC_AVIH: {
                    aviItem = aviHeader = new AVITag_AVIH();
                    aviItem.read(dwFourCC, raf);

                    numStreams = aviHeader.getStreams();

                    streamHeaders = new AVITag_STRH[numStreams];
                    streamFormats = new AVIChunk[numStreams];
                    openDmlSuperIndex = new AVITag_AviDmlSuperIndex[numStreams];
                    break;
                }

                case FOURCC_STRH: {
                    if (streamIndex >= numStreams) {
                        throw new IllegalStateException("Read more stream headers than expected, expected ["
                                + numStreams + "]");
                    }

                    streamIndex++;

                    aviItem = streamHeaders[streamIndex] = new AVITag_STRH();
                    aviItem.read(dwFourCC, raf);
                    previousStreamType = ((AVITag_STRH) aviItem).getType();

                    break;
                }

                case FOURCC_STRF: {
                    // The previous FOURCC_STRH should precede this FOURCC_STRF

                    switch (previousStreamType) {
                    case FOURCC_VIDS: {
                        aviItem = streamFormats[streamIndex] = new AVITag_BitmapInfoHeader();
                        aviItem.read(dwFourCC, raf);
                        break;
                    }

                    case FOURCC_AUDS: {
                        aviItem = streamFormats[streamIndex] = new AVITag_WaveFormatEx();
                        aviItem.read(dwFourCC, raf);
                        break;
                    }

                    default: {
                        throw new IOException("Expected vids or auds got [" + toFourCC(previousStreamType) + "]");
                    }
                    }
                    break;
                }

                case FOURCC_SEGM: {
                    aviItem = new AVI_SEGM();
                    aviItem.read(dwFourCC, raf);

                    aviItem.skip(raf);
                    break;
                }

                case FOURCC_IDXL: {
                    // Old style AVI Index
                    aviItem = new AVITag_AviIndex();
                    aviItem.read(dwFourCC, raf);

                    aviIndexes.add((AVITag_AviIndex) aviItem);
                    break;
                }

                case FOURCC_INDX: {
                    // Open DML style Index ( super index )

                    openDmlSuperIndex[streamIndex] = new AVITag_AviDmlSuperIndex();
                    openDmlSuperIndex[streamIndex].read(dwFourCC, raf);

                    aviItem = openDmlSuperIndex[streamIndex];
                    break;
                }

                default: {
                    // Chunks where the FOURCC str is not constant, eg. includes
                    // a stream no like "ix00", "ix01" etc

                    if (dwFourCCStr.endsWith("db")) {
                        // uncompressed video chunk
                        aviItem = new AVITag_VideoChunk(false, raf);
                        aviItem.read(dwFourCC, raf);

                        if (skipFrames) {
                            aviItem.skip(raf);
                        } else {
                            byte[] videoFrameData = ((AVITag_VideoChunk) aviItem).getVideoPacket();
                            ByteBuffer bb = ByteBuffer.wrap(videoFrameData);

                            // TODO : Decode uncompressed video data
                        }
                    } else if (dwFourCCStr.endsWith("dc")) {
                        // compressed video chunk
                        aviItem = new AVITag_VideoChunk(true, raf);
                        aviItem.read(dwFourCC, raf);

                        ((AVITag_VideoChunk) aviItem).setFrameNo(videoFrameNo);
                        videoFrameNo++;

                        String fourccStr = toFourCC(dwFourCC);
                        int streamNo = Integer.parseInt(fourccStr.substring(0, 2));

                        if (skipFrames) {
                            aviItem.skip(raf);
                        } else {
                            byte[] videoFrameData = ((AVITag_VideoChunk) aviItem).getVideoPacket();
                            ByteBuffer bb = ByteBuffer.wrap(videoFrameData);

                            // TODO: Decode compressed video data
                            // Look up the stream header from streamNo to find
                            // the codec
                        }
                    } else if (dwFourCCStr.endsWith("wb")) {
                        // audio chunk
                        aviItem = new AVITag_AudioChunk();
                        aviItem.read(dwFourCC, raf);
                        aviItem.skip(raf);
                    } else if (dwFourCCStr.endsWith("tx")) {
                        // subtitle chunk
                        aviItem = new AVIChunk();
                        aviItem.read(dwFourCC, raf);
                        aviItem.skip(raf);
                    } else if (dwFourCCStr.startsWith("ix")) {
                        // New style OpenDML AVI Indexes
                        aviItem = new AVITag_AviDmlStandardIndex();
                        aviItem.read(dwFourCC, raf);
                    }
                    /*
                     * else if ( (dwFourCC==FOURCC_IDXL) ||
                     * (dwFourCCStr.startsWith("ix")) ) { if (!openDmlIndexes) {
                     * // Old style AVI Index aviItem = new AVITag_AviIndex();
                     * aviItem.read(dwFourCC, raf); } else { // New style
                     * OpenDML AVI Indexes aviItem = new AVITag_AviDmlIndex();
                     * aviItem.read(dwFourCC, raf); } }
                     */
                    else {
                        // Some unknown chunk we will skip

                        aviItem = new AVIChunk();
                        aviItem.read(dwFourCC, raf);
                        aviItem.skip(raf);
                    }

                    break;
                }
                }

                Logger.debug(aviItem.toString());

                fileLeft = fileSize - raf.position();

            } while (fileLeft > 0);

            long t2 = currentTimeMillis();

            Logger.debug("\tFile Left [" + fileLeft + "]");
            Logger.debug("\tParse time : " + (t2 - t1) + "ms");

        } finally {
            if (ps != null) {
                ps.close();
            }
        }
    }

    /*
     * Generic AVI Chunk class
     */
static class AVIChunk {
        protected int dwFourCC;
        protected String fwFourCCStr;
        protected int dwChunkSize;
        protected long startOfChunk;

        public void read(final int dwFourCC, final DataReader raf) throws IOException {
            startOfChunk = raf.position() - 4; // Add four as we've
                                                     // already read the
                                                     // dwFourCC DWORD flag

            this.dwFourCC = dwFourCC;
            this.fwFourCCStr = AVIReader.toFourCC(dwFourCC);
            dwChunkSize = raf.readInt();
        }

        public long getStartOfChunk() {
            return (this.startOfChunk);
        }

        public long getEndOfChunk() {
            return (startOfChunk + 8 + getChunkSize()); // 2 x DWORD (dwFourCC &
                                                        // dwChunkSize)
        }

        public int getFourCC() {
            return (dwFourCC);
        }

        public void skip(final DataReader raf) throws IOException {
            int chunkSize = getChunkSize();
            if (chunkSize < 0)
                throw new IOException("Negative chunk size for chunk [" + toFourCC(this.dwFourCC) + "]");

            raf.skipBytes(chunkSize);
        }

        public int getChunkSize() {
            // Chunks are padded to 2 byte alignments
            // If the size is an odd number, then add one

            // Chunk alignment to word
            if ((dwChunkSize & 1) == 1)
                return (dwChunkSize + 1);
            else
                return (dwChunkSize);

        }

        @Override
        public String toString() {
            String chunkStr = toFourCC(dwFourCC);
            if (chunkStr.trim().length() == 0) {
                chunkStr = Integer.toHexString(dwFourCC);
            }

            return ("\tCHUNK [" + chunkStr + "], Size [" + dwChunkSize + "], StartOfChunk [" + getStartOfChunk() + "]");
        }
    }

    /*
     * Generic AVI List class
     */
static class AVIList extends AVIChunk {
        protected int dwListTypeFourCC;
        protected String dwListTypeFourCCStr;

        @Override
        public void read(final int dwFourCC, final DataReader raf) throws IOException {
            super.read(dwFourCC, raf);

            dwChunkSize -= 4; // Correct for next field being _in between the
                              // size and the data
            dwListTypeFourCC = raf.readInt();
            dwListTypeFourCCStr = AVIReader.toFourCC(dwListTypeFourCC);
        }

        public int getListType() {
            return (dwListTypeFourCC);
        }

        /*
         * @Override public void skip(final DataReader raf) throws
         * IOException { // Don't skip, each list contains N number of AviChunks
         * // raf.skipBytes(0); }
         */

        @Override
        public String toString() {
            String dwFourCCStr = toFourCC(this.dwFourCC);

            return (dwFourCCStr + " [" + dwListTypeFourCCStr + "], Size [" + dwChunkSize + "], StartOfChunk ["
                    + getStartOfChunk() + "]");
        }
    }

static class AVI_SEGM extends AVIChunk {
        @Override
        public void read(final int dwFourCC, final DataReader raf) throws IOException {
            super.read(dwFourCC, raf);
        }

        @Override
        public int getChunkSize() {
            // Segment hack
            if (dwChunkSize == 0)
                return (0);
            else
                return (dwChunkSize + 1); // Not sure dwSize is always 1, or
                                          // signifies a 2-byte alignment
        }

        @Override
        public String toString() {
            return ("SEGMENT Align, Size [" + dwChunkSize + "], StartOfChunk [" + getStartOfChunk() + "]");
        }
    }

static class AVITag_AVIH extends AVIChunk {
        // public byte[] fcc = new byte[]{'a','v','i','h'};

        public String getHeight;
        final static int AVIF_HASINDEX = 0x00000010; // Index at end of file?
        final static int AVIF_MUSTUSEINDEX = 0x00000020;
        final static int AVIF_ISINTERLEAVED = 0x00000100;
        final static int AVIF_TRUSTCKTYPE = 0x00000800; // Use CKType to find
                                                        // key frames
        final static int AVIF_WASCAPTUREFILE = 0x00010000;
        final static int AVIF_COPYRIGHTED = 0x00020000;

        private int dwMicroSecPerFrame; // (1 / frames per sec) * 1,000,000
        private int dwMaxBytesPerSec;
        private int dwPaddingGranularity;
        private int dwFlags;
        private int dwTotalFrames; // replace with correct value
        private int dwInitialFrames;
        private int dwStreams;
        private int dwSuggestedBufferSize;
        private int dwWidth; // replace with correct value
        private int dwHeight; // replace with correct value
        private int[] dwReserved = new int[4];

        @Override
        public void read(final int dwFourCC, final DataReader raf) throws IOException {
            super.read(dwFourCC, raf);
            if (dwFourCC != FOURCC_AVIH)
                throw new IOException("Unexpected AVI header : " + toFourCC(dwFourCC));

            if (getChunkSize() != 56)
                throw new IOException("Expected dwSize=56");

            dwMicroSecPerFrame = raf.readInt();
            dwMaxBytesPerSec = raf.readInt();
            dwPaddingGranularity = raf.readInt();
            dwFlags = raf.readInt();
            dwTotalFrames = raf.readInt();
            dwInitialFrames = raf.readInt();
            dwStreams = raf.readInt();
            dwSuggestedBufferSize = raf.readInt();
            dwWidth = raf.readInt();
            dwHeight = raf.readInt();
            dwReserved[0] = raf.readInt();
            dwReserved[1] = raf.readInt();
            dwReserved[2] = raf.readInt();
            dwReserved[3] = raf.readInt();
        }

        public int getWidth() {
            return dwWidth;
        }

        public int getHeight() {
            return dwHeight;
        }

        public int getStreams() {
            return (dwStreams);
        }

        public int getTotalFrames() {
            return (dwTotalFrames);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();

            if ((dwFlags & AVIF_HASINDEX) != 0)
                sb.append("HASINDEX ");
            if ((dwFlags & AVIF_MUSTUSEINDEX) != 0)
                sb.append("MUSTUSEINDEX ");
            if ((dwFlags & AVIF_ISINTERLEAVED) != 0)
                sb.append("ISINTERLEAVED ");
            if ((dwFlags & AVIF_WASCAPTUREFILE) != 0)
                sb.append("AVIF_WASCAPTUREFILE ");
            if ((dwFlags & AVIF_COPYRIGHTED) != 0)
                sb.append("AVIF_COPYRIGHTED ");

            return ("AVIH Resolution [" + this.dwWidth + "x" + this.dwHeight + "], NumFrames [" + this.dwTotalFrames
                    + "], Flags [" + Integer.toHexString(dwFlags) + "] - [" + sb.toString().trim() + "]");
        }
    }

static class AVITag_STRH extends AVIChunk {
        final static int AVISF_DISABLED = 0x00000001;
        final static int AVISF_VIDEO_PALCHANGES = 0x00010000;

        private int fccType; // private byte[] fccType = new
                             // byte[]{'v','i','d','s'};
        private int fccCodecHandler; // private byte[] fccHandler = new
                                     // byte[]{'M','J','P','G'};
        private int dwFlags = 0;
        private short wPriority = 0;
        private short wLanguage = 0;
        private int dwInitialFrames = 0;
        private int dwScale = 0; // microseconds per frame
        private int dwRate = 1000000; // dwRate / dwScale = frame rate
        private int dwStart = 0;
        private int dwLength = 0; // num frames
        private int dwSuggestedBufferSize = 0;
        private int dwQuality = -1;
        private int dwSampleSize = 0;
        private short left = 0;
        private short top = 0;
        private short right = 0;
        private short bottom = 0;

        @Override
        public void read(final int dwFourCC, final DataReader raf) throws IOException {
            super.read(dwFourCC, raf);
            if (dwFourCC != FOURCC_STRH)
                throw new IOException("Expected 'strh' fourcc got [" + toFourCC(this.dwFourCC) + "]");

            fccType = raf.readInt();

            fccCodecHandler = raf.readInt();
            dwFlags = raf.readInt();

            wPriority = raf.readShort();
            wLanguage = raf.readShort();

            dwInitialFrames = raf.readInt();
            dwScale = raf.readInt();
            dwRate = raf.readInt();
            dwStart = raf.readInt();
            dwLength = raf.readInt();
            dwSuggestedBufferSize = raf.readInt();
            dwQuality = raf.readInt();
            dwSampleSize = raf.readInt();

            left = raf.readShort();
            top = raf.readShort();
            right = raf.readShort();
            bottom = raf.readShort();
        }

        public int getType() {
            return (fccType);
        }

        public int getHandler() {
            return (fccCodecHandler);
        }

        public String getHandlerStr() {
            if (fccCodecHandler != 0)
                return (toFourCC(fccCodecHandler));
            else
                return ("");
        }

        public int getInitialFrames() {
            return dwInitialFrames;
        }

        @Override
        public String toString() {
            return ("\tCHUNK [" + toFourCC(this.dwFourCC) + "], Type["
                    + (fccType > 0 ? toFourCC(this.fccType) : "    ") + "], Handler ["
                    + (fccCodecHandler > 0 ? toFourCC(this.fccCodecHandler) : "    ") + "]");
        }
    }

    /**
     * typedef struct tagBITMAPINFOHEADER { DWORD biSize; LONG biWidth; LONG
     * biHeight; WORD biPlanes; WORD biBitCount; DWORD biCompression; DWORD
     * biSizeImage; LONG biXPelsPerMeter; LONG biYPelsPerMeter; DWORD biClrUsed;
     * DWORD biClrImportant; } BITMAPINFOHEADER;
     */
static class AVITag_BitmapInfoHeader extends AVIChunk {
        private int biSize;
        private int biWidth; // long
        private int biHeight; // long
        private short biPlanes;
        private short biBitCount;
        private int biCompression;
        private int biSizeImage;
        private int biXPelsPerMeter; // long
        private int biYPelsPerMeter; // long
        private int biClrUsed;
        private int biClrImportant;

        // Optional palette info ( number of colours I believe )
        private byte r;
        private byte g;
        private byte b;
        private byte x;

        @Override
        public void read(final int dwFourCC, final DataReader raf) throws IOException {
            super.read(dwFourCC, raf);

            biSize = raf.readInt();
            biWidth = raf.readInt();
            biHeight = raf.readInt();
            biPlanes = raf.readShort();
            biBitCount = raf.readShort();
            biCompression = raf.readInt();
            biSizeImage = raf.readInt();
            biXPelsPerMeter = raf.readInt();
            biYPelsPerMeter = raf.readInt();
            biClrUsed = raf.readInt();
            biClrImportant = raf.readInt();

            if (this.getChunkSize() == 56) // Normal size is 40, plus optional
                                           // extra 4 dwords for palette info =
                                           // 16 bytes)
            {
                r = raf.readByte(); // readSh3ort(raf);
                g = raf.readByte();
                b = raf.readByte();
                x = raf.readByte();
            }

            /*
             * logger.debug("\t\t\t- Compression    [" + toFourCC(biCompression)
             * + "]"); logger.debug("\t\t\t- Bits Per Pixel [" + biBitCount +
             * "]"); logger.debug("\t\t\t- Resolution     [" + (biWidth &
             * SIZE_MASK) + " x " + ( biHeight & SIZE_MASK) + "]");
             * logger.debug("\t\t\t- Planes         [" + biPlanes + "]");
             */
        }

        @Override
        public int getChunkSize() {
            return (biSize);
        }

        @Override
        public String toString() {
            return ("\tCHUNK [" + toFourCC(dwFourCC) + "], BitsPerPixel [" + biBitCount + "], Resolution ["
                    + (biWidth & SIZE_MASK) + " x " + (biHeight & SIZE_MASK) + "], Planes [" + biPlanes + "]");
        }
    }

    /*
     * typedef struct { WORD wFormatTag; WORD nChannels; DWORD nSamplesPerSec;
     * DWORD nAvgBytesPerSec; WORD nBlockAlign; } WAVEFORMAT;
     * 
     * typedef struct { WORD wFormatTag; WORD nChannels; DWORD nSamplesPerSec;
     * DWORD nAvgBytesPerSec; WORD nBlockAlign;
     * 
     * WORD wBitsPerSample; WORD cbSize; } WAVEFORMATEX;
     * 
     * typedef struct _GUID { DWORD Data1; WORD Data2; WORD Data3; BYTE
     * Data4[8]; } GUID;
     * 
     * 
     * typedef struct { WAVEFORMATEX Format; union { WORD wValidBitsPerSample;
     * // bits of precision WORD wSamplesPerBlock; // valid if wBitsPerSample==0
     * WORD wReserved; // If neither applies, set to zero. } Samples; DWORD
     * dwChannelMask; // which channels are present in stream GUID SubFormat; }
     * WAVEFORMATEXTENSIBLE, *PWAVEFORMATEXTENSIBLE;
     */

static class AVITag_WaveFormatEx extends AVIChunk {
        public final static int SPEAKER_FRONT_LEFT = 0x1;
        public final static int SPEAKER_FRONT_RIGHT = 0x2;
        public final static int SPEAKER_FRONT_CENTER = 0x4;
        public final static int SPEAKER_LOW_FREQUENCY = 0x8;
        public final static int SPEAKER_BACK_LEFT = 0x10;
        public final static int SPEAKER_BACK_RIGHT = 0x20;
        public final static int SPEAKER_FRONT_LEFT_OF_CENTER = 0x40;
        public final static int SPEAKER_FRONT_RIGHT_OF_CENTER = 0x80;
        public final static int SPEAKER_BACK_CENTER = 0x100;
        public final static int SPEAKER_SIDE_LEFT = 0x200;
        public final static int SPEAKER_SIDE_RIGHT = 0x400;
        public final static int SPEAKER_TOP_CENTER = 0x800;
        public final static int SPEAKER_TOP_FRONT_LEFT = 0x1000;
        public final static int SPEAKER_TOP_FRONT_CENTER = 0x2000;
        public final static int SPEAKER_TOP_FRONT_RIGHT = 0x4000;
        public final static int SPEAKER_TOP_BACK_LEFT = 0x8000;
        public final static int SPEAKER_TOP_BACK_CENTER = 0x10000;
        public final static int SPEAKER_TOP_BACK_RIGHT = 0x20000;

        // WaveFormat
        protected short wFormatTag;
        protected short channels;
        protected int nSamplesPerSec;
        protected int nAvgBytesPerSec;
        protected short nBlockAlign;

        // WaveFormatEx // WaveFormat PCM
        protected short wBitsPerSample;

        // WaveFormatEx
        protected short cbSize;

        // WaveFormatExtensible
        protected short wValidBitsPerSample;
        protected short samplesValidBitsPerSample;
        protected short wReserved;

        protected int channelMask;
        protected int guid_data1;
        protected short guid_data2;
        protected short guid_data3;
        protected byte[] guid_data4 = new byte[8];

        // Optional MP3 parameters if wFormatTag = 0x0055
        protected boolean mp3Flag = false;
        protected short wID;
        protected int fdwFlags;
        protected short nBlockSize;
        protected short nFramesPerBlock;
        protected short nCodecDelay;

        private String audioFormat = "?";

        @Override
        public void read(final int dwFourCC, final DataReader raf) throws IOException {
            super.read(dwFourCC, raf);

            // WAVEFORMAT Fields
            wFormatTag = raf.readShort();
            channels = raf.readShort();
            nSamplesPerSec = raf.readInt();
            nAvgBytesPerSec = raf.readInt();
            nBlockAlign = raf.readShort();

            switch ((int) wFormatTag) {
            // See mmreg.h for a list of all audio format tags

            case AUDIO_FORMAT_PCM: {
                wBitsPerSample = raf.readShort();

                if (dwChunkSize == 40) {
                    // Simulate a C union struct
                    wValidBitsPerSample = samplesValidBitsPerSample = wReserved = raf.readShort();
                    cbSize = raf.readShort();

                    channelMask = raf.readInt();

                    // GUID SubFormat
                    guid_data1 = raf.readInt();
                    guid_data2 = raf.readShort();
                    guid_data3 = raf.readShort();
                    raf.readFully(guid_data4);
                }
                audioFormat = "PCM";
                break;
            }

            case AUDIO_FORMAT_MP3: {
                // WaveFormatEX
                wBitsPerSample = raf.readShort();
                cbSize = raf.readShort();

                wID = raf.readShort();
                fdwFlags = raf.readInt();
                nBlockSize = raf.readShort();
                nFramesPerBlock = raf.readShort();
                nCodecDelay = raf.readShort();

                mp3Flag = true;
                audioFormat = "MP3";
                break;
            }

            case AUDIO_FORMAT_AC3: {
                audioFormat = "AC3";
                break;
            }

            case AUDIO_FORMAT_DTS: {
                audioFormat = "DTS";
                break;
            }

            case AUDIO_FORMAT_VORBIS: {
                audioFormat = "VORBIS";
                break;
            }

            case AUDIO_FORMAT_EXTENSIBLE: {
                // WaveFormatEX
                wBitsPerSample = raf.readShort();
                cbSize = raf.readShort();

                // WaveFormat Extensible

                // Simulate a C union struct
                wValidBitsPerSample = samplesValidBitsPerSample = wReserved = raf.readShort();

                channelMask = raf.readInt();

                // GUID SubFormat
                guid_data1 = raf.readInt();
                guid_data2 = raf.readShort();
                guid_data3 = raf.readShort();
                raf.readFully(guid_data4);

                audioFormat = "EXTENSIBLE";
                break;
            }

            default: {
                audioFormat = "Unknown : " + Integer.toHexString(wFormatTag);
                break;
            }
            }

        }

        public boolean isMP3() {
            return (mp3Flag);
        }

        public short getCbSize() {
            return (this.cbSize);
        }

        @Override
        public String toString() {
            return (String
                    .format("\tCHUNK [%s], ChunkSize [%d], Format [%s], Channels [%d], Channel Mask [%s], MP3 [%b], SamplesPerSec [%d], nBlockAlign [%d]",
                            toFourCC(dwFourCC), getChunkSize(), audioFormat, channels,
                            Integer.toHexString(channelMask), mp3Flag, this.nSamplesPerSec, this.getStartOfChunk(),
                            this.nBlockAlign));
        }
    }

    /*
     * Subtitle
     * 
     * char[4]; // 'GAB2' BYTE 0x00; WORD 0x02; // unicode DWORD dwSize_name; //
     * length of stream name in bytes char name[dwSize_name]; // zero-terminated
     * subtitle stream name encoded in UTF-16 WORD 0x04; DWORD dwSize; // size
     * of SRT/SSA text file char data[dwSize]; // entire SRT/SSA file
     */

static class AVITag_VideoChunk extends AVIChunk {
        protected int streamNo;
        protected boolean compressed = false;
        protected int frameNo = -1;
		private DataReader raf;

        public AVITag_VideoChunk(final boolean compressed, DataReader raf) {
            super();

            this.compressed = compressed;
			this.raf = raf;
        }

        public int getStreamNo() {
            return (streamNo);
        }

        public void setFrameNo(final int frameNo) {
            this.frameNo = frameNo;
        }

        @Override
        public int getChunkSize() {
            // Chunks are padded to 2 byte alignments
            // If the size is an odd number, then add one

            // Chunk alignment
            if ((dwChunkSize & 1) == 1)
                return (dwChunkSize + 1);
            else
                return (dwChunkSize);
        }

        @Override
        public void read(final int dwFourCC, final DataReader raf) throws IOException {
            super.read(dwFourCC, raf);

            String fourccStr = toFourCC(dwFourCC);
            streamNo = Integer.parseInt(fourccStr.substring(0, 2));
        }

        public byte[] getVideoPacket() throws IOException {
            byte[] videoFrameData = new byte[dwChunkSize];

            int bytesRead = raf.readFully(videoFrameData);
            if (bytesRead != dwChunkSize)
                throw new IOException("Read mismatch expected chunksize [" + dwChunkSize + "], Actual read ["
                        + bytesRead + "]");

            int alignment = getChunkSize() - dwChunkSize;
            if (alignment > 0)
                raf.skipBytes(alignment);

            return (videoFrameData);

        }

        @Override
        public String toString() {
            return ("\tVIDEO CHUNK - Stream " + streamNo + ",  chunkStart=" + this.getStartOfChunk() + ", "
                    + (compressed ? "compressed" : "uncompressed") + ", ChunkSize=" + getChunkSize() + ", FrameNo=" + this.frameNo);
        }
    }

static class AVITag_AudioChunk extends AVIChunk {
        protected int streamNo;
		private DataReader raf;

        @Override
        public void read(final int dwFourCC, final DataReader raf) throws IOException {
            this.raf = raf;
			super.read(dwFourCC, raf);

            String fourccStr = toFourCC(dwFourCC);
            streamNo = Integer.parseInt(fourccStr.substring(0, 2));
        }

        @Override
        public int getChunkSize() {
            // Chunks are padded to 2 byte alignments
            // If the size is an odd number, then add one

            // Chunk alignment
            if ((dwChunkSize & 1) == 1)
                return (dwChunkSize + 1);
            else
                return (dwChunkSize);
        }

        public byte[] getAudioPacket() throws IOException {
            byte[] audioFrameData = new byte[dwChunkSize];

            int bytesRead = raf.readFully(audioFrameData);
            if (bytesRead != dwChunkSize)
                throw new IOException("Read mismatch expected chunksize [" + dwChunkSize + "], Actual read ["
                        + bytesRead + "]");

            int alignment = getChunkSize() - dwChunkSize;
            if (alignment > 0)
                raf.skipBytes(alignment);

            return (audioFrameData);
        }

        @Override
        public String toString() {
            return ("\tAUDIO CHUNK - Stream " + streamNo + ", StartOfChunk=" + this.getStartOfChunk() + ", ChunkSize=" + getChunkSize());
        }
    }

    /**
     * AVIINDEXENTRY index_entry[n]
     * 
     * typedef struct { DWORD ckid; DWORD dwFlags; DWORD dwChunkOffset; DWORD
     * dwChunkLength; } AVIINDEXENTRY;
     * 
     * // Flag bitmasks #define AVIIF_LIST 0x00000001 #define AVIIF_KEYFRAME
     * 0x00000010 #define AVIIF_NO_TIME 0x00000100 #define AVIIF_COMPRESSOR
     * 0x0FFF0000 // unused?
     */
static class AVITag_AviIndex extends AVIChunk {
        protected int numIndexes = 0;
        protected int[] ckid;
        protected int[] dwFlags;
        protected int[] dwChunkOffset;
        protected int[] dwChunkLength;

        @Override
        public void read(final int dwFourCC, final DataReader raf) throws IOException {
            super.read(dwFourCC, raf);

            numIndexes = this.getChunkSize() >> 4;

            ckid = new int[numIndexes];
            dwFlags = new int[numIndexes];
            dwChunkOffset = new int[numIndexes];
            dwChunkLength = new int[numIndexes];

            for (int i = 0; i < numIndexes; i++) {
                ckid[i] = raf.readInt(); // raf.readInt();
                dwFlags[i] = raf.readInt(); // raf.readInt();
                dwChunkOffset[i] = raf.readInt(); // raf.readInt();
                dwChunkLength[i] = raf.readInt(); // raf.readInt();
            }

            raf.position(this.getEndOfChunk());

            int alignment = getChunkSize() - dwChunkSize;
            if (alignment > 0)
                raf.skipBytes(alignment);
        }

        public int getNumIndexes() {
            return numIndexes;
        }

        public int[] getCkid() {
            return ckid;
        }

        public int[] getDwFlags() {
            return dwFlags;
        }

        public int[] getDwChunkOffset() {
            return dwChunkOffset;
        }

        public int[] getDwChunkLength() {
            return dwChunkLength;
        }

        public void debugOut() {
            for (int i = 0; i < numIndexes; i++) {
                Logger.debug("\t");
            }

        }

        @Override
        public String toString() {
            return (String.format("\tAvi Index List, StartOfChunk [%d], ChunkSize [%d], NumIndexes [%d]",
                    this.getStartOfChunk(), this.dwChunkSize, (getChunkSize() >> 4)));
        }

    }

    /**
     * typedef struct _avisuperindex_chunk { FOURCC fcc; DWORD cb; WORD
     * wLongsPerEntry; BYTE bIndexSubType; BYTE bIndexType; DWORD nEntriesInUse;
     * DWORD dwChunkId; DWORD dwReserved[3]; struct _avisuperindex_entry {
     * __int64 qwOffset; DWORD dwSize; DWORD dwDuration; } aIndex[ ]; }
     * AVISUPERINDEX;
     * 
     * #define STDINDEXSIZE 0x4000 #define NUMINDEX(wLongsPerEntry)
     * ((STDINDEXSIZE-32)/4/(wLongsPerEntry)) #define
     * NUMINDEXFILL(wLongsPerEntry) ((STDINDEXSIZE/4) -
     * NUMINDEX(wLongsPerEntry))
     */

    // Open DML style AVI Super Index. Extension index when the first is not
    // enough, mainly for files > 1gb
static class AVITag_AviDmlSuperIndex extends AVIChunk {
        // Read
        protected short wLongsPerEntry;
        protected byte bIndexSubType;
        protected byte bIndexType;
        protected int nEntriesInUse;
        protected int dwChunkId;
        protected int[] dwReserved = new int[3];
        protected long[] qwOffset;
        protected int[] dwSize;
        protected int[] dwDuration;
        private int numIndex;
        private int numIndexFill;

        // Generated
        StringBuffer sb = new StringBuffer();
        private int streamNo = 0;

        @Override
        public void read(final int dwFourCC, final DataReader raf) throws IOException {
            super.read(dwFourCC, raf);

            wLongsPerEntry = raf.readShort();
            bIndexSubType = raf.readByte();
            bIndexType = raf.readByte();
            nEntriesInUse = raf.readInt();
            dwChunkId = raf.readInt(); // id the index points to eg. 00dx

            dwReserved[0] = raf.readInt();
            dwReserved[1] = raf.readInt();
            dwReserved[2] = raf.readInt();

            qwOffset = new long[nEntriesInUse];
            dwSize = new int[nEntriesInUse];
            dwDuration = new int[nEntriesInUse];

            String chunkIdStr = toFourCC(this.dwChunkId);
            sb.append(String
                    .format("\tAvi DML Super Index List - ChunkSize=%d, NumIndexes = %d, longsPerEntry = %d, Stream = %s, Type = %s",
                            this.getChunkSize(), nEntriesInUse, wLongsPerEntry, chunkIdStr.substring(0, 2),
                            chunkIdStr.substring(2)));

            for (int i = 0; i < nEntriesInUse; i++) {
                qwOffset[i] = raf.readLong();
                dwSize[i] = raf.readInt();
                dwDuration[i] = raf.readInt();

                sb.append(String.format("\n\t\tStandard Index - Offset [%d], Size [%d], Duration [%d]", qwOffset[i],
                        dwSize[i], dwDuration[i]));
            }

            raf.position(this.getEndOfChunk());
        }

        @Override
        public String toString() {
            return (sb.toString());
        }
    }

    /**
     * typedef struct _avistdindex_chunk { FOURCC fcc; DWORD cb; WORD
     * wLongsPerEntry; // 2 bytes BYTE bIndexSubType; // 1 byte BYTE bIndexType;
     * // 1 byte DWORD nEntriesInUse; // 4 bytes DWORD dwChunkId; // 4 bytes
     * __int64 qwBaseOffset; // 8 bytes DWORD dwReserved3; // 4 bytes
     * 
     * struct _avistdindex_entry { DWORD dwOffset; // 4 bytes DWORD dwSize; // 4
     * bytes } aIndex[ ]; } AVISTDINDEX;
     * 
     * 
     * #define AVISTDINDEX_DELTAFRAME ( 0x80000000) // Delta frames have the
     * high bit set #define AVISTDINDEX_SIZEMASK (~0x80000000)
     * 
     * #define AVI_INDEX_OF_INDEXES 0x00 #define AVI_INDEX_OF_CHUNKS 0x01
     * #define AVI_INDEX_OF_TIMED_CHUNKS 0x02 #define AVI_INDEX_OF_SUB_2FIELD
     * 0x03 #define AVI_INDEX_IS_DATA 0x80
     */

    // Open DML style AVI Standard Index. Extension index when the first is not
    // enough, mainly for files > 1gb
static class AVITag_AviDmlStandardIndex extends AVIChunk {
        protected short wLongsPerEntry;
        protected byte bIndexSubType;
        protected byte bIndexType;
        protected int nEntriesInUse;
        protected int dwChunkId;

        protected long qwBaseOffset;
        protected int dwReserved2;

        protected int[] dwOffset;
        protected int[] dwDuration;

        int lastOffset = -1, lastDuration = -1;

        @Override
        public int getChunkSize() {
            return (dwChunkSize);
        }

        @Override
        public void read(final int dwFourCC, final DataReader raf) throws IOException {
            super.read(dwFourCC, raf);

            wLongsPerEntry = raf.readShort();
            bIndexSubType = raf.readByte();
            bIndexType = raf.readByte();
            nEntriesInUse = raf.readInt();
            dwChunkId = raf.readInt(); // id the index points to eg. 00dx
            qwBaseOffset = raf.readLong();
            dwReserved2 = raf.readInt();

            dwOffset = new int[nEntriesInUse];
            dwDuration = new int[nEntriesInUse];

            try {
                for (int i = 0; i < nEntriesInUse; i++) {
                    dwOffset[i] = raf.readInt();
                    dwDuration[i] = raf.readInt();

                    lastOffset = dwOffset[i];
                    lastDuration = dwDuration[i];
                }
            } catch (Exception e) {
                Logger.debug("Failed to read : " + toString());
            }

            raf.position(this.getEndOfChunk());
        }

        @Override
        public String toString() {
            return (String
                    .format("\tAvi DML Standard Index List Type=%d, SubType=%d, ChunkId=%s, StartOfChunk=%d, NumIndexes=%d, LongsPerEntry=%d, ChunkSize=%d, FirstOffset=%d, FirstDuration=%d,LastOffset=%d, LastDuration=%d",
                            bIndexType, bIndexSubType, toFourCC(this.dwChunkId), this.getStartOfChunk(), nEntriesInUse,
                            wLongsPerEntry, getChunkSize(), dwOffset[0], dwDuration[0], lastOffset, lastDuration));
        }
    }
}
