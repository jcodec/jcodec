package net.sourceforge.jaad.aac.transport;

import net.sourceforge.jaad.aac.AACException;
import net.sourceforge.jaad.aac.syntax.IBitStream;
import net.sourceforge.jaad.aac.syntax.PCE;

/**
 * This class is part of JAAD ( jaadec.sourceforge.net ) that is distributed
 * under the Public Domain license. Code changes provided by the JCodec project
 * are distributed under FreeBSD license.
 * 
 * @author in-somnia
 */
public final class ADIFHeader {

	private static final long ADIF_ID = 0x41444946; //'ADIF'
	private long id;
	private boolean copyrightIDPresent;
	private byte[] copyrightID;
	private boolean originalCopy, home, bitstreamType;
	private int bitrate;
	private int pceCount;
	private int[] adifBufferFullness;
	private PCE[] pces;

	public static boolean isPresent(IBitStream in) throws AACException {
		return in.peekBits(32)==ADIF_ID;
	}

	private ADIFHeader() {
		copyrightID = new byte[9];
	}

	public static ADIFHeader readHeader(IBitStream in) throws AACException {
		final ADIFHeader h = new ADIFHeader();
		h.decode(in);
		return h;
	}

	private void decode(IBitStream in) throws AACException {
		int i;
		id = in.readBits(32); //'ADIF'
		copyrightIDPresent = in.readBool();
		if(copyrightIDPresent) {
			for(i = 0; i<9; i++) {
				copyrightID[i] = (byte) in.readBits(8);
			}
		}
		originalCopy = in.readBool();
		home = in.readBool();
		bitstreamType = in.readBool();
		bitrate = in.readBits(23);
		pceCount = in.readBits(4)+1;
		pces = new PCE[pceCount];
		adifBufferFullness = new int[pceCount];
		for(i = 0; i<pceCount; i++) {
			if(bitstreamType) adifBufferFullness[i] = -1;
			else adifBufferFullness[i] = in.readBits(20);
			pces[i] = new PCE();
			pces[i].decode(in);
		}
	}

	public PCE getFirstPCE() {
		return pces[0];
	}
}
