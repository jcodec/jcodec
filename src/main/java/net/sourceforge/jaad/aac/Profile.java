package net.sourceforge.jaad.aac;

/**
 * This class is part of JAAD ( jaadec.sourceforge.net ) that is distributed
 * under the Public Domain license. Code changes provided by the JCodec project
 * are distributed under FreeBSD license.
 * 
 * Different AAC profiles.
 * The function <code>Decoder.canDecode</code> specifies if the decoder can
 * handle a given format.
 * More precisely, the ISO standard calls these 'object types'.
 * @author in-somnia
 */
public final class Profile {

public final static Profile UNKNOWN = new Profile(-1, "unknown", false);
public final static Profile AAC_MAIN = new Profile(1, "AAC Main Profile", true);
public final static Profile AAC_LC = new Profile(2, "AAC Low Complexity", true);
public final static Profile AAC_SSR = new Profile(3, "AAC Scalable Sample Rate", false);
public final static Profile AAC_LTP = new Profile(4, "AAC Long Term Prediction", false);
public final static Profile AAC_SBR = new Profile(5, "AAC SBR", true);
public final static Profile AAC_SCALABLE = new Profile(6, "Scalable AAC", false);
public final static Profile TWIN_VQ = new Profile(7, "TwinVQ", false);
public final static Profile AAC_LD = new Profile(11, "AAC Low Delay", false);
public final static Profile ER_AAC_LC = new Profile(17, "Error Resilient AAC Low Complexity", true);
public final static Profile ER_AAC_SSR = new Profile(18, "Error Resilient AAC SSR", false);
public final static Profile ER_AAC_LTP = new Profile(19, "Error Resilient AAC Long Term Prediction", false);
public final static Profile ER_AAC_SCALABLE = new Profile(20, "Error Resilient Scalable AAC", false);
public final static Profile ER_TWIN_VQ = new Profile(21, "Error Resilient TwinVQ", false);
public final static Profile ER_BSAC = new Profile(22, "Error Resilient BSAC", false);
public final static Profile ER_AAC_LD = new Profile(23, "Error Resilient AAC Low Delay", false);
    
	private static final Profile[] ALL = {
		AAC_MAIN, AAC_LC, AAC_SSR, AAC_LTP, AAC_SBR, AAC_SCALABLE, TWIN_VQ,
		null, null, null, AAC_LD, null, null, null, null, null, ER_AAC_LC, ER_AAC_SSR,
		ER_AAC_LTP, ER_AAC_SCALABLE, ER_TWIN_VQ, ER_BSAC, ER_AAC_LD
	};

	/**
	 * Returns a profile instance for the given index. If the index is not
	 * between 1 and 23 inclusive, UNKNOWN is returned.
	 * @return a profile with the given index
	 */
	public static Profile forInt(int i) {
		Profile p;
		if(i<=0||i>ALL.length) p = UNKNOWN;
		else p = ALL[i-1];
		return p;
	}
	private final int num;
	private final String descr;
	private final boolean supported;

	private Profile(int num, String descr, boolean supported) {
		this.num = num;
		this.descr = descr;
		this.supported = supported;
	}

	/**
	 * Returns this profile's index between 1 and 23 or -1 if this is the
	 * <code>UNKNOWN</code> instance.
	 * @return the profile's index
	 */
	public int getIndex() {
		return num;
	}

	/**
	 * Returns a short description of this profile.
	 * @return the profile's description
	 */
	public String getDescription() {
		return descr;
	}

	/**
	 * Returns a string representation of this profile. The method is
	 * identical to <code>getDescription()</code>.
	 * @return the profile's description
	 */
	@Override
	public String toString() {
		return descr;
	}

	/**
	 * Returns a boolean, indicating if this profile can be decoded by the
	 * <code>Decoder</code>.
	 * @see Decoder#canDecode(net.sourceforge.jaad.aac.Profile) 
	 * @return true if the profile is supported
	 */
	public boolean isDecodingSupported() {
		return supported;
	}

	/**
	 * Returns a boolean, indicating if this profile contains error resilient
	 * tools. That is, if it's index is higher than 16, since the first error
	 * resilient profile is ER_AAC_LC (17).
	 * This method is mainly used internally.
	 * @return true if the profile uses error resilience
	 */
	public boolean isErrorResilientProfile() {
		return num>16;
	}
}
