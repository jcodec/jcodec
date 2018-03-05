package org.jcodec.codecs.mpeg4;

import static org.jcodec.codecs.mpeg4.MPEG4Consts.SCAN_TABLES;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.jcodec.common.io.BitReader;
import org.jcodec.common.logging.Logger;
import org.jcodec.common.tools.MathUtil;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class MPEG4DecodingContext {
    public int width;
    public int height;
    public int horiz_mc_ref;
    public int vert_mc_ref;

    public short[] intraMpegQuantMatrix;
    public short[] interMpegQuantMatrix;

    public int[][] gmcWarps;
    public int mbWidth;
    public int mbHeight;
    public int spriteEnable;
    public int shape;
    public int quant;
    public int quantBits;
    public int timeIncrementBits;
    public int intraDCThreshold;
    public int spriteWarpingPoints;
    public boolean reducedResolutionEnable;
    public int fcodeForward;
    public int fcodeBackward;
    public boolean newPredEnable;
    public boolean rounding;
    public boolean quarterPel;
    public boolean cartoonMode;
    public int lastTimeBase;
    public int timeBase;
    public int time;
    public int lastNonBTime;
    public int pframeTs;
    public int bframeTs;

    public boolean topFieldFirst;
    public boolean alternateVerticalScan;

    int volVersionId;
    int timestampMSB;
    int timestampLSB;
    boolean complexityEstimationDisable;
    boolean interlacing;
    boolean spriteBrightnessChange;
    boolean scalability;
    Estimation estimation;

    public MPEG4DecodingContext() {
        intraMpegQuantMatrix = new short[64];
        interMpegQuantMatrix = new short[64];
        gmcWarps = new int[3][2];
        estimation = new Estimation();
    }

    private static class Estimation {

        public int method;
        public boolean opaque;
        public boolean transparent;
        public boolean intraCae;
        public boolean interCae;
        public boolean noUpdate;
        public boolean upsampling;
        public boolean intraBlocks;
        public boolean interBlocks;
        public boolean inter4vBlocks;
        public boolean notCodedBlocks;
        public boolean dctCoefs;
        public boolean dctLines;
        public boolean vlcSymbols;
        public boolean vlcBits;
        public boolean apm;
        public boolean npm;
        public boolean interpolateMcQ;
        public boolean forwBackMcQ;
        public boolean halfpel2;
        public boolean halfpel4;
        public boolean sadct;
        public boolean quarterpel;
        
    }

    public static MPEG4DecodingContext readFromHeaders(ByteBuffer bb) {
        MPEG4DecodingContext ret = new MPEG4DecodingContext();
        if (ret.readHeaders(bb))
            return ret;
        return null;
    }

    private final static int VIDOBJ_START_CODE = 0x00000100;
    private final static int VIDOBJLAY_START_CODE = 0x00000120;
    private final static int VISOBJSEQ_START_CODE = 0x000001b0;
    private final static int VISOBJSEQ_STOP_CODE = 0x000001b1;
    private final static int USERDATA_START_CODE = 0x000001b2;
    private final static int GRPOFVOP_START_CODE = 0x000001b3;
    private final static int VISOBJ_START_CODE = 0x000001b5;

    private final static int VISOBJ_TYPE_VIDEO = 1;

    private final static int VIDOBJLAY_AR_EXTPAR = 15;

    private final static int VIDOBJLAY_SHAPE_RECTANGULAR = 0;

    private final static int VIDOBJLAY_SHAPE_BINARY = 1;

    private final static int VIDOBJLAY_SHAPE_BINARY_ONLY = 2;

    private final static int VIDOBJLAY_SHAPE_GRAYSCALE = 3;

    private final static int VOP_START_CODE = 0x1b6;

    private final static int VIDOBJ_START_CODE_MASK = 0x0000001f;

    private final static int VIDOBJLAY_START_CODE_MASK = 0x0000000f;

    private final static int SPRITE_STATIC = 1;
    private final static int SPRITE_GMC = 2;

    private final static int VLC_CODE = 0;
    private final static int VLC_LEN = 1;

    private int timeIncrementResolution;
    private boolean packedMode;
    public int codingType;
    public boolean quantType;
    public int bsVersion = 0xffff;

    public static void getMatrix(BitReader br, short[] matrix) {
        int last, value = 0;
        int i = 0;

        do {
            last = value;
            value = br.readNBit(8);
            matrix[SCAN_TABLES[0][i++]] = (short) value;
        } while (value != 0 && i < 64);

        i--;

        while (i < 64) {
            matrix[SCAN_TABLES[0][i++]] = (short) last;
        }
    }

    public boolean readHeaders(ByteBuffer bb) {
        bb.order(ByteOrder.BIG_ENDIAN);

        while (bb.remaining() >= 4) {
            int startCode = bb.getInt();
            while ((startCode & ~0xff) != 0x100 && bb.hasRemaining()) {
                startCode <<= 8;
                startCode |= bb.get() & 0xff;
            }

            if (startCode == VISOBJSEQ_START_CODE) {
                int profile;

                profile = bb.get();

            } else if (startCode == VISOBJSEQ_STOP_CODE) {
            } else if (startCode == VISOBJ_START_CODE) {
                int verId;

                BitReader br = BitReader.createBitReader(bb);

                if (br.readBool()) {
                    verId = br.readNBit(4);

                    br.skip(3);
                } else {
                    verId = 1;
                }

                int visual_object_type = br.readNBit(4);
                if (visual_object_type != VISOBJ_TYPE_VIDEO) {
                    return false;
                }

                if (br.readBool()) {
                    br.skip(3);
                    br.skip(1);

                    if (br.readBool()) {
                        br.skip(8);
                        br.skip(8);
                        br.skip(8);
                    }
                }
                br.terminate();
            } else if ((startCode & ~VIDOBJ_START_CODE_MASK) == VIDOBJ_START_CODE) {
            } else if ((startCode & ~VIDOBJLAY_START_CODE_MASK) == VIDOBJLAY_START_CODE) {
                BitReader br = BitReader.createBitReader(bb);
                br.skip(1);

                br.skip(8);

                if (br.readBool()) {
                    volVersionId = br.readNBit(4);

                    br.skip(3);
                } else {
                    volVersionId = 1;
                }

                int aspectRatio = br.readNBit(4);

                if (aspectRatio == VIDOBJLAY_AR_EXTPAR) {
                    br.readNBit(8);
                    br.readNBit(8);
                }

                if (br.readBool()) {
                    br.skip(2);

                    boolean lowDelay = br.readBool();

                    if (br.readBool()) {

                        int bitrate = br.readNBit(15) << 15;
                        br.skip(1);
                        bitrate |= br.readNBit(15);
                        br.skip(1);

                        int bufferSize = br.readNBit(15) << 3;
                        br.skip(1);
                        bufferSize |= br.readNBit(3);

                        int occupancy = br.readNBit(11) << 15;
                        br.skip(1);
                        occupancy |= br.readNBit(15);
                        br.skip(1);
                    }
                }

                shape = br.readNBit(2);

                if (shape != VIDOBJLAY_SHAPE_RECTANGULAR) {
                }

                if (shape == VIDOBJLAY_SHAPE_GRAYSCALE && volVersionId != 1) {
                    br.skip(4);
                }

                br.skip(1);

                timeIncrementResolution = br.readNBit(16);

                if (timeIncrementResolution > 0) {
                    timeIncrementBits = Math.max(MathUtil.log2(timeIncrementResolution - 1) + 1, 1);
                } else {
                    timeIncrementBits = 1;
                }

                br.skip(1);

                if (br.readBool()) {
                    br.skip(timeIncrementBits);
                }

                if (shape != VIDOBJLAY_SHAPE_BINARY_ONLY) {
                    if (shape == VIDOBJLAY_SHAPE_RECTANGULAR) {
                        br.skip(1);
                        width = br.readNBit(13);
                        br.skip(1);
                        height = br.readNBit(13);
                        br.skip(1);
                        calcSizes();
                    }

                    interlacing = br.readBool();

                    if (!br.readBool()) {
                    }

                    spriteEnable = br.readNBit((volVersionId == 1 ? 1 : 2));

                    if (spriteEnable == SPRITE_STATIC || spriteEnable == SPRITE_GMC) {
                        if (spriteEnable != SPRITE_GMC) {
                            br.readNBit(13);
                            br.skip(1);
                            br.readNBit(13);
                            br.skip(1);
                            br.readNBit(13);
                            br.skip(1);
                            br.readNBit(13);
                            br.skip(1);
                        }

                        spriteWarpingPoints = br.readNBit(6);
                        br.readNBit(2);
                        spriteBrightnessChange = br.readBool();

                        if (spriteEnable != SPRITE_GMC) {
                            br.readNBit(1);
                        }
                    }

                    if (volVersionId != 1 && shape != VIDOBJLAY_SHAPE_RECTANGULAR) {
                        br.skip(1);
                    }

                    if (br.readBool()) {
                        quantBits = br.readNBit(4);
                        br.skip(4);
                    } else {
                        quantBits = 5;
                    }

                    if (shape == VIDOBJLAY_SHAPE_GRAYSCALE) {
                        br.skip(1);
                        br.skip(1);
                        br.skip(1);
                    }

                    quantType = br.readBool();

                    if (quantType) {
                        if (br.readBool()) {
                            getMatrix(br, intraMpegQuantMatrix);
                        } else {
                            System.arraycopy(MPEG4Consts.DEFAULT_INTRA_MATRIX, 0, intraMpegQuantMatrix, 0,
                                    intraMpegQuantMatrix.length);
                        }

                        if (br.readBool()) {
                            getMatrix(br, interMpegQuantMatrix);
                        } else {
                            System.arraycopy(MPEG4Consts.DEFAULT_INTER_MATRIX, 0, interMpegQuantMatrix, 0,
                                    interMpegQuantMatrix.length);
                        }

                        if (shape == VIDOBJLAY_SHAPE_GRAYSCALE) {
                            return false;
                        }
                    }

                    if (volVersionId != 1) {
                        quarterPel = br.readBool();
                    } else {
                        quarterPel = false;
                    }

                    complexityEstimationDisable = br.readBool();
                    if (!complexityEstimationDisable) {
                        readVolComplexityEstimationHeader(br, estimation);
                    }

                    br.skip(1);

                    if (br.readBool()) {
                        br.skip(1);
                    }

                    if (volVersionId != 1) {
                        newPredEnable = br.readBool();

                        if (newPredEnable) {
                            br.skip(2);
                            br.skip(1);
                        }

                        reducedResolutionEnable = br.readBool();
                    } else {
                        newPredEnable = false;
                        reducedResolutionEnable = false;
                    }

                    scalability = br.readBool();

                    if (scalability) {
                        br.skip(1);
                        br.skip(4);
                        br.skip(1);
                        br.skip(5);
                        br.skip(5);
                        br.skip(5);
                        br.skip(5);
                        br.skip(1);

                        if (shape == VIDOBJLAY_SHAPE_BINARY) {
                            br.skip(1);
                            br.skip(1);
                            br.skip(5);
                            br.skip(5);
                            br.skip(5);
                            br.skip(5);
                        }

                        return false;
                    }
                } else {
                    if (volVersionId != 1) {
                        scalability = br.readBool();

                        if (scalability) {
                            br.skip(4);
                            br.skip(5);
                            br.skip(5);
                            br.skip(5);
                            br.skip(5);

                            return false;
                        }
                    }

                    br.skip(1);
                }
                br.terminate();
            } else if (startCode == GRPOFVOP_START_CODE) {
                BitReader br = BitReader.createBitReader(bb);

                int hours, minutes, seconds;

                hours = br.readNBit(5);
                minutes = br.readNBit(6);
                br.skip(1);
                seconds = br.readNBit(6);

                br.skip(1);
                br.skip(1);
                br.terminate();
            } else if (startCode == VOP_START_CODE) {
                return true;
            } else if (startCode == USERDATA_START_CODE) {
                byte[] tmp = new byte[256];

                int i = 0;
                tmp[i++] = bb.get();
                for (; (tmp[i] = bb.get()) != 0; i++)
                    ;
                bb.position(bb.position() - 1);

                String userData = new String(tmp, 0, i);

                if (userData.startsWith("XviD")) {
                    if (tmp[userData.length() - 1] == 'C') {
                        bsVersion = Integer.parseInt(userData.substring(4, userData.length() - 1));
                        cartoonMode = true;
                    } else {
                        bsVersion = Integer.parseInt(userData.substring(4));
                    }
                }

                if (userData.startsWith("DivX")) {
                    int version, build;
                    char packed;

                    int buildIndex = userData.indexOf("Build");

                    if (buildIndex == -1) {
                        buildIndex = userData.indexOf("b");
                    }

                    try {
                        version = Integer.parseInt(userData.substring(4, buildIndex));
                        build = Integer.parseInt(userData.substring(buildIndex + 1, userData.length() - 1));
                        packed = userData.charAt(userData.length() - 1);
                        packedMode = packed == 'p';

                    } catch (Exception ignored) {
                    }
                }
            } else {
                Logger.debug("Unknown");
            }
        }

        return false;
    }

    private void calcSizes() {
        mbWidth = (width + 15) / 16;
        mbHeight = (height + 15) / 16;
    }

    private void readVolComplexityEstimationHeader(BitReader br, Estimation estimation) {
        estimation.method = br.readNBit(2);

        if (estimation.method == 0 || estimation.method == 1) {
            if (!br.readBool()) {
                estimation.opaque = br.readBool();
                estimation.transparent = br.readBool();
                estimation.intraCae = br.readBool();
                estimation.interCae = br.readBool();
                estimation.noUpdate = br.readBool();
                estimation.upsampling = br.readBool();
            }

            if (!br.readBool()) {
                estimation.intraBlocks = br.readBool();
                estimation.interBlocks = br.readBool();
                estimation.inter4vBlocks = br.readBool();
                estimation.notCodedBlocks = br.readBool();
            }
        }

        br.skip(1);

        if (!br.readBool()) {
            estimation.dctCoefs = br.readBool();
            estimation.dctLines = br.readBool();
            estimation.vlcSymbols = br.readBool();
            estimation.vlcBits = br.readBool();
        }

        if (!br.readBool()) {
            estimation.apm = br.readBool();
            estimation.npm = br.readBool();
            estimation.interpolateMcQ = br.readBool();
            estimation.forwBackMcQ = br.readBool();
            estimation.halfpel2 = br.readBool();
            estimation.halfpel4 = br.readBool();
        }

        br.skip(1);

        if (estimation.method == 1) {
            if (!br.readBool()) {
                estimation.sadct = br.readBool();
                estimation.quarterpel = br.readBool();
            }
        }
    }

    public boolean readVOPHeader(BitReader br) {
        rounding = false;
        quant = 2;

        codingType = br.readNBit(2);

        while (br.readBool()) {
            timestampMSB++;
        }

        br.skip(1);

        if (getTimeIncrementBits() != 0) {
            timestampLSB = br.readNBit(getTimeIncrementBits());
        }

        br.skip(1);

        if (!br.readBool()) {
            return false;
        }

        if (newPredEnable) {
            int vopId;
            int vopIdForPrediction;

            vopId = br.readNBit(Math.min(getTimeIncrementBits() + 3, 15));

            if (br.readBool()) {
                vopIdForPrediction = br.readNBit(Math.min(getTimeIncrementBits() + 3, 15));
            }

            br.skip(1);
        }

        if ((shape != VIDOBJLAY_SHAPE_BINARY_ONLY)
                && ((codingType == MPEG4Bitstream.P_VOP) || (codingType == MPEG4Bitstream.S_VOP && spriteEnable == SPRITE_GMC))) {
            rounding = br.readBool();
        }

        if (reducedResolutionEnable && shape == VIDOBJLAY_SHAPE_RECTANGULAR
                && (codingType == MPEG4Bitstream.P_VOP || codingType == MPEG4Bitstream.I_VOP)) {
            if (br.readBool()) {
            }
        }

        if (shape != VIDOBJLAY_SHAPE_RECTANGULAR) {
            if (!(spriteEnable == SPRITE_STATIC && codingType == MPEG4Bitstream.I_VOP)) {
                width = br.readNBit(13);
                br.skip(1);
                height = br.readNBit(13);
                br.skip(1);
                horiz_mc_ref = br.readNBit(13);
                br.skip(1);
                vert_mc_ref = br.readNBit(13);
                br.skip(1);
                calcSizes();
            }

            br.skip(1);

            if (br.readBool()) {
                br.skip(8);
            }
        }
        Estimation estimation = new Estimation();

        if (shape != VIDOBJLAY_SHAPE_BINARY_ONLY) {
            if (!complexityEstimationDisable) {
                readVopComplexityEstimationHeader(br, estimation, spriteEnable, codingType);
            }

            intraDCThreshold = MPEG4Consts.INTRA_DC_THRESHOLD_TABLE[br.readNBit(3)];

            if (interlacing) {
                topFieldFirst = br.readBool();
                alternateVerticalScan = br.readBool();
            }
        }

        if ((spriteEnable == SPRITE_STATIC || spriteEnable == SPRITE_GMC) && codingType == MPEG4Bitstream.S_VOP) {
            for (int i = 0; i < spriteWarpingPoints; i++) {
                int length;
                int x = 0, y = 0;

                length = getSpriteTrajectory(br);

                if (length > 0) {
                    x = br.readNBit(length);

                    if ((x >> (length - 1)) == 0) {
                        x = -(x ^ ((1 << length) - 1));
                    }
                }

                br.skip(1);

                length = getSpriteTrajectory(br);

                if (length > 0) {
                    y = br.readNBit(length);

                    if ((y >> (length - 1)) == 0) {
                        y = -(y ^ ((1 << length) - 1));
                    }
                }

                br.skip(1);

                gmcWarps[i][0] = x;
                gmcWarps[i][1] = y;
            }

            if (spriteBrightnessChange) {
            }
            if (spriteEnable == SPRITE_STATIC) {
            }

        }

        if ((quant = br.readNBit(quantBits)) < 1) {
            quant = 1;
        }

        if (codingType != MPEG4Bitstream.I_VOP) {
            fcodeForward = br.readNBit(3);
        }

        if (codingType == MPEG4Bitstream.B_VOP) {
            fcodeBackward = br.readNBit(3);
        }

        if (!scalability) {
            if ((shape != VIDOBJLAY_SHAPE_RECTANGULAR) && (codingType != MPEG4Bitstream.I_VOP)) {
                br.skip(1);
            }
        }

        if (codingType != MPEG4Bitstream.B_VOP) {
            lastTimeBase = timeBase;
            timeBase += timestampMSB;
            time = timeBase * getTimeIncrementResolution() + timestampLSB;
            pframeTs = time - lastNonBTime;
            lastNonBTime = time;
        } else {
            time = (lastTimeBase + timestampMSB) * getTimeIncrementResolution() + timestampLSB;
            bframeTs = pframeTs - (lastNonBTime - time);
        }
        return true;
    }

    private int getSpriteTrajectory(BitReader br) {
        for (int i = 0; i < 12; i++) {
            if (br.checkNBit(MPEG4Consts.SPRITE_TRAJECTORY_LEN[i][VLC_LEN]) == MPEG4Consts.SPRITE_TRAJECTORY_LEN[i][VLC_CODE]) {
                br.skip(MPEG4Consts.SPRITE_TRAJECTORY_LEN[i][VLC_LEN]);
                return i;
            }
        }

        return -1;
    }

    private void readVopComplexityEstimationHeader(BitReader br, Estimation estimation, int spriteEnable,
            int codingType) {
        if (estimation.method == 0 || estimation.method == 1) {
            if (codingType == MPEG4Bitstream.I_VOP) {
                if (estimation.opaque)
                    br.skip(8);
                if (estimation.transparent)
                    br.skip(8);
                if (estimation.intraCae)
                    br.skip(8);
                if (estimation.interCae)
                    br.skip(8);
                if (estimation.noUpdate)
                    br.skip(8);
                if (estimation.upsampling)
                    br.skip(8);
                if (estimation.intraBlocks)
                    br.skip(8);
                if (estimation.notCodedBlocks)
                    br.skip(8);
                if (estimation.dctCoefs)
                    br.skip(8);
                if (estimation.dctLines)
                    br.skip(8);
                if (estimation.vlcSymbols)
                    br.skip(8);
                if (estimation.vlcBits)
                    br.skip(8);
                if (estimation.sadct)
                    br.skip(8);
            }

            if (codingType == MPEG4Bitstream.P_VOP) {
                if (estimation.opaque)
                    br.skip(8);
                if (estimation.transparent)
                    br.skip(8);
                if (estimation.intraCae)
                    br.skip(8);
                if (estimation.interCae)
                    br.skip(8);
                if (estimation.noUpdate)
                    br.skip(8);
                if (estimation.upsampling)
                    br.skip(8);
                if (estimation.intraBlocks)
                    br.skip(8);
                if (estimation.notCodedBlocks)
                    br.skip(8);
                if (estimation.dctCoefs)
                    br.skip(8);
                if (estimation.dctLines)
                    br.skip(8);
                if (estimation.vlcSymbols)
                    br.skip(8);
                if (estimation.vlcBits)
                    br.skip(8);
                if (estimation.interBlocks)
                    br.skip(8);
                if (estimation.inter4vBlocks)
                    br.skip(8);
                if (estimation.apm)
                    br.skip(8);
                if (estimation.npm)
                    br.skip(8);
                if (estimation.forwBackMcQ)
                    br.skip(8);
                if (estimation.halfpel2)
                    br.skip(8);
                if (estimation.halfpel4)
                    br.skip(8);
                if (estimation.sadct)
                    br.skip(8);
                if (estimation.quarterpel)
                    br.skip(8);
            }

            if (codingType == MPEG4Bitstream.B_VOP) {
                if (estimation.opaque)
                    br.skip(8);
                if (estimation.transparent)
                    br.skip(8);
                if (estimation.intraCae)
                    br.skip(8);
                if (estimation.interCae)
                    br.skip(8);
                if (estimation.noUpdate)
                    br.skip(8);
                if (estimation.upsampling)
                    br.skip(8);
                if (estimation.intraBlocks)
                    br.skip(8);
                if (estimation.notCodedBlocks)
                    br.skip(8);
                if (estimation.dctCoefs)
                    br.skip(8);
                if (estimation.dctLines)
                    br.skip(8);
                if (estimation.vlcSymbols)
                    br.skip(8);
                if (estimation.vlcBits)
                    br.skip(8);
                if (estimation.interBlocks)
                    br.skip(8);
                if (estimation.inter4vBlocks)
                    br.skip(8);
                if (estimation.apm)
                    br.skip(8);
                if (estimation.npm)
                    br.skip(8);
                if (estimation.forwBackMcQ)
                    br.skip(8);
                if (estimation.halfpel2)
                    br.skip(8);
                if (estimation.halfpel4)
                    br.skip(8);
                if (estimation.interpolateMcQ)
                    br.skip(8);
                if (estimation.sadct)
                    br.skip(8);
                if (estimation.quarterpel)
                    br.skip(8);
            }

            if (codingType == MPEG4Bitstream.S_VOP && spriteEnable == SPRITE_STATIC) {
                if (estimation.intraBlocks)
                    br.skip(8);
                if (estimation.notCodedBlocks)
                    br.skip(8);
                if (estimation.dctCoefs)
                    br.skip(8);
                if (estimation.dctLines)
                    br.skip(8);
                if (estimation.vlcSymbols)
                    br.skip(8);
                if (estimation.vlcBits)
                    br.skip(8);
                if (estimation.interBlocks)
                    br.skip(8);
                if (estimation.inter4vBlocks)
                    br.skip(8);
                if (estimation.apm)
                    br.skip(8);
                if (estimation.npm)
                    br.skip(8);
                if (estimation.forwBackMcQ)
                    br.skip(8);
                if (estimation.halfpel2)
                    br.skip(8);
                if (estimation.halfpel4)
                    br.skip(8);
                if (estimation.interpolateMcQ)
                    br.skip(8);
            }
        }
    }

    public boolean getPackedMode() {
        return packedMode;
    }

    public int getTimeIncrementBits() {
        return timeIncrementBits;
    }

    public int getTimeIncrementResolution() {
        return timeIncrementResolution;
    }
}
