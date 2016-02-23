package org.jcodec.audio;

import org.jcodec.platform.Platform;

import java.lang.IllegalAccessException; import java.lang.StackTraceElement;

import java.nio.FloatBuffer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 *
 */
public class FilterSocket {
    private FloatBuffer[] buffers;
    private long[] positions;
    private int[] delays;
    private AudioFilter[] filters;
    private int totalInputs;
    private int totalOutputs;

    public FilterSocket(AudioFilter... filters) {
        totalInputs = 0;
        totalOutputs = 0;

        for (int i = 0; i < filters.length; i++) {
            totalInputs += filters[i].getNInputs();
            totalOutputs += filters[i].getNOutputs();
        }

        buffers = new FloatBuffer[totalInputs];
        positions = new long[totalInputs];
        delays = new int[totalInputs];
        for (int i = 0, b = 0; i < filters.length; i++) {
            for (int j = 0; j < filters[i].getNInputs(); j++, b++) {
                delays[b] = filters[i].getDelay();
            }
        }
        this.filters = filters;
    }

    public void allocateBuffers(int bufferSize) {
        for (int i = 0; i < totalInputs; i++) {
            buffers[i] = FloatBuffer.allocate(bufferSize + delays[i] * 2);
            buffers[i].position(delays[i]);
        }
    }

    FilterSocket(AudioFilter filter, FloatBuffer[] buffers, long[] positions) {
        this.filters = new AudioFilter[] { filter };
        this.buffers = buffers;
        this.positions = positions;
        this.delays = new int[] { filter.getDelay() };
        this.totalInputs = filter.getNInputs();
        this.totalOutputs = filter.getNOutputs();
    }

    public void filter(FloatBuffer[] outputs) {
        if (outputs.length != totalOutputs)
            throw new IllegalArgumentException("Can not output to provided filter socket inputs != outputs ("
                    + outputs.length + "!=" + totalOutputs + ")");
        for (int i = 0, ii = 0, oi = 0; i < filters.length; ii += filters[i].getNInputs(), oi += filters[i]
                .getNOutputs(), i++) {
            filters[i].filter(Platform.copyOfRangeO(buffers, ii, filters[i].getNInputs() + ii),
                    Platform.copyOfRangeL(positions, ii, filters[i].getNInputs() + ii),
                    Platform.copyOfRangeO(outputs, oi, filters[i].getNOutputs() + oi));
        }
    }

    FloatBuffer[] getBuffers() {
        return buffers;
    }

    public void rotate() {
        for (int i = 0; i < buffers.length; i++) {
            positions[i] += buffers[i].position();
            Audio.rotate(buffers[i]);
        }
    }

    public void setBuffers(FloatBuffer[] ins, long[] pos) {
        if (ins.length != totalInputs)
            throw new IllegalArgumentException(
                    "Number of input buffers provided is less then the number of filter inputs.");
        if (pos.length != totalInputs)
            throw new IllegalArgumentException(
                    "Number of input buffer positions provided is less then the number of filter inputs.");
        buffers = ins;
        positions = pos;
    }

    public int getTotalInputs() {
        return totalInputs;
    }

    public int getTotalOutputs() {
        return totalOutputs;
    }

    AudioFilter[] getFilters() {
        return filters;
    }

    public long[] getPositions() {
        return positions;
    }
}