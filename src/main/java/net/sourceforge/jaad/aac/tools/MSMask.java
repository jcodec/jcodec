package net.sourceforge.jaad.aac.tools;

import net.sourceforge.jaad.aac.AACException;

/**
 * This class is part of JAAD ( jaadec.sourceforge.net ) that is distributed
 * under the Public Domain license. Code changes provided by the JCodec project
 * are distributed under FreeBSD license.
 * 
 * The MSMask indicates, if MS is applied to a specific ICStream.
 * @author in-somnia
 */
public enum MSMask {

	TYPE_ALL_0(0),
	TYPE_USED(1),
	TYPE_ALL_1(2),
	TYPE_RESERVED(3);

	public static MSMask forInt(int i) throws AACException {
		MSMask m;
		switch(i) {
			case 0:
				m = TYPE_ALL_0;
				break;
			case 1:
				m = TYPE_USED;
				break;
			case 2:
				m = TYPE_ALL_1;
				break;
			case 3:
				m = TYPE_RESERVED;
				break;
			default:
				throw new AACException("unknown MS mask type");
		}
		return m;
	}
	private int num;

	private MSMask(int num) {
		this.num = num;
	}
}
