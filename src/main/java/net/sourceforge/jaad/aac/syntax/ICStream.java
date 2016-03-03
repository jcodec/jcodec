package net.sourceforge.jaad.aac.syntax;
import java.util.Arrays;
import java.util.logging.Level;
import net.sourceforge.jaad.aac.AACException;
import net.sourceforge.jaad.aac.ChannelConfiguration;
import net.sourceforge.jaad.aac.AACDecoderConfig;
import net.sourceforge.jaad.aac.error.RVLC;
import net.sourceforge.jaad.aac.gain.GainControl;
import net.sourceforge.jaad.aac.huffman.HCB;
import net.sourceforge.jaad.aac.huffman.Huffman;
import net.sourceforge.jaad.aac.tools.TNS;

/**
 * This class is part of JAAD ( jaadec.sourceforge.net ) that is distributed
 * under the Public Domain license. Code changes provided by the JCodec project
 * are distributed under FreeBSD license.
 * 
 * @author in-somnia
 */
//TODO: apply pulse data
public class ICStream implements SyntaxConstants, HCB, ScaleFactorTable, IQTable {

	private static final int SF_DELTA = 60;
	private static final int SF_OFFSET = 200;
	private static int randomState = 0x1F2E3D4C;
	private final int frameLength;
	//always needed
	private final ICSInfo info;
	private final int[] sfbCB;
	private final int[] sectEnd;
	private final float[] data;
	private final float[] scaleFactors;
	private int globalGain;
	private boolean pulseDataPresent, tnsDataPresent, gainControlPresent;
	//only allocated if needed
	private TNS tns;
	private GainControl gainControl;
	private int[] pulseOffset, pulseAmp;
	private int pulseCount;
	private int pulseStartSWB;
	//error resilience
	private boolean noiseUsed;
	private int reorderedSpectralDataLen, longestCodewordLen;
	private RVLC rvlc;

	public ICStream(int frameLength) {
		this.frameLength = frameLength;
		info = new ICSInfo(frameLength);
		sfbCB = new int[MAX_SECTIONS];
		sectEnd = new int[MAX_SECTIONS];
		data = new float[frameLength];
		scaleFactors = new float[MAX_SECTIONS];
	}

	/* ========= decoding ========== */
	public void decode(IBitStream _in, boolean commonWindow, AACDecoderConfig conf) throws AACException {
		if(conf.isScalefactorResilienceUsed()&&rvlc==null) rvlc = new RVLC();
		final boolean er = conf.getProfile().isErrorResilientProfile();

		globalGain = _in.readBits(8);

		if(!commonWindow) info.decode(_in, conf, commonWindow);

		decodeSectionData(_in, conf.isSectionDataResilienceUsed());

		//if(conf.isScalefactorResilienceUsed()) rvlc.decode(_in, this, scaleFactors);
		/*else*/ decodeScaleFactors(_in);

		pulseDataPresent = _in.readBool();
		if(pulseDataPresent) {
			if(info.isEightShortFrame()) throw new AACException("pulse data not allowed for short frames");
			LOGGER.log(Level.FINE, "PULSE");
			decodePulseData(_in);
		}

		tnsDataPresent = _in.readBool();
		if(tnsDataPresent&&!er) {
			if(tns==null) tns = new TNS();
			tns.decode(_in, info);
		}

		gainControlPresent = _in.readBool();
		if(gainControlPresent) {
			if(gainControl==null) gainControl = new GainControl(frameLength);
			LOGGER.log(Level.FINE, "GAIN");
			gainControl.decode(_in, info.getWindowSequence());
		}

		//RVLC spectral data
		//if(conf.isScalefactorResilienceUsed()) rvlc.decodeScalefactors(this, _in, scaleFactors);

		if(conf.isSpectralDataResilienceUsed()) {
			int max = (conf.getChannelConfiguration()==ChannelConfiguration.CHANNEL_CONFIG_STEREO) ? 6144 : 12288;
			reorderedSpectralDataLen = Math.max(_in.readBits(14), max);
			longestCodewordLen = Math.max(_in.readBits(6), 49);
			//HCR.decodeReorderedSpectralData(this, _in, data, conf.isSectionDataResilienceUsed());
		}
		else decodeSpectralData(_in);
	}

	public void decodeSectionData(IBitStream _in, boolean sectionDataResilienceUsed) throws AACException {
		Arrays.fill(sfbCB, 0);
		Arrays.fill(sectEnd, 0);
		final int bits = info.isEightShortFrame() ? 3 : 5;
		final int escVal = (1<<bits)-1;

		final int windowGroupCount = info.getWindowGroupCount();
		final int maxSFB = info.getMaxSFB();

		int end, cb, incr;
		int idx = 0;

		for(int g = 0; g<windowGroupCount; g++) {
			int k = 0;
			while(k<maxSFB) {
				end = k;
				cb = _in.readBits(4);
				if(cb==12) throw new AACException("invalid huffman codebook: 12");
				while((incr = _in.readBits(bits))==escVal) {
					end += incr;
				}
				end += incr;
				if(end>maxSFB) throw new AACException("too many bands: "+end+", allowed: "+maxSFB);
				for(; k<end; k++) {
					sfbCB[idx] = cb;
					sectEnd[idx++] = end;
				}
			}
		}
	}

	private void decodePulseData(IBitStream _in) throws AACException {
		pulseCount = _in.readBits(2)+1;
		pulseStartSWB = _in.readBits(6);
		if(pulseStartSWB>=info.getSWBCount()) throw new AACException("pulse SWB out of range: "+pulseStartSWB+" > "+info.getSWBCount());

		if(pulseOffset==null||pulseCount!=pulseOffset.length) {
			//only reallocate if needed
			pulseOffset = new int[pulseCount];
			pulseAmp = new int[pulseCount];
		}

		pulseOffset[0] = info.getSWBOffsets()[pulseStartSWB];
		pulseOffset[0] += _in.readBits(5);
		pulseAmp[0] = _in.readBits(4);
		for(int i = 1; i<pulseCount; i++) {
			pulseOffset[i] = _in.readBits(5)+pulseOffset[i-1];
			if(pulseOffset[i]>1023) throw new AACException("pulse offset out of range: "+pulseOffset[0]);
			pulseAmp[i] = _in.readBits(4);
		}
	}

	public void decodeScaleFactors(IBitStream _in) throws AACException {
		final int windowGroups = info.getWindowGroupCount();
		final int maxSFB = info.getMaxSFB();
		//0: spectrum, 1: noise, 2: intensity
		final int[] offset = {globalGain, globalGain-90, 0};

		int tmp;
		boolean noiseFlag = true;

		int sfb, idx = 0;
		for(int g = 0; g<windowGroups; g++) {
			for(sfb = 0; sfb<maxSFB;) {
				int end = sectEnd[idx];
				switch(sfbCB[idx]) {
					case ZERO_HCB:
						for(; sfb<end; sfb++, idx++) {
							scaleFactors[idx] = 0;
						}
						break;
					case INTENSITY_HCB:
					case INTENSITY_HCB2:
						for(; sfb<end; sfb++, idx++) {
							offset[2] += Huffman.decodeScaleFactor(_in)-SF_DELTA;
							tmp = Math.min(Math.max(offset[2], -155), 100);
							scaleFactors[idx] = SCALEFACTOR_TABLE[-tmp+SF_OFFSET];
						}
						break;
					case NOISE_HCB:
						for(; sfb<end; sfb++, idx++) {
							if(noiseFlag) {
								offset[1] += _in.readBits(9)-256;
								noiseFlag = false;
							}
							else offset[1] += Huffman.decodeScaleFactor(_in)-SF_DELTA;
							tmp = Math.min(Math.max(offset[1], -100), 155);
							scaleFactors[idx] = -SCALEFACTOR_TABLE[tmp+SF_OFFSET];
						}
						break;
					default:
						for(; sfb<end; sfb++, idx++) {
							offset[0] += Huffman.decodeScaleFactor(_in)-SF_DELTA;
							if(offset[0]>255) throw new AACException("scalefactor out of range: "+offset[0]);
							scaleFactors[idx] = SCALEFACTOR_TABLE[offset[0]-100+SF_OFFSET];
						}
						break;
				}
			}
		}
	}

	private void decodeSpectralData(IBitStream _in) throws AACException {
		Arrays.fill(data, 0);
		final int maxSFB = info.getMaxSFB();
		final int windowGroups = info.getWindowGroupCount();
		final int[] offsets = info.getSWBOffsets();
		final int[] buf = new int[4];

		int sfb, j, k, w, hcb, off, width, num;
		int groupOff = 0, idx = 0;
		for(int g = 0; g<windowGroups; g++) {
			int groupLen = info.getWindowGroupLength(g);

			for(sfb = 0; sfb<maxSFB; sfb++, idx++) {
				hcb = sfbCB[idx];
				off = groupOff+offsets[sfb];
				width = offsets[sfb+1]-offsets[sfb];
				if(hcb==ZERO_HCB||hcb==INTENSITY_HCB||hcb==INTENSITY_HCB2) {
					for(w = 0; w<groupLen; w++, off += 128) {
						Arrays.fill(data, off, off+width, 0);
					}
				}
				else if(hcb==NOISE_HCB) {
					//apply PNS: fill with random values
					for(w = 0; w<groupLen; w++, off += 128) {
						float energy = 0;

						for(k = 0; k<width; k++) {
							randomState *= 1664525+1013904223;
							data[off+k] = randomState;
							energy += data[off+k]*data[off+k];
						}

						float scale = (float) (scaleFactors[idx]/Math.sqrt(energy));
						for(k = 0; k<width; k++) {
							data[off+k] *= scale;
						}
					}
				}
				else {
					for(w = 0; w<groupLen; w++, off += 128) {
						num = (hcb>=FIRST_PAIR_HCB) ? 2 : 4;
						for(k = 0; k<width; k += num) {
							Huffman.decodeSpectralData(_in, hcb, buf, 0);

							//inverse quantization & scaling
							for(j = 0; j<num; j++) {
								data[off+k+j] = (buf[j]>0) ? IQ_TABLE[buf[j]] : -IQ_TABLE[-buf[j]];
								data[off+k+j] *= scaleFactors[idx];
							}
						}
					}
				}
			}
			groupOff += groupLen<<7;
		}
	}

	/* =========== gets ============ */
	/**
	 * Does inverse quantization and applies the scale factors on the decoded
	 * data. After this the noiseless decoding is finished and the decoded data
	 * is returned.
	 * @return the inverse quantized and scaled data
	 */
	public float[] getInvQuantData() throws AACException {
		return data;
	}

	public ICSInfo getInfo() {
		return info;
	}

	public int[] getSectEnd() {
		return sectEnd;
	}

	public int[] getSfbCB() {
		return sfbCB;
	}

	public float[] getScaleFactors() {
		return scaleFactors;
	}

	public boolean isTNSDataPresent() {
		return tnsDataPresent;
	}

	public TNS getTNS() {
		return tns;
	}

	public int getGlobalGain() {
		return globalGain;
	}

	public boolean isNoiseUsed() {
		return noiseUsed;
	}

	public int getLongestCodewordLength() {
		return longestCodewordLen;
	}

	public int getReorderedSpectralDataLength() {
		return reorderedSpectralDataLen;
	}

	public boolean isGainControlPresent() {
		return gainControlPresent;
	}

	public GainControl getGainControl() {
		return gainControl;
	}
}
