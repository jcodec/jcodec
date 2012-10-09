package org.jcodec.containers.mp4.boxes.channel;

import java.util.Arrays;
import java.util.List;


/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class ChannelLayout {
    public final static List<Label> stereo = Arrays.asList(Label.Left, Label.Right);

    public final static int kCAFChannelLayoutTag_UseChannelDescriptions = (0 << 16) | 0;
    // use the array of AudioChannelDescriptions to define the mapping.
    public final static int kCAFChannelLayoutTag_UseChannelBitmap = (1 << 16) | 0;
    // use the bitmap to define the mapping.

    // 1 Channel Layout
    public final static int kCAFChannelLayoutTag_Mono = (100 << 16) | 1;
    // a standard mono stream

    // 2 Channel layouts
    public final static int kCAFChannelLayoutTag_Stereo = (101 << 16) | 2;
    // a standard stereo stream (L R)
    public final static int kCAFChannelLayoutTag_StereoHeadphones = (102 << 16) | 2;
    // a standard stereo stream (L R) - implied headphone playback
    public final static int kCAFChannelLayoutTag_MatrixStereo = (103 << 16) | 2;
    // a matrix encoded stereo stream (Lt, Rt)
    public final static int kCAFChannelLayoutTag_MidSide = (104 << 16) | 2;
    // mid/side recording
    public final static int kCAFChannelLayoutTag_XY = (105 << 16) | 2;
    // coincident mic pair (often 2 figure 8's)
    public final static int kCAFChannelLayoutTag_Binaural = (106 << 16) | 2;
    // binaural stereo (left, right)

    // Symmetric arrangements - same distance between speaker locations
    public final static int kCAFChannelLayoutTag_Ambisonic_B_Format = (107 << 16) | 4;
    // W, X, Y, Z
    public final static int kCAFChannelLayoutTag_Quadraphonic = (108 << 16) | 4;
    // front left, front right, back left, back right
    public final static int kCAFChannelLayoutTag_Pentagonal = (109 << 16) | 5;
    // left, right, rear left, rear right, center
    public final static int kCAFChannelLayoutTag_Hexagonal = (110 << 16) | 6;
    // left, right, rear left, rear right, center, rear
    public final static int kCAFChannelLayoutTag_Octagonal = (111 << 16) | 8;
    // front left, front right, rear left, rear right;
    // front center, rear center, side left, side right
    public final static int kCAFChannelLayoutTag_Cube = (112 << 16) | 8;
    // left, right, rear left, rear right
    // top left, top right, top rear left, top rear right

    // MPEG defined layouts
    public final static int kCAFChannelLayoutTag_MPEG_1_0 = kCAFChannelLayoutTag_Mono; // C
    public final static int kCAFChannelLayoutTag_MPEG_2_0 = kCAFChannelLayoutTag_Stereo; // L
                                                                                         // R
    public final static int kCAFChannelLayoutTag_MPEG_3_0_A = (113 << 16) | 3; // L
                                                                               // R
                                                                               // C
    public final static int kCAFChannelLayoutTag_MPEG_3_0_B = (114 << 16) | 3; // C
                                                                               // L
                                                                               // R
    public final static int kCAFChannelLayoutTag_MPEG_4_0_A = (115 << 16) | 4; // L
                                                                               // R
                                                                               // C
                                                                               // Cs
    public final static int kCAFChannelLayoutTag_MPEG_4_0_B = (116 << 16) | 4; // C
                                                                               // L
                                                                               // R
                                                                               // Cs
    public final static int kCAFChannelLayoutTag_MPEG_5_0_A = (117 << 16) | 5; // L
                                                                               // R
                                                                               // C
                                                                               // Ls
                                                                               // Rs
    public final static int kCAFChannelLayoutTag_MPEG_5_0_B = (118 << 16) | 5; // L
                                                                               // R
                                                                               // Ls
                                                                               // Rs
                                                                               // C
    public final static int kCAFChannelLayoutTag_MPEG_5_0_C = (119 << 16) | 5; // L
                                                                               // C
                                                                               // R
                                                                               // Ls
                                                                               // Rs
    public final static int kCAFChannelLayoutTag_MPEG_5_0_D = (120 << 16) | 5; // C
                                                                               // L
                                                                               // R
                                                                               // Ls
                                                                               // Rs
    public final static int kCAFChannelLayoutTag_MPEG_5_1_A = (121 << 16) | 6; // L
                                                                               // R
                                                                               // C
                                                                               // LFE
                                                                               // Ls
                                                                               // Rs
    public final static int kCAFChannelLayoutTag_MPEG_5_1_B = (122 << 16) | 6; // L
                                                                               // R
                                                                               // Ls
                                                                               // Rs
                                                                               // C
                                                                               // LFE
    public final static int kCAFChannelLayoutTag_MPEG_5_1_C = (123 << 16) | 6; // L
                                                                               // C
                                                                               // R
                                                                               // Ls
                                                                               // Rs
                                                                               // LFE
    public final static int kCAFChannelLayoutTag_MPEG_5_1_D = (124 << 16) | 6; // C
                                                                               // L
                                                                               // R
                                                                               // Ls
                                                                               // Rs
                                                                               // LFE
    public final static int kCAFChannelLayoutTag_MPEG_6_1_A = (125 << 16) | 7; // L
                                                                               // R
                                                                               // C
                                                                               // LFE
                                                                               // Ls
                                                                               // Rs
                                                                               // Cs
    public final static int kCAFChannelLayoutTag_MPEG_7_1_A = (126 << 16) | 8; // L
                                                                               // R
                                                                               // C
                                                                               // LFE
                                                                               // Ls
                                                                               // Rs
                                                                               // Lc
                                                                               // Rc
    public final static int kCAFChannelLayoutTag_MPEG_7_1_B = (127 << 16) | 8; // C
                                                                               // Lc
                                                                               // Rc
                                                                               // L
                                                                               // R
                                                                               // Ls
                                                                               // Rs
                                                                               // LFE
    public final static int kCAFChannelLayoutTag_MPEG_7_1_C = (128 << 16) | 8; // L
                                                                               // R
                                                                               // C
                                                                               // LFE
                                                                               // Ls
                                                                               // R
                                                                               // Rls
                                                                               // Rrs
    public final static int kCAFChannelLayoutTag_Emagic_Default_7_1 = (129 << 16) | 8;
    // L R Ls Rs C LFE Lc Rc
    public final static int kCAFChannelLayoutTag_SMPTE_DTV = (130 << 16) | 8;
    // L R C LFE Ls Rs Lt Rt
    // (kCAFChannelLayoutTag_ITU_5_1 plus a matrix encoded stereo mix)

    // ITU defined layouts
    public final static int kCAFChannelLayoutTag_ITU_1_0 = kCAFChannelLayoutTag_Mono; // C
    public final static int kCAFChannelLayoutTag_ITU_2_0 = kCAFChannelLayoutTag_Stereo; // L
                                                                                        // R
    public final static int kCAFChannelLayoutTag_ITU_2_1 = (131 << 16) | 3; // L
                                                                            // R
                                                                            // Cs
    public final static int kCAFChannelLayoutTag_ITU_2_2 = (132 << 16) | 4; // L
                                                                            // R
                                                                            // Ls
                                                                            // Rs
    public final static int kCAFChannelLayoutTag_ITU_3_0 = kCAFChannelLayoutTag_MPEG_3_0_A; // L
                                                                                            // R
                                                                                            // C
    public final static int kCAFChannelLayoutTag_ITU_3_1 = kCAFChannelLayoutTag_MPEG_4_0_A; // L
                                                                                            // R
                                                                                            // C
                                                                                            // Cs
    public final static int kCAFChannelLayoutTag_ITU_3_2 = kCAFChannelLayoutTag_MPEG_5_0_A; // L
                                                                                            // R
                                                                                            // C
                                                                                            // Ls
                                                                                            // Rs
    public final static int kCAFChannelLayoutTag_ITU_3_2_1 = kCAFChannelLayoutTag_MPEG_5_1_A;
    // L R C LFE Ls Rs
    public final static int kCAFChannelLayoutTag_ITU_3_4_1 = kCAFChannelLayoutTag_MPEG_7_1_C;
    // L R C LFE Ls Rs Rls Rrs

    // DVD defined layouts
    public final static int kCAFChannelLayoutTag_DVD_0 = kCAFChannelLayoutTag_Mono; // C
                                                                                    // (mono)
    public final static int kCAFChannelLayoutTag_DVD_1 = kCAFChannelLayoutTag_Stereo; // L
                                                                                      // R
    public final static int kCAFChannelLayoutTag_DVD_2 = kCAFChannelLayoutTag_ITU_2_1; // L
                                                                                       // R
                                                                                       // Cs
    public final static int kCAFChannelLayoutTag_DVD_3 = kCAFChannelLayoutTag_ITU_2_2; // L
                                                                                       // R
                                                                                       // Ls
                                                                                       // Rs
    public final static int kCAFChannelLayoutTag_DVD_4 = (133 << 16) | 3; // L R
                                                                          // LFE
    public final static int kCAFChannelLayoutTag_DVD_5 = (134 << 16) | 4; // L R
                                                                          // LFE
                                                                          // Cs
    public final static int kCAFChannelLayoutTag_DVD_6 = (135 << 16) | 5; // L R
                                                                          // LFE
                                                                          // Ls
                                                                          // Rs
    public final static int kCAFChannelLayoutTag_DVD_7 = kCAFChannelLayoutTag_MPEG_3_0_A;// L
                                                                                         // R
                                                                                         // C
    public final static int kCAFChannelLayoutTag_DVD_8 = kCAFChannelLayoutTag_MPEG_4_0_A;// L
                                                                                         // R
                                                                                         // C
                                                                                         // Cs
    public final static int kCAFChannelLayoutTag_DVD_9 = kCAFChannelLayoutTag_MPEG_5_0_A;// L
                                                                                         // R
                                                                                         // C
                                                                                         // Ls
                                                                                         // Rs
    public final static int kCAFChannelLayoutTag_DVD_10 = (136 << 16) | 4; // L
                                                                           // R
                                                                           // C
                                                                           // LFE
    public final static int kCAFChannelLayoutTag_DVD_11 = (137 << 16) | 5; // L
                                                                           // R
                                                                           // C
                                                                           // LFE
                                                                           // Cs
    public final static int kCAFChannelLayoutTag_DVD_12 = kCAFChannelLayoutTag_MPEG_5_1_A;// L
                                                                                          // R
                                                                                          // C
                                                                                          // LFE
                                                                                          // Ls
                                                                                          // Rs
    // 13 through 17 are duplicates of 8 through 12.
    public final static int kCAFChannelLayoutTag_DVD_13 = kCAFChannelLayoutTag_DVD_8; // L
                                                                                      // R
                                                                                      // C
                                                                                      // Cs
    public final static int kCAFChannelLayoutTag_DVD_14 = kCAFChannelLayoutTag_DVD_9; // L
                                                                                      // R
                                                                                      // C
                                                                                      // Ls
                                                                                      // Rs
    public final static int kCAFChannelLayoutTag_DVD_15 = kCAFChannelLayoutTag_DVD_10; // L
                                                                                       // R
                                                                                       // C
                                                                                       // LFE
    public final static int kCAFChannelLayoutTag_DVD_16 = kCAFChannelLayoutTag_DVD_11; // L
                                                                                       // R
                                                                                       // C
                                                                                       // LFE
                                                                                       // Cs
    public final static int kCAFChannelLayoutTag_DVD_17 = kCAFChannelLayoutTag_DVD_12; // L
                                                                                       // R
                                                                                       // C
                                                                                       // LFE
                                                                                       // Ls
                                                                                       // Rs
    public final static int kCAFChannelLayoutTag_DVD_18 = (138 << 16) | 5; // L
                                                                           // R
                                                                           // Ls
                                                                           // Rs
                                                                           // LFE
    public final static int kCAFChannelLayoutTag_DVD_19 = kCAFChannelLayoutTag_MPEG_5_0_B;// L
                                                                                          // R
                                                                                          // Ls
                                                                                          // Rs
                                                                                          // C
    public final static int kCAFChannelLayoutTag_DVD_20 = kCAFChannelLayoutTag_MPEG_5_1_B;// L
                                                                                          // R
                                                                                          // Ls
                                                                                          // Rs
                                                                                          // C
                                                                                          // LFE

    // These layouts are recommended for Mac OS X's AudioUnit use
    // These are the symmetrical layouts
    public final static int kCAFChannelLayoutTag_AudioUnit_4 = kCAFChannelLayoutTag_Quadraphonic;
    public final static int kCAFChannelLayoutTag_AudioUnit_5 = kCAFChannelLayoutTag_Pentagonal;
    public final static int kCAFChannelLayoutTag_AudioUnit_6 = kCAFChannelLayoutTag_Hexagonal;
    public final static int kCAFChannelLayoutTag_AudioUnit_8 = kCAFChannelLayoutTag_Octagonal;
    // These are the surround-based layouts
    public final static int kCAFChannelLayoutTag_AudioUnit_5_0 = kCAFChannelLayoutTag_MPEG_5_0_B;
    // L R Ls Rs C
    public final static int kCAFChannelLayoutTag_AudioUnit_6_0 = (139 << 16) | 6; // L
                                                                                  // R
                                                                                  // Ls
                                                                                  // Rs
                                                                                  // C
                                                                                  // Cs
    public final static int kCAFChannelLayoutTag_AudioUnit_7_0 = (140 << 16) | 7; // L
                                                                                  // R
                                                                                  // Ls
                                                                                  // Rs
                                                                                  // C
                                                                                  // Rls
                                                                                  // Rrs
    public final static int kCAFChannelLayoutTag_AudioUnit_5_1 = kCAFChannelLayoutTag_MPEG_5_1_A;
    // L R C LFE Ls Rs
    public final static int kCAFChannelLayoutTag_AudioUnit_6_1 = kCAFChannelLayoutTag_MPEG_6_1_A;
    // L R C LFE Ls Rs Cs
    public final static int kCAFChannelLayoutTag_AudioUnit_7_1 = kCAFChannelLayoutTag_MPEG_7_1_C;
    // L R C LFE Ls Rs Rls Rrs

    // These layouts are used for AAC Encoding within the MPEG-4 Specification
    public final static int kCAFChannelLayoutTag_AAC_Quadraphonic = kCAFChannelLayoutTag_Quadraphonic;
    // L R Ls Rs
    public final static int kCAFChannelLayoutTag_AAC_4_0 = kCAFChannelLayoutTag_MPEG_4_0_B; // C
                                                                                            // L
                                                                                            // R
                                                                                            // Cs
    public final static int kCAFChannelLayoutTag_AAC_5_0 = kCAFChannelLayoutTag_MPEG_5_0_D; // C
                                                                                            // L
                                                                                            // R
                                                                                            // Ls
                                                                                            // Rs
    public final static int kCAFChannelLayoutTag_AAC_5_1 = kCAFChannelLayoutTag_MPEG_5_1_D; // C
                                                                                            // L
                                                                                            // R
                                                                                            // Ls
                                                                                            // Rs
                                                                                            // Lfe
    public final static int kCAFChannelLayoutTag_AAC_6_0 = (141 << 16) | 6; // C
                                                                            // L
                                                                            // R
                                                                            // Ls
                                                                            // Rs
                                                                            // Cs
    public final static int kCAFChannelLayoutTag_AAC_6_1 = (142 << 16) | 7; // C
                                                                            // L
                                                                            // R
                                                                            // Ls
                                                                            // Rs
                                                                            // Cs
                                                                            // Lfe
    public final static int kCAFChannelLayoutTag_AAC_7_0 = (143 << 16) | 7; // C
                                                                            // L
                                                                            // R
                                                                            // Ls
                                                                            // Rs
                                                                            // Rls
                                                                            // Rrs
    public final static int kCAFChannelLayoutTag_AAC_7_1 = kCAFChannelLayoutTag_MPEG_7_1_B;
    // C Lc Rc L R Ls Rs Lfe
    public final static int kCAFChannelLayoutTag_AAC_Octagonal = (144 << 16) | 8; // C
                                                                                  // L
                                                                                  // R
                                                                                  // Ls
                                                                                  // Rs
                                                                                  // Rls
                                                                                  // Rrs
                                                                                  // Cs
    public final static int kCAFChannelLayoutTag_TMH_10_2_std = (145 << 16) | 16;
    // L R C Vhc Lsd Rsd Ls Rs Vhl Vhr Lw Rw Csd Cs LFE1 LFE2
    public final static int kCAFChannelLayoutTag_TMH_10_2_full = (146 << 16) | 21;
    // TMH_10_2_std plus: Lc Rc HI VI Haptic
    public final static int kCAFChannelLayoutTag_RESERVED_DO_NOT_USE = (147 << 16);
}