package org.jcodec.codecs.aac;

public enum ObjectType {
    AOT_NULL, AOT_AAC_MAIN, // /< Y Main
    AOT_AAC_LC, // /< Y Low Complexity
    AOT_AAC_SSR, // /< N (code in SoC repo) Scalable Sample Rate
    AOT_AAC_LTP, // /< Y Long Term Prediction
    AOT_SBR, // /< Y Spectral Band Replication
    AOT_AAC_SCALABLE, // /< N Scalable
    AOT_TWINVQ, // /< N Twin Vector Quantizer
    AOT_CELP, // /< N Code Excited Linear Prediction
    AOT_HVXC, // /< N Harmonic Vector eXcitation Coding
    CRAP1, CRAP2, AOT_TTSI, // /< N Text-To-Speech Interface
    AOT_MAINSYNTH, // /< N Main Synthesis
    AOT_WAVESYNTH, // /< N Wavetable Synthesis
    AOT_MIDI, // /< N General MIDI
    AOT_SAFX, // /< N Algorithmic Synthesis and Audio Effects
    AOT_ER_AAC_LC, // /< N Error Resilient Low Complexity
    CRAP3, AOT_ER_AAC_LTP, // /< N Error Resilient Long Term Prediction
    AOT_ER_AAC_SCALABLE, // /< N Error Resilient Scalable
    AOT_ER_TWINVQ, // /< N Error Resilient Twin Vector Quantizer
    AOT_ER_BSAC, // /< N Error Resilient Bit-Sliced Arithmetic Coding
    AOT_ER_AAC_LD, // /< N Error Resilient Low Delay
    AOT_ER_CELP, // /< N Error Resilient Code Excited Linear Prediction
    AOT_ER_HVXC, // /< N Error Resilient Harmonic Vector eXcitation Coding
    AOT_ER_HILN, // /< N Error Resilient Harmonic and Individual Lines plus
                 // Noise
    AOT_ER_PARAM, // /< N Error Resilient Parametric
    AOT_SSC, // /< N SinuSoidal Coding
    AOT_PS, // /< N Parametric Stereo
    AOT_SURROUND, // /< N MPEG Surround
    AOT_ESCAPE, // /< Y Escape Value
    AOT_L1, // /< Y Layer 1
    AOT_L2, // /< Y Layer 2
    AOT_L3, // /< Y Layer 3
    AOT_DST, // /< N Direct Stream Transfer
    AOT_ALS, // /< Y Audio LosslesS
    AOT_SLS, // /< N Scalable LosslesS
    AOT_SLS_NON_CORE, // /< N Scalable LosslesS (non core)
    AOT_ER_AAC_ELD, // /< N Error Resilient Enhanced Low Delay
    AOT_SMR_SIMPLE, // /< N Symbolic Music Representation Simple
    AOT_SMR_MAIN, // /< N Symbolic Music Representation Main
    AOT_USAC_NOSBR, // /< N Unified Speech and Audio Coding (no SBR)
    AOT_SAOC, // /< N Spatial Audio Object Coding
    AOT_LD_SURROUND, // /< N Low Delay MPEG Surround
    AOT_USAC
}
