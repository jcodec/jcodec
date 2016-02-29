package net.sourceforge.jaad.aac;

import static net.sourceforge.jaad.aac.Profile.*;

import net.sourceforge.jaad.aac.syntax.BitStream;
import net.sourceforge.jaad.aac.syntax.SyntaxConstants;
import net.sourceforge.jaad.aac.syntax.IBitStream;
import net.sourceforge.jaad.aac.syntax.PCE;

/**
 * This class is part of JAAD ( jaadec.sourceforge.net ) that is distributed
 * under the Public Domain license. Code changes provided by the JCodec project
 * are distributed under FreeBSD license.
 * 
 * DecoderConfig that must be passed to the
 * <code>Decoder</code> constructor. Typically it is created via one of the
 * static parsing methods.
 *
 * @author in-somnia
 */
public class DecoderConfig implements SyntaxConstants {

	private Profile profile, extProfile;
	private SampleFrequency sampleFrequency;
	private ChannelConfiguration channelConfiguration;
	private boolean frameLengthFlag;
	private boolean dependsOnCoreCoder;
	private int coreCoderDelay;
	private boolean extensionFlag;
	//extension: SBR
	private boolean sbrPresent, downSampledSBR, sbrEnabled;
	//extension: error resilience
	private boolean sectionDataResilience, scalefactorResilience, spectralDataResilience;

	private DecoderConfig() {
		profile = Profile.AAC_MAIN;
		extProfile = Profile.UNKNOWN;
		sampleFrequency = SampleFrequency.SAMPLE_FREQUENCY_NONE;
		channelConfiguration = ChannelConfiguration.CHANNEL_CONFIG_UNSUPPORTED;
		frameLengthFlag = false;
		sbrPresent = false;
		downSampledSBR = false;
		sbrEnabled = true;
		sectionDataResilience = false;
		scalefactorResilience = false;
		spectralDataResilience = false;
	}

	/* ========== gets/sets ========== */
	public ChannelConfiguration getChannelConfiguration() {
		return channelConfiguration;
	}

	public void setChannelConfiguration(ChannelConfiguration channelConfiguration) {
		this.channelConfiguration = channelConfiguration;
	}

	public int getCoreCoderDelay() {
		return coreCoderDelay;
	}

	public void setCoreCoderDelay(int coreCoderDelay) {
		this.coreCoderDelay = coreCoderDelay;
	}

	public boolean isDependsOnCoreCoder() {
		return dependsOnCoreCoder;
	}

	public void setDependsOnCoreCoder(boolean dependsOnCoreCoder) {
		this.dependsOnCoreCoder = dependsOnCoreCoder;
	}

	public Profile getExtObjectType() {
		return extProfile;
	}

	public void setExtObjectType(Profile extObjectType) {
		this.extProfile = extObjectType;
	}

	public int getFrameLength() {
		return frameLengthFlag ? WINDOW_SMALL_LEN_LONG : WINDOW_LEN_LONG;
	}

	public boolean isSmallFrameUsed() {
		return frameLengthFlag;
	}

	public void setSmallFrameUsed(boolean shortFrame) {
		this.frameLengthFlag = shortFrame;
	}

	public Profile getProfile() {
		return profile;
	}

	public void setProfile(Profile profile) {
		this.profile = profile;
	}

	public SampleFrequency getSampleFrequency() {
		return sampleFrequency;
	}

	public void setSampleFrequency(SampleFrequency sampleFrequency) {
		this.sampleFrequency = sampleFrequency;
	}

	//=========== SBR =============
	public boolean isSBRPresent() {
		return sbrPresent;
	}

	public boolean isSBRDownSampled() {
		return downSampledSBR;
	}

	public boolean isSBREnabled() {
		return sbrEnabled;
	}

	public void setSBREnabled(boolean enabled) {
		sbrEnabled = enabled;
	}

	//=========== ER =============
	public boolean isScalefactorResilienceUsed() {
		return scalefactorResilience;
	}

	public boolean isSectionDataResilienceUsed() {
		return sectionDataResilience;
	}

	public boolean isSpectralDataResilienceUsed() {
		return spectralDataResilience;
	}

	/* ======== static builder ========= */
	/**
	 * Parses the input arrays as a DecoderSpecificInfo, as used in MP4
	 * containers.
	 * 
	 * @return a DecoderConfig
	 */
	public static DecoderConfig parseMP4DecoderSpecificInfo(byte[] data) throws AACException {
		final IBitStream _in = BitStream.createBitStream(data);
		final DecoderConfig config = new DecoderConfig();

		try {
			config.profile = readProfile(_in);

			int sf = _in.readBits(4);
			if(sf==0xF) config.sampleFrequency = SampleFrequency.forFrequency(_in.readBits(24));
			else config.sampleFrequency = SampleFrequency.forInt(sf);
			config.channelConfiguration = ChannelConfiguration.forInt(_in.readBits(4));

			Profile cp = config.profile;
			if (AAC_SBR == cp) {
                config.extProfile = cp;
                config.sbrPresent = true;
                sf = _in.readBits(4);
                //TODO: 24 bits already read; read again?
                //if(sf==0xF) config.sampleFrequency = SampleFrequency.forFrequency(_in.readBits(24));
                //if sample frequencies are the same: downsample SBR
                config.downSampledSBR = config.sampleFrequency.getIndex()==sf;
                config.sampleFrequency = SampleFrequency.forInt(sf);
                config.profile = readProfile(_in);
			} else if (AAC_MAIN == cp || AAC_LC ==  cp || AAC_SSR == cp || AAC_LTP == cp || ER_AAC_LC == cp || ER_AAC_LTP == cp || ER_AAC_LD == cp ) {
                //ga-specific info:
                config.frameLengthFlag = _in.readBool();
                if(config.frameLengthFlag) throw new AACException("config uses 960-sample frames, not yet supported"); //TODO: are 960-frames working yet?
                config.dependsOnCoreCoder = _in.readBool();
                if(config.dependsOnCoreCoder) config.coreCoderDelay = _in.readBits(14);
                else config.coreCoderDelay = 0;
                config.extensionFlag = _in.readBool();

                if(config.extensionFlag) {
                    if(cp.isErrorResilientProfile()) {
                        config.sectionDataResilience = _in.readBool();
                        config.scalefactorResilience = _in.readBool();
                        config.spectralDataResilience = _in.readBool();
                    }
                    //extensionFlag3
                    _in.skipBit();
                }

                if(config.channelConfiguration==ChannelConfiguration.CHANNEL_CONFIG_NONE) {
                    //TODO: is this working correct? -> ISO 14496-3 part 1: 1.A.4.3
                    _in.skipBits(3); //PCE
                    PCE pce = new PCE();
                    pce.decode(_in);
                    config.profile = pce.getProfile();
                    config.sampleFrequency = pce.getSampleFrequency();
                    config.channelConfiguration = ChannelConfiguration.forInt(pce.getChannelCount());
                }

                if(_in.getBitsLeft()>10) readSyncExtension(_in, config);
			} else {
                throw new AACException("profile not supported: "+cp.getIndex());
			}
			return config;
		}
		finally {
			_in.destroy();
		}
	}

	private static Profile readProfile(IBitStream _in) throws AACException {
		int i = _in.readBits(5);
		if(i==31) i = 32+_in.readBits(6);
		return Profile.forInt(i);
	}

	private static void readSyncExtension(IBitStream _in, DecoderConfig config) throws AACException {
		final int type = _in.readBits(11);
		switch(type) {
			case 0x2B7:
				final Profile profile = Profile.forInt(_in.readBits(5));

				if(profile.equals(Profile.AAC_SBR)) {
					config.sbrPresent = _in.readBool();
					if(config.sbrPresent) {
						config.profile = profile;

						int tmp = _in.readBits(4);

						if(tmp==config.sampleFrequency.getIndex()) config.downSampledSBR = true;
						if(tmp==15) {
							throw new AACException("sample rate specified explicitly, not supported yet!");
							//tmp = _in.readBits(24);
						}
					}
				}
				break;
		}
	}
}
