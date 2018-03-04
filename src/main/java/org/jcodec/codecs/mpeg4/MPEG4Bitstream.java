package org.jcodec.codecs.mpeg4;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static org.jcodec.codecs.mpeg4.MPEG4Consts.BS_VERSION_BUGGY_DC_CLIP;
import static org.jcodec.codecs.mpeg4.MPEG4Consts.CBPY_TABLE;
import static org.jcodec.codecs.mpeg4.MPEG4Consts.COEFF_TAB;
import static org.jcodec.codecs.mpeg4.MPEG4Consts.DC_LUM_TAB;
import static org.jcodec.codecs.mpeg4.MPEG4Consts.DEFAULT_ACDC_VALUES;
import static org.jcodec.codecs.mpeg4.MPEG4Consts.INTRA_DC_THRESHOLD_TABLE;
import static org.jcodec.codecs.mpeg4.MPEG4Consts.MAX_LEVEL;
import static org.jcodec.codecs.mpeg4.MPEG4Consts.MAX_RUN;
import static org.jcodec.codecs.mpeg4.MPEG4Consts.MCBPC_INTER_TABLE;
import static org.jcodec.codecs.mpeg4.MPEG4Consts.MCBPC_INTRA_TABLE;
import static org.jcodec.codecs.mpeg4.MPEG4Consts.MODE_BACKWARD;
import static org.jcodec.codecs.mpeg4.MPEG4Consts.MODE_DIRECT;
import static org.jcodec.codecs.mpeg4.MPEG4Consts.MODE_DIRECT_NONE_MV;
import static org.jcodec.codecs.mpeg4.MPEG4Consts.MODE_FORWARD;
import static org.jcodec.codecs.mpeg4.MPEG4Consts.MODE_INTER;
import static org.jcodec.codecs.mpeg4.MPEG4Consts.MODE_INTER4V;
import static org.jcodec.codecs.mpeg4.MPEG4Consts.MODE_INTERPOLATE;
import static org.jcodec.codecs.mpeg4.MPEG4Consts.MODE_INTER_Q;
import static org.jcodec.codecs.mpeg4.MPEG4Consts.MODE_INTRA;
import static org.jcodec.codecs.mpeg4.MPEG4Consts.MODE_INTRA_Q;
import static org.jcodec.codecs.mpeg4.MPEG4Consts.SCAN_TABLES;
import static org.jcodec.codecs.mpeg4.MPEG4Consts.TMNMV_TAB_0;
import static org.jcodec.codecs.mpeg4.MPEG4Consts.TMNMV_TAB_1;
import static org.jcodec.codecs.mpeg4.MPEG4Consts.TMNMV_TAB_2;
import static org.jcodec.codecs.mpeg4.MPEG4Consts.ZERO_MV;
import static org.jcodec.codecs.mpeg4.MPEG4Decoder.I_VOP;
import static org.jcodec.codecs.mpeg4.MPEG4Decoder.P_VOP;
import static org.jcodec.codecs.mpeg4.MPEG4Decoder.S_VOP;
import static org.jcodec.common.tools.MathUtil.abs;
import static org.jcodec.common.tools.MathUtil.log2;

import java.util.Arrays;

import org.jcodec.codecs.mpeg4.Macroblock.Vector;
import org.jcodec.common.io.BitReader;
import org.jcodec.common.logging.Logger;
import org.jcodec.common.tools.MathUtil;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 */
class MPEG4Bitstream {
    private final static int REVERSE_EVENT_LEN = 0;
    private final static int REVERSE_EVENT_LAST = 1;
    private final static int REVERSE_EVENT_RUN = 2;
    private final static int REVERSE_EVENT_LEVEL = 3;
    private final static int VLC_TABLE_VLC_CODE = 0;
    private final static int VLC_TABLE_VLC_LEN = 1;
    private final static int VLC_TABLE_EVENT_LAST = 2;
    private final static int VLC_TABLE_EVENT_RUN = 3;
    private final static int VLC_TABLE_EVENT_LEVEL = 4;
    private final static int VLC_CODE = 0;
    private final static int VLC_LEN = 1;
    private final static int ESCAPE = 3;
    private final static int NUMBITS_VP_RESYNC_MARKER = 17;
    private final static int RESYNC_MARKER = 1;
    private final static int VIDOBJLAY_SHAPE_RECTANGULAR = 0;
    private final static int VIDOBJLAY_SHAPE_BINARY_ONLY = 2;
    private final static int SPRITE_STATIC = 1;
    private final static int SPRITE_GMC = 2;
    private final static int[] DQUANT_TABLE = { -1, -2, 1, 2 };

    private final static byte[][][] vlcTab = new byte[2][4096][4];

    static {
        initVLCTables();
    }

    private final static void initVLCTables() {
        for (int intra = 0; intra < 2; intra++)
            for (int i = 0; i < 4096; i++)
                vlcTab[intra][i][REVERSE_EVENT_LEVEL] = 0;

        for (int intra = 0; intra < 2; intra++) {
            for (int i = 0; i < 102; i++) {
                int len = COEFF_TAB[intra][i][VLC_TABLE_VLC_LEN];
                int last = COEFF_TAB[intra][i][VLC_TABLE_EVENT_LAST];
                int run = COEFF_TAB[intra][i][VLC_TABLE_EVENT_RUN];
                int level = COEFF_TAB[intra][i][VLC_TABLE_EVENT_LEVEL];
                int code = COEFF_TAB[intra][i][VLC_TABLE_VLC_CODE];
                int lowBits = 12 - len;

                for (int j = 0; j < (1 << lowBits); j++) {
                    int entry = (code << lowBits) | j;
                    vlcTab[intra][entry][REVERSE_EVENT_LEN] = (byte) len;
                    vlcTab[intra][entry][REVERSE_EVENT_LAST] = (byte) last;
                    vlcTab[intra][entry][REVERSE_EVENT_RUN] = (byte) run;
                    vlcTab[intra][entry][REVERSE_EVENT_LEVEL] = (byte) level;
                }
            }
        }
    }

    static void readMatrix(BitReader br, short[] matrix) {
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

    static int readMcbpcIntra(BitReader br) {
        int index = br.checkNBit(9);
        index >>= 3;
        br.skip(MCBPC_INTRA_TABLE[index][VLC_LEN]);

        return MCBPC_INTRA_TABLE[index][VLC_CODE];
    }

    static int readMcbpcInter(BitReader br) {
        int index = Math.min(br.checkNBit(9), 256);
        br.skip(MCBPC_INTER_TABLE[index][VLC_LEN]);

        return MCBPC_INTER_TABLE[index][VLC_CODE];
    }

    static int readCbpy(BitReader br, boolean intra) {
        int index = br.checkNBit(6);

        br.skip(CBPY_TABLE[index][VLC_LEN]);
        int cbpy = CBPY_TABLE[index][VLC_CODE];

        if (!intra)
            cbpy = 15 - cbpy;

        return cbpy;
    }

    static void readIntraBlock(BitReader br, short[] block, int direction, int coeff) {
        final short[] scan = SCAN_TABLES[direction];
        int c;
        do {
            c = readCoeffs(br, true, false);
            int level = level(c);
            coeff += run(c);
            if ((coeff & ~63) != 0) {
                Logger.error("invalid run or index");
                break;
            }

            block[scan[coeff]] = (short) level;

            if (level < -2047 || level > 2047) {
                Logger.error("intra_overflow: " + level);
            }

            coeff++;
        } while (last(c) == 0);
    }

    static void readInterBlockH263(BitReader br, short[] block, int direction, int quant) {
        final short[] scan = SCAN_TABLES[direction];
        final int quant_m_2 = quant << 1;
        final int quant_add = ((quant & 1) != 0 ? quant : quant - 1);
        int p = 0;

        p = 0;
        int coeff;
        do {
            coeff = readCoeffs(br, false, false);
            int level = level(coeff);
            p += run(coeff);
            if ((p & ~63) != 0) {
                Logger.error("invalid run or index");
                break;
            }

            if (level < 0) {
                level = level * quant_m_2 - quant_add;
                block[scan[p]] = (short) (level >= -2048 ? level : -2048);
            } else {
                level = level * quant_m_2 + quant_add;
                block[scan[p]] = (short) (level <= 2047 ? level : 2047);
            }
            p++;
        } while (last(coeff) == 0);
    }

    static void readInterBlockMPEG(BitReader br, short[] block, int direction, int quant, short[] matrix) {
        final short[] scan = SCAN_TABLES[direction];
        int p = 0;
        int sum = 0;

        int coeff;
        do {
            coeff = readCoeffs(br, false, false);
            int level = level(coeff);
            p += run(coeff);
            if ((p & ~63) != 0) {
                Logger.error("invalid run or index");
                break;
            }

            if (level < 0) {
                level = ((2 * -level + 1) * matrix[scan[p]] * quant) >> 4;
                block[scan[p]] = (short) (level <= 2048 ? -level : -2048);
            } else {
                level = ((2 * level + 1) * matrix[scan[p]] * quant) >> 4;
                block[scan[p]] = (short) (level <= 2047 ? level : 2047);
            }

            sum ^= block[scan[p]];

            p++;
        } while (last(coeff) == 0);

        if ((sum & 1) == 0) {
            block[63] ^= 1;
        }
    }

    static int packCoeff(int level, int run, int last) {
        return ((last & 0xff) << 24) | ((run & 0xff) << 16) | (level & 0xffff);
    }

    static int last(int coeff) {
        return coeff >> 24;
    }

    static int run(int coeff) {
        return (coeff >> 16) & 0xff;
    }

    static int level(int coeff) {
        return (short) coeff;
    }

    static int readCoeffs(BitReader br, boolean intra, boolean shortVideoHeader) {
        int mode;
        short level;
        int run, last;
        byte[] reverse;

        if (shortVideoHeader)
            intra = false;

        int intraIndex = intra ? 1 : 0;

        if (br.checkNBit(7) != ESCAPE) {
            reverse = vlcTab[intraIndex][br.checkNBit(12)];

            if ((level = reverse[REVERSE_EVENT_LEVEL]) == 0) {
                run = 64;
                return packCoeff(0, run, 1);
            }

            last = reverse[REVERSE_EVENT_LAST];
            run = reverse[REVERSE_EVENT_RUN];

            br.skip(reverse[REVERSE_EVENT_LEN]);

            return packCoeff(br.readBool() ? -level : level, run, last);
        }

        br.skip(7);

        if (shortVideoHeader) {
            last = br.readNBit(1);
            run = br.readNBit(6);
            level = (byte) br.readNBit(8);

            if (level == 0 || level == 128)
                Logger.error("Illegal LEVEL for ESCAPE mode 4: " + level);

            return packCoeff(level, run, last);
        }

        mode = br.checkNBit(2);

        if (mode < 3) {
            br.skip((mode == 2) ? 2 : 1);

            reverse = vlcTab[intraIndex][br.checkNBit(12)];

            if ((level = reverse[REVERSE_EVENT_LEVEL]) == 0) {
                run = 64;
                return packCoeff(0, run, 1);
            }

            last = reverse[REVERSE_EVENT_LAST];
            run = reverse[REVERSE_EVENT_RUN];

            br.skip(reverse[REVERSE_EVENT_LEN]);

            if (mode < 2)
                level += MAX_LEVEL[intraIndex][last][run];
            else
                run += MAX_RUN[intraIndex][last][level] + 1;

            return packCoeff(br.readBool() ? -level : level, run, last);
        }

        br.skip(2);
        last = br.read1Bit();
        run = br.readNBit(6);
        br.skip(1);
        level = (short) ((br.readNBit(12) << 20) >> 20);
        br.skip(1);

        return packCoeff(level, run, last);
    }

    static int readMVData(BitReader br) {
        if (br.readBool()) {
            return 0;
        }

        int[] tab;

        int index = br.checkNBit(12);

        if (index >= 512) {
            tab = TMNMV_TAB_0[(index >> 8) - 2];
        } else if (index >= 128) {
            tab = TMNMV_TAB_1[(index >> 2) - 32];
        } else {
            if (index < 4) {
                return 0;
            }

            tab = TMNMV_TAB_2[index - 4];
        }

        br.skip(tab[1]);

        return tab[0];
    }

    static int readMVComponent(BitReader br, int fcode) {
        int scaleFac = 1 << (fcode - 1);

        int data = readMVData(br);

        if (scaleFac == 1 || data == 0) {
            return data;
        }

        int res = br.readNBit(fcode - 1);

        int mv = ((Math.abs(data) - 1) * scaleFac) + res + 1;

        return data < 0 ? -mv : mv;
    }

    static int readMBType(BitReader br) {
        for (int mbType = 0; mbType <= 3; mbType++) {
            if (br.readBool()) {
                return mbType;
            }
        }

        return -1;
    }

    static int readDBQuant(BitReader br) {
        if (!br.readBool()) {
            return 0;
        } else if (!br.readBool()) {
            return -2;
        }

        return 2;
    }

    static int readDCSizeLum(BitReader br) {
        int code = br.checkNBit(11);

        for (int i = 11; i > 3; i--) {
            if (code == 1) {
                br.skip(i);
                return i + 1;
            }
            code >>= 1;
        }

        br.skip(DC_LUM_TAB[code][1]);

        return DC_LUM_TAB[code][0];
    }

    static int readDCSizeChrom(BitReader br) {
        int code = br.checkNBit(12);

        for (int i = 12; i > 2; i--) {
            if (code == 1) {
                br.skip(i);
                return i;
            }
            code >>= 1;
        }

        return 3 - br.readNBit(2);
    }

    static short readDCDif(BitReader br, int dcSize) {
        int code = br.readNBit(dcSize);
        int msb = code >> (dcSize - 1);

        if (msb == 0)
            return (short) (-1 * (code ^ ((1 << dcSize) - 1)));

        return (short) code;
    }

    static int readVideoPacketHeader(BitReader br, MPEG4DecodingContext ctx, final int addBits,
            boolean fcodeForwardEnabled, boolean fcodeBackwardEnabled, boolean intraDCThresholdEnabled) {
        int startcodeBits = NUMBITS_VP_RESYNC_MARKER + addBits;
        int mbNumBits = log2(ctx.mbWidth * ctx.mbHeight - 1) + 1;
        int mbnum;
        boolean hec = false;

        br.align();
        br.skip(startcodeBits);

        if (ctx.shape != VIDOBJLAY_SHAPE_RECTANGULAR) {
            hec = br.readBool();
            if (hec && ctx.spriteEnable != SPRITE_STATIC) {
                br.skip(13);
                br.skip(1);
                br.skip(13);
                br.skip(1);
                br.skip(13);
                br.skip(1);
                br.skip(13);
                br.skip(1);
            }
        }

        mbnum = br.readNBit(mbNumBits);

        if (ctx.shape != VIDOBJLAY_SHAPE_BINARY_ONLY) {
            ctx.quant = br.readNBit(ctx.quantBits);
        }

        if (ctx.shape == VIDOBJLAY_SHAPE_RECTANGULAR)
            hec = br.readBool();

        if (hec) {
            int timeBase;
            int timeIncrement = 0;
            int codingType;

            for (timeBase = 0; br.readBool(); timeBase++) {
            }
            br.skip(1);
            if (ctx.timeIncrementBits != 0)
                timeIncrement = (br.readNBit(ctx.timeIncrementBits));
            br.skip(1);

            codingType = br.readNBit(2);

            if (ctx.shape != VIDOBJLAY_SHAPE_RECTANGULAR) {
                br.skip(1);
                if (codingType != I_VOP)
                    br.skip(1);
            }

            if (ctx.shape != VIDOBJLAY_SHAPE_BINARY_ONLY) {
                if (intraDCThresholdEnabled) {
                    ctx.intraDCThreshold = INTRA_DC_THRESHOLD_TABLE[br.readNBit(3)];
                }

                if (ctx.spriteEnable == SPRITE_GMC && codingType == S_VOP && ctx.spriteWarpingPoints > 0) {
                }
                if (ctx.reducedResolutionEnable && ctx.shape == VIDOBJLAY_SHAPE_RECTANGULAR
                        && (codingType == P_VOP || codingType == I_VOP)) {
                    br.skip(1);
                }

                if (codingType != I_VOP && fcodeForwardEnabled) {
                    ctx.fcodeForward = br.readNBit(3);
                }

                if (codingType == MPEG4Decoder.B_VOP && fcodeBackwardEnabled) {
                    ctx.fcodeBackward = br.readNBit(3);
                }
            }
        }

        if (ctx.newPredEnable) {
            int vopId;
            int vopIdForPrediction;

            vopId = br.readNBit(Math.min(ctx.timeIncrementBits + 3, 15));
            if (br.readBool()) {
                vopIdForPrediction = br.readNBit(Math.min(ctx.timeIncrementBits + 3, 15));
            }
            br.skip(1);
        }

        return mbnum;
    }

    static boolean checkResyncMarker(BitReader br, int addBits) {
        int nbits = br.bitsToAlign();
        int nbitResyncMarker = NUMBITS_VP_RESYNC_MARKER + addBits;
        int code = br.checkNBitDontCare(nbitResyncMarker + nbits);

        int MASK1 = (1 << (nbits - 1)) - 1;
        int MASK2 = (1 << nbitResyncMarker) - 1;

        if ((code >> nbitResyncMarker) == MASK1) {
            return (code & MASK2) == RESYNC_MARKER;
        }

        return false;
    }

    static void readIntraMode(BitReader br, MPEG4DecodingContext ctx, Macroblock mb) {
        while (br.checkNBit(9) == 1)
            br.skip(9);

        if (checkResyncMarker(br, 0)) {
            mb.bound = readVideoPacketHeader(br, ctx, 0, false, false, true);

            mb.x = mb.bound % ctx.mbWidth;
            mb.y = mb.bound / ctx.mbWidth;
        }

        int mcbpc = MPEG4Bitstream.readMcbpcIntra(br);
        mb.mode = mcbpc & 7;
        int cbpc = mcbpc >>> 4;

        mb.acpredFlag = br.readBool();

        int cbpy = MPEG4Bitstream.readCbpy(br, true);
        mb.cbp = (cbpy << 2) | cbpc;

        if (mb.mode == MODE_INTRA_Q) {
            ctx.quant += DQUANT_TABLE[br.readNBit(2)];
            if (ctx.quant > 31) {
                ctx.quant = 31;
            } else if (ctx.quant < 1) {
                ctx.quant = 1;
            }
        }

        mb.quant = ctx.quant;
        mb.mvs[0].x = mb.mvs[0].y = mb.mvs[1].x = mb.mvs[1].y = mb.mvs[2].x = mb.mvs[2].y = mb.mvs[3].x = mb.mvs[3].y = 0;

        if (ctx.interlacing) {
            mb.fieldDCT = br.readBool();
        }
    }

    static void readCoeffIntra(BitReader br, MPEG4DecodingContext ctx, Macroblock mb, Macroblock aboveMb,
            Macroblock leftMb, Macroblock aboveLeftMb) {
        for (int i = 0; i < 6; i++) {
            Arrays.fill(mb.block[i], (byte) 0);
            int iDcScaler = getDCScaler(mb.quant, i < 4);
            int startCoeff;

            predictAcdc(ctx, mb.x, mb.y, i, mb.quant, iDcScaler, mb.predictors, mb.bound, mb, aboveMb, leftMb,
                    aboveLeftMb);

            if (!mb.acpredFlag) {
                mb.acpredDirections[i] = 0;
            }

            if (mb.quant < ctx.intraDCThreshold) {
                int dcSize = i < 4 ? readDCSizeLum(br) : readDCSizeChrom(br);
                short dcDif = dcSize != 0 ? readDCDif(br, dcSize) : 0;

                if (dcSize > 8) {
                    br.skip(1);
                }

                mb.block[i][0] = dcDif;
                startCoeff = 1;
            } else {
                startCoeff = 0;
            }

            if ((mb.cbp & (1 << (5 - i))) != 0) {
                int direction = ctx.alternateVerticalScan ? 2 : mb.acpredDirections[i];

                MPEG4Bitstream.readIntraBlock(br, mb.block[i], direction, startCoeff);
            }

            addAcdc(mb, ctx.bsVersion, i, iDcScaler);

            if (!ctx.quantType) {
                dequantH263Intra(ctx, mb.block[i], mb.quant, iDcScaler);
            } else {
                dequantMpegIntra(ctx, mb.block[i], mb.quant, iDcScaler);
            }
        }
    }

    static void addAcdc(Macroblock mb, int bsVersion, int block, int iDcScaler) {
        short[] coeffs = mb.block[block];
        byte acpredDirection = (byte) mb.acpredDirections[block];
        short[] current = mb.predValues[block];

        coeffs[0] += mb.predictors[0]; // dc prediction
        current[0] = (short) (coeffs[0] * iDcScaler);

        if (bsVersion == 0 || bsVersion > BS_VERSION_BUGGY_DC_CLIP) {
            current[0] = (((current[0]) < (-2048)) ? (-2048) : ((current[0]) > (2047)) ? (2047) : (current[0]));
        }

        if (acpredDirection == 1) {
            for (int i = 1; i < 8; i++) {
                short level = (short) (coeffs[i] + mb.predictors[i]);

                coeffs[i] = level;
                current[i] = level;
                current[i + 7] = coeffs[i * 8];
            }
        } else if (acpredDirection == 2) {
            for (int i = 1; i < 8; i++) {
                short level = (short) (coeffs[i * 8] + mb.predictors[i]);

                coeffs[i * 8] = level;
                current[i + 7] = level;
                current[i] = coeffs[i];
            }
        } else {
            for (int i = 1; i < 8; i++) {
                current[i] = coeffs[i];
                current[i + 7] = coeffs[i * 8];
            }
        }
    }

    static void readInterModeCoeffs(BitReader br, MPEG4DecodingContext ctx, int fcode, Macroblock mb,
            Macroblock aboveMb, Macroblock leftMb, Macroblock aboveLeftMb, Macroblock aboveRightMb) {
        if (!br.readBool()) {
            mb.coded = true;
            int mcbpc, cbpc, cbpy;
            boolean intra = false;
            boolean mcsel = false;

            mcbpc = MPEG4Bitstream.readMcbpcInter(br);
            mb.mode = mcbpc & 7;
            cbpc = (mcbpc >>> 4);

            intra = (mb.mode == MODE_INTRA || mb.mode == MODE_INTRA_Q);

            if (intra) {
                mb.acpredFlag = br.readBool();
            }
            mb.mcsel = mcsel;

            cbpy = readCbpy(br, intra);

            mb.cbp = (cbpy << 2) | cbpc;

            if (mb.mode == MODE_INTER_Q || mb.mode == MODE_INTRA_Q) {
                int dquant = DQUANT_TABLE[br.readNBit(2)];
                ctx.quant += dquant;
                if (ctx.quant > 31) {
                    ctx.quant = 31;
                } else if (ctx.quant < 1) {
                    ctx.quant = 1;
                }
            }
            mb.quant = ctx.quant;

            if (ctx.interlacing) {
                if (mb.cbp != 0 || intra) {
                    mb.fieldDCT = br.readBool();
                }

                if ((mb.mode == MODE_INTER || mb.mode == MODE_INTER_Q) && !mcsel) {
                    mb.fieldPred = br.readBool();

                    if (mb.fieldPred) {
                        mb.fieldForTop = br.readBool();
                        mb.fieldForBottom = br.readBool();
                    }
                }
            }

            if (mcsel) {
                readInterCoeffs(br, ctx, mb);
            } else if (mb.mode == MODE_INTER || mb.mode == MODE_INTER_Q || mb.mode == MODE_INTER4V) {
                if (mb.mode == MODE_INTER || mb.mode == MODE_INTER_Q) {
                    if (ctx.interlacing && mb.fieldPred) {
                        readMVInterlaced(br, ctx, mb.x, mb.y, 0, mb, fcode, mb.bound, mb, aboveMb, leftMb,
                                aboveRightMb);
                    } else {
                        readMV(br, ctx, mb.x, mb.y, 0, mb.mvs[0], fcode, mb.bound, mb, aboveMb, leftMb, aboveRightMb);
                        mb.mvs[1].x = mb.mvs[2].x = mb.mvs[3].x = mb.mvs[0].x;
                        mb.mvs[1].y = mb.mvs[2].y = mb.mvs[3].y = mb.mvs[0].y;
                    }
                } else if (mb.mode == MODE_INTER4V) {
                    readMV(br, ctx, mb.x, mb.y, 0, mb.mvs[0], fcode, mb.bound, mb, aboveMb, leftMb, aboveRightMb);
                    readMV(br, ctx, mb.x, mb.y, 1, mb.mvs[1], fcode, mb.bound, mb, aboveMb, leftMb, aboveRightMb);
                    readMV(br, ctx, mb.x, mb.y, 2, mb.mvs[2], fcode, mb.bound, mb, aboveMb, leftMb, aboveRightMb);
                    readMV(br, ctx, mb.x, mb.y, 3, mb.mvs[3], fcode, mb.bound, mb, aboveMb, leftMb, aboveRightMb);
                }
                if (!mb.fieldPred) {
                    readInterCoeffs(br, ctx, mb);
                } else {
                    readInterCoeffs(br, ctx, mb);
                }
            } else {
                mb.mvs[0].x = mb.mvs[1].x = mb.mvs[2].x = mb.mvs[3].x = 0;
                mb.mvs[0].y = mb.mvs[1].y = mb.mvs[2].y = mb.mvs[3].y = 0;
                readCoeffIntra(br, ctx, mb, aboveMb, leftMb, aboveLeftMb);
            }
        } else {
            mb.mode = MPEG4Consts.MODE_NOT_CODED;
            mb.quant = ctx.quant;

            mb.mvs[0].x = mb.mvs[1].x = mb.mvs[2].x = mb.mvs[3].x = 0;
            mb.mvs[0].y = mb.mvs[1].y = mb.mvs[2].y = mb.mvs[3].y = 0;
            mb.cbp = 0;

            readInterCoeffs(br, ctx, mb);
        }
    }

    private static void dequantH263Intra(MPEG4DecodingContext ctx, short[] block, final int quant, final int dcscalar) {
        final int quantM2 = quant << 1;
        final int quantAdd = ((quant & 1) != 0 ? quant : quant - 1);

        block[0] = (short) MathUtil.clip(block[0] * dcscalar, -2048, 2047);

        for (int i = 1; i < 64; i++) {
            int acLevel = block[i];

            if (acLevel == 0) {
                block[i] = 0;
            } else if (acLevel < 0) {
                acLevel = quantM2 * -acLevel + quantAdd;
                block[i] = (short) (acLevel <= 2048 ? -acLevel : -2048);
            } else {
                acLevel = quantM2 * acLevel + quantAdd;
                block[i] = (short) (acLevel <= 2047 ? acLevel : 2047);
            }
        }
    }

    private static void dequantMpegIntra(MPEG4DecodingContext ctx, short[] block, final int quant, final int dcscalar) {
        block[0] = (short) MathUtil.clip(block[0] * dcscalar, -2048, 2047);

        for (int i = 1; i < 64; i++) {
            if (block[i] == 0) {
                block[i] = 0;
            } else if (block[i] < 0) {
                int level = ((-block[i]) * ctx.intraMpegQuantMatrix[i] * quant) >> 3;

                block[i] = (short) (level <= 2048 ? -(short) level : -2048);
            } else {
                int level = (block[i] * ctx.intraMpegQuantMatrix[i] * quant) >> 3;

                block[i] = (short) (level <= 2047 ? level : 2047);
            }
        }
    }

    private static void readMV(BitReader br, MPEG4DecodingContext ctx, int x, int y, int k, Vector retMV, int fcode,
            int bound, Macroblock mb, Macroblock aboveMb, Macroblock leftMb, Macroblock aboveRightMb) {
        final int scaleFac = 1 << (fcode - 1);
        final int high = (32 * scaleFac) - 1;
        final int low = ((-32) * scaleFac);
        final int range = (64 * scaleFac);

        final Vector pmv = getPMV2(ctx, bound, x, y, k, mb, aboveMb, leftMb, aboveRightMb);
        Vector mv = Macroblock.vec();

        mv.x = readMVComponent(br, fcode);
        mv.y = readMVComponent(br, fcode);

        mv.x += pmv.x;
        mv.y += pmv.y;

        if (mv.x < low) {
            mv.x += range;
        } else if (mv.x > high) {
            mv.x -= range;
        }

        if (mv.y < low) {
            mv.y += range;
        } else if (mv.y > high) {
            mv.y -= range;
        }

        retMV.x = mv.x;
        retMV.y = mv.y;
    }

    private static void readMVInterlaced(BitReader br, MPEG4DecodingContext ctx, int x, int y, int k, Macroblock pMB,
            int fcode, int bound, Macroblock mb, Macroblock aboveMb, Macroblock leftMb, Macroblock aboveRightMb) {
        final int scaleFac = 1 << (fcode - 1);
        final int high = (32 * scaleFac) - 1;
        final int low = ((-32) * scaleFac);
        final int range = (64 * scaleFac);

        final Vector pmv = getPMV2Interlaced(ctx, bound, mb, aboveMb, leftMb, aboveRightMb);
        Vector mv = Macroblock.vec();
        Vector mvf1 = Macroblock.vec();
        Vector mvf2 = Macroblock.vec();

        if (!pMB.fieldPred) {
            mv.x = readMVComponent(br, fcode);
            mv.y = readMVComponent(br, fcode);

            mv.x += pmv.x;
            mv.y += pmv.y;

            if (mv.x < low) {
                mv.x += range;
            } else if (mv.x > high) {
                mv.x -= range;
            }

            if (mv.y < low) {
                mv.y += range;
            } else if (mv.y > high) {
                mv.y -= range;
            }

            pMB.mvs[0] = pMB.mvs[1] = pMB.mvs[2] = pMB.mvs[3] = mv;
        } else {
            mvf1.x = readMVComponent(br, fcode);
            mvf1.y = readMVComponent(br, fcode);

            mvf1.x += pmv.x;
            mvf1.y = 2 * (mvf1.y + pmv.y / 2);

            if (mvf1.x < low) {
                mvf1.x += range;
            } else if (mvf1.x > high) {
                mvf1.x -= range;
            }

            if (mvf1.y < low) {
                mvf1.y += range;
            } else if (mvf1.y > high) {
                mvf1.y -= range;
            }

            mvf2.x = readMVComponent(br, fcode);
            mvf2.y = readMVComponent(br, fcode);

            mvf2.x += pmv.x;
            mvf2.y = 2 * (mvf2.y + pmv.y / 2);

            if (mvf2.x < low) {
                mvf2.x += range;
            } else if (mvf2.x > high) {
                mvf2.x -= range;
            }

            if (mvf2.y < low) {
                mvf2.y += range;
            } else if (mvf2.y > high) {
                mvf2.y -= range;
            }

            pMB.mvs[0] = mvf1;
            pMB.mvs[1] = mvf2;
            pMB.mvs[2].x = pMB.mvs[3].x = 0;
            pMB.mvs[2].y = pMB.mvs[3].y = 0;

            int n = pMB.mvs[0].x + pMB.mvs[1].x;
            pMB.mvsAvg.x = (((n) >> 1) | ((n) & 1));
            n = pMB.mvs[0].y + pMB.mvs[1].y;
            pMB.mvsAvg.y = (((n) >> 1) | ((n) & 1));
        }
    }

    private static Vector getPMV2Interlaced(MPEG4DecodingContext ctx, int bound, Macroblock mb, Macroblock aboveMb,
            Macroblock leftMb, Macroblock aboveRightMb) {
        int num_cand = 0, last_cand = 1;

        Vector[] pmv = new Vector[4];
        for (int i = 0; i < 4; i++) {
            pmv[i] = Macroblock.vec();
        }

        int lz = 1;
        int tz = 2;
        int rz = 2;

        if (leftMb != null) {
            num_cand++;

            if (leftMb.fieldPred) {
                pmv[1] = leftMb.mvsAvg;
            } else {
                pmv[1] = leftMb.mvs[lz];
            }
        } else {
            pmv[1] = ZERO_MV;
        }

        if (aboveMb != null) {
            num_cand++;
            last_cand = 2;
            if (aboveMb.fieldPred) {
                pmv[2] = aboveMb.mvsAvg;
            } else {
                pmv[2] = aboveMb.mvs[tz];
            }
        } else {
            pmv[2] = ZERO_MV;
        }

        if (aboveRightMb != null) {
            num_cand++;
            last_cand = 3;
            if (aboveRightMb.fieldPred) {
                pmv[3] = aboveRightMb.mvsAvg;
            } else {
                pmv[3] = aboveRightMb.mvs[rz];
            }
        } else {
            pmv[3] = ZERO_MV;
        }

        if (num_cand > 1) {
            pmv[0].x = min(max(pmv[1].x, pmv[2].x), (min(max(pmv[2].x, pmv[3].x), max(pmv[1].x, pmv[3].x))));
            pmv[0].y = min(max(pmv[1].y, pmv[2].y), (min(max(pmv[2].y, pmv[3].y), max(pmv[1].y, pmv[3].y))));
            return pmv[0];
        }

        return pmv[last_cand];
    }

    private static int getDCScaler(int quant, boolean lum) {
        if (quant < 5)
            return 8;

        if (quant < 25 && !lum)
            return (quant + 13) / 2;

        if (quant < 9)
            return 2 * quant;

        if (quant < 25)
            return quant + 8;

        if (lum)
            return 2 * quant - 16;

        return quant - 6;
    }

    private static void predictAcdc(MPEG4DecodingContext ctx, int x, int y, int block, int currentQuant, int iDcScaler,
            short[] predictors, final int bound, Macroblock mb, Macroblock aboveMb, Macroblock leftMb,
            Macroblock aboveLeftMb) {
        final int mbpos = (y * ctx.mbWidth) + x;
        short[] leftPred = null, topPred = null, diag = null, current = null;

        int leftQuant = currentQuant;
        int topQuant = currentQuant;

        short[] pLeft = DEFAULT_ACDC_VALUES;
        short[] pTop = DEFAULT_ACDC_VALUES;
        short[] pDiag = DEFAULT_ACDC_VALUES;

        if (x != 0 && mbpos >= bound + 1 && (leftMb.mode == MODE_INTRA || leftMb.mode == MODE_INTRA_Q)) {
            leftPred = leftMb.predValues[0];
            leftQuant = leftMb.quant;
        }

        if (mbpos >= bound + ctx.mbWidth && (aboveMb.mode == MODE_INTRA || aboveMb.mode == MODE_INTRA_Q)) {
            topPred = aboveMb.predValues[0];
            topQuant = aboveMb.quant;
        }

        if (x != 0 && mbpos >= bound + ctx.mbWidth + 1
                && (aboveLeftMb.mode == MODE_INTRA || aboveLeftMb.mode == MODE_INTRA_Q)) {
            diag = aboveLeftMb.predValues[0];
        }

        current = mb.predValues[0];

        switch (block) {
        case 0:
            if (leftPred != null)
                pLeft = leftMb.predValues[1];

            if (topPred != null)
                pTop = aboveMb.predValues[2];

            if (diag != null)
                pDiag = aboveLeftMb.predValues[3];

            break;

        case 1:
            pLeft = current;
            leftQuant = currentQuant;

            if (topPred != null) {
                pTop = aboveMb.predValues[3];
                pDiag = aboveMb.predValues[2];
            }
            break;

        case 2:
            if (leftPred != null) {
                pLeft = leftMb.predValues[3];
                pDiag = leftMb.predValues[1];
            }

            pTop = current;
            topQuant = currentQuant;

            break;

        case 3:
            pLeft = mb.predValues[2];
            leftQuant = currentQuant;

            pTop = mb.predValues[1];
            topQuant = currentQuant;

            pDiag = current;

            break;

        case 4:
            if (leftPred != null)
                pLeft = leftMb.predValues[4];

            if (topPred != null)
                pTop = aboveMb.predValues[4];

            if (diag != null)
                pDiag = aboveLeftMb.predValues[4];

            break;

        case 5:
            if (leftPred != null)
                pLeft = leftMb.predValues[5];

            if (topPred != null)
                pTop = aboveMb.predValues[5];

            if (diag != null)
                pDiag = aboveLeftMb.predValues[5];

            break;
        }

        if (abs(pLeft[0] - pDiag[0]) < abs(pDiag[0] - pTop[0])) {
            mb.acpredDirections[block] = 1; // vertical
            predictors[0] = (short) (pTop[0] > 0 ? ((pTop[0] + (iDcScaler >>> 1)) / iDcScaler)
                    : ((pTop[0] - (iDcScaler >>> 1)) / iDcScaler));
            for (int i = 1; i < 8; i++) {
                int a = pTop[i] * topQuant;
                predictors[i] = (pTop[i] != 0
                        ? (short) (a > 0 ? ((a + (currentQuant >>> 1)) / currentQuant)
                                : ((a - (currentQuant >>> 1)) / currentQuant))
                        : 0);
            }
        } else {
            mb.acpredDirections[block] = 2;
            predictors[0] = (short) (pLeft[0] > 0 ? ((pLeft[0] + (iDcScaler >>> 1)) / iDcScaler)
                    : ((pLeft[0] - (iDcScaler >>> 1)) / iDcScaler));
            for (int i = 1; i < 8; i++) {
                int a = pLeft[i + 7] * leftQuant;
                predictors[i] = (pLeft[i + 7] != 0
                        ? (short) (a > 0 ? ((a + (currentQuant >>> 1)) / currentQuant)
                                : ((a - (currentQuant >>> 1)) / currentQuant))
                        : 0);
            }
        }
    }

    private static Vector getPMV2(MPEG4DecodingContext ctx, int bound, int x, int y, int block, Macroblock mb,
            Macroblock aboveMb, Macroblock leftMb, Macroblock aboveRightMb) {
        int num_cand = 0, last_cand = 1;

        Vector[] pmv = new Vector[4];
        for (int i = 0; i < 4; i++) {
            pmv[i] = Macroblock.vec();
        }
        int lz, tz, rz;
        switch (block) {
        case 0:
            lz = 1;
            tz = 2;
            rz = 2;
            break;
        case 1:
            leftMb = mb;
            lz = 0;
            tz = 3;
            rz = 2;
            break;
        case 2:
            aboveMb = mb;
            aboveRightMb = mb;
            lz = 3;
            tz = 0;
            rz = 1;
            break;
        default:
            leftMb = mb;
            aboveMb = mb;
            aboveRightMb = mb;
            lz = 2;
            tz = 0;
            rz = 1;
        }

        if (leftMb != null) {
            num_cand++;
            pmv[1] = leftMb.mvs[lz];
        } else {
            pmv[1] = ZERO_MV;
        }

        if (aboveMb != null) {
            num_cand++;
            last_cand = 2;
            pmv[2] = aboveMb.mvs[tz];
        } else {
            pmv[2] = ZERO_MV;
        }

        if (aboveRightMb != null) {
            num_cand++;
            last_cand = 3;
            pmv[3] = aboveRightMb.mvs[rz];
        } else {
            pmv[3] = ZERO_MV;
        }

        if (num_cand > 1) {
            pmv[0].x = selectCand(pmv[1].x, pmv[2].x, pmv[3].x);
            pmv[0].y = selectCand(pmv[1].y, pmv[2].y, pmv[3].y);
            return pmv[0];
        }

        return pmv[last_cand];
    }

    private static int selectCand(int p1x, int p2x, int p3x) {
        int neg12x = neg(p1x - p2x);
        int neg13x = neg(p1x - p3x);
        int neg23x = neg(p2x - p3x);
        int neg1x = neg(p1x - p2x + neg23x - neg13x);
        int neg2x = neg(p2x - p1x + neg1x + neg12x - neg23x);
        return p1x - neg12x + neg2x;
    }

    static int neg(int v) {
        return v < 0 ? v : 0;
    }

    static void readBi(BitReader br, MPEG4DecodingContext ctx, int fcodeForward, int fcodeBackward, Macroblock mb,
            Macroblock lastMB, Vector pFMV, Vector pBMV) {
        if (!br.readBool()) {
            boolean modb2 = br.readBool();

            mb.mode = readMBType(br);

            if (!modb2) {
                mb.cbp = br.readNBit(6);
            } else {
                mb.cbp = 0;
            }

            if (mb.mode != 0 && mb.cbp != 0) {
                mb.quant += MPEG4Bitstream.readDBQuant(br);
                if (mb.quant > 31)
                    mb.quant = 31;
                else if (mb.quant < 1)
                    mb.quant = 1;
            }

            if (ctx.interlacing) {
                if (mb.cbp != 0) {
                    mb.fieldDCT = br.readBool();
                }

                if (mb.mode != 0) {
                    mb.fieldPred = br.readBool();

                    if (mb.fieldPred) {
                        mb.fieldForTop = br.readBool();
                        mb.fieldForBottom = br.readBool();
                    }
                }
            }

        } else {
            mb.mode = MODE_DIRECT_NONE_MV;
            mb.cbp = 0;
        }

        Vector mv = Macroblock.vec();
        switch (mb.mode) {
        case MODE_DIRECT:
            getBMotionVector(br, mv, 1, ZERO_MV, mb.x, mb.y);

        case MODE_DIRECT_NONE_MV:
            for (int i = 0; i < 4; i++) {
                mb.mvs[i].x = lastMB.mvs[i].x * ctx.bframeTs / ctx.pframeTs + mv.x;
                mb.mvs[i].y = lastMB.mvs[i].y * ctx.bframeTs / ctx.pframeTs + mv.y;

                mb.bmvs[i].x = (mv.x != 0) ? mb.mvs[i].x - lastMB.mvs[i].x
                        : lastMB.mvs[i].x * (ctx.bframeTs - ctx.pframeTs) / ctx.pframeTs;
                mb.bmvs[i].y = (mv.y != 0) ? mb.mvs[i].y - lastMB.mvs[i].y
                        : lastMB.mvs[i].y * (ctx.bframeTs - ctx.pframeTs) / ctx.pframeTs;
            }

            readInterCoeffs(br, ctx, mb);
            break;

        case MODE_INTERPOLATE:
            getBMotionVector(br, mb.mvs[0], fcodeForward, pFMV, mb.x, mb.y);
            pFMV.x = mb.mvs[1].x = mb.mvs[2].x = mb.mvs[3].x = mb.mvs[0].x;
            pFMV.y = mb.mvs[1].y = mb.mvs[2].y = mb.mvs[3].y = mb.mvs[0].y;

            getBMotionVector(br, mb.bmvs[0], fcodeBackward, pBMV, mb.x, mb.y);
            pBMV.x = mb.bmvs[1].x = mb.bmvs[2].x = mb.bmvs[3].x = mb.bmvs[0].x;
            pBMV.y = mb.bmvs[1].y = mb.bmvs[2].y = mb.bmvs[3].y = mb.bmvs[0].y;

            readInterCoeffs(br, ctx, mb);
            break;

        case MODE_BACKWARD:
            getBMotionVector(br, mb.mvs[0], fcodeBackward, pBMV, mb.x, mb.y);
            pBMV.x = mb.mvs[1].x = mb.mvs[2].x = mb.mvs[3].x = mb.mvs[0].x;
            pBMV.y = mb.mvs[1].y = mb.mvs[2].y = mb.mvs[3].y = mb.mvs[0].y;

            readInterCoeffs(br, ctx, mb);
            break;

        case MODE_FORWARD:
            getBMotionVector(br, mb.mvs[0], fcodeForward, pFMV, mb.x, mb.y);
            pFMV.x = mb.mvs[1].x = mb.mvs[2].x = mb.mvs[3].x = mb.mvs[0].x;
            pFMV.y = mb.mvs[1].y = mb.mvs[2].y = mb.mvs[3].y = mb.mvs[0].y;

            readInterCoeffs(br, ctx, mb);
            break;

        default:
        }
    }

    private static void getBMotionVector(BitReader br, Vector mv, int fcode, Vector pmv, int x, int y) {
        final int scaleFac = 1 << (fcode - 1);
        final int high = (32 * scaleFac) - 1;
        final int low = ((-32) * scaleFac);
        final int range = (64 * scaleFac);

        int mvX = readMVComponent(br, fcode);
        int mvY = readMVComponent(br, fcode);

        mvX += pmv.x;
        mvY += pmv.y;

        if (mvX < low)
            mvX += range;
        else if (mvX > high)
            mvX -= range;

        if (mvY < low)
            mvY += range;
        else if (mvY > high)
            mvY -= range;

        mv.x = mvX;
        mv.y = mvY;
    }

    static void readInterCoeffs(BitReader br, MPEG4DecodingContext ctx, Macroblock mb) {
        final int iQuant = mb.quant;
        final int direction = ctx.alternateVerticalScan ? 2 : 0;

        for (int i = 0; i < 6; i++) {
            short[] block = mb.block[i];
            Arrays.fill(block, (byte) 0);
            if ((mb.cbp & (1 << (5 - i))) != 0) {
                if (ctx.quantType) {
                    readInterBlockMPEG(br, block, direction, iQuant, ctx.interMpegQuantMatrix);
                } else {
                    readInterBlockH263(br, block, direction, iQuant);
                }
            }
        }
    }
}
