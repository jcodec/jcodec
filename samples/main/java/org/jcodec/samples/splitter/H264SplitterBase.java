package org.jcodec.samples.splitter;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public abstract class H264SplitterBase {
	private static final int NU_IDR = 5;
	private static final int NU_NON_IDR = 1;
	private static final int NU_PPS = 8;
	private static final int NU_SPS = 7;
	private OutputStream os;
	private int nIdr;
	private byte[] sps;
	private byte[] pps;
	private byte[] oldSps;
	private byte[] oldPps;
	private int[] next;
	private int sliceCount;
	private boolean prevIdr;
	static byte[] marker = new byte[4];

	static {
		marker[0] = marker[1] = marker[2] = 0;
		marker[3] = 1;
	}

	protected void split(InputStream io) throws IOException {
		while (doNALUnit(io))
			;
		finishCurrentSlice();
	}

	private boolean doNALUnit(InputStream io) throws IOException {
		if (!readMarker(io))
			return false;

		int nalUnit = readRBSPByte(io);
		int nalUnitType = nalUnit & 0x1F;

		if (nalUnitType == NU_IDR) {
			if (sps != null && pps != null) {
				if (!prevIdr) {
					if (nIdr >= getMaxIdr()) {
						breakHere();
					}
					nIdr++;
				}
				prevIdr = true;
				writeMarker();
				os.write(nalUnit);
				copyRBSP(io, os);
			} else {
				skipRBSP(io);
			}
		} else {
			if (nalUnitType == NU_SPS) {
				oldSps = sps;
				sps = readRBSP(io);
			} else if (nalUnitType == NU_PPS) {
				oldPps = pps;
				pps = readRBSP(io);
				if (newSPSPPS()) {
					breakHere();
				}
			} else if (nalUnitType == NU_NON_IDR) {
				if (sps != null && pps != null) {
					writeMarker();
					os.write(nalUnit);
					copyRBSP(io, os);
				} else {
					skipRBSP(io);
				}
			} else {
				skipRBSP(io);
			}
			prevIdr = false;
		}

		return true;
	}

	private boolean newSPSPPS() {
		return !Arrays.equals(this.oldSps, sps)
				|| !Arrays.equals(this.oldPps, pps);
	}

	private int readRBSPByte(InputStream io) throws IOException {
		if (next[0] == -1)
			throw new EOFException();
		int ret = next[0];
		next[0] = next[1];
		next[1] = next[2];
		next[2] = io.read();

		return ret;
	}

	private boolean moreRBSPData() {
		return next[0] != -1
				&& !(next[0] == 0 && next[1] == 0 && (next[2] == 1 || next[2] == 0));
	}

	private void copyRBSP(InputStream io, OutputStream os) throws IOException {
		while (moreRBSPData()) {
			os.write(readRBSPByte(io));
		}
	}

	private void skipRBSP(InputStream io) throws IOException {
		while (moreRBSPData())
			readRBSPByte(io);
	}

	private byte[] readRBSP(InputStream io) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		copyRBSP(io, baos);
		return baos.toByteArray();
	}

	private void read3B(int[] buf, InputStream io) throws IOException {
		buf[0] = io.read();
		buf[1] = io.read();
		buf[2] = io.read();
	}

	private boolean readMarker(InputStream io) throws IOException {
		if (next == null) {
			next = new int[3];
			read3B(next, io);
		}
		if (next[0] == -1 || next[1] == -1 || next[2] == -1)
			return false;

		if ((next[0] | next[1] | next[2]) == 0) {
			int fourth = io.read();
			if (fourth == -1)
				return false;
		}

		read3B(next, io);
		return true;
	}

	private void writeMarker() throws IOException {
		os.write(marker);
	}

	private void breakHere() throws IOException {
		finishCurrentSlice();
		startNewSlice();

		++sliceCount;
		writeMarker();
		os.write((2 << 5) | NU_SPS);
		os.write(sps);
		writeMarker();
		os.write((2 << 5) | NU_PPS);
		os.write(pps);
		nIdr = 0;
	}

	protected void setOutputStream(OutputStream os) {
		this.os = os;
	}

	protected OutputStream getOutputStream() {
		return os;
	}

	protected int getSliceCount() {
		return sliceCount;
	}

	protected abstract void finishCurrentSlice() throws IOException;

	protected abstract void startNewSlice() throws IOException;

	protected abstract int getMaxIdr();
}
