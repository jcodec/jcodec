package org.jcodec.audio;
import org.jcodec.platform.Platform;

import java.lang.IllegalArgumentException;
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

    public static FilterSocket createFilterSocket(AudioFilter... arguments) {
        FilterSocket fs = new FilterSocket();
        fs.totalInputs = 0;
        fs.totalOutputs = 0;

        for (int i = 0; i < arguments.length; i++) {
            fs.totalInputs += arguments[i].getNInputs();
            fs.totalOutputs += arguments[i].getNOutputs();
        }

        fs.buffers = new FloatBuffer[fs.totalInputs];
        fs.positions = new long[fs.totalInputs];
        fs.delays = new int[fs.totalInputs];
        for (int i = 0, b = 0; i < arguments.length; i++) {
            for (int j = 0; j < arguments[i].getNInputs(); j++, b++) {
                fs.delays[b] = arguments[i].getDelay();
            }
        }
        fs.filters = arguments;
        return fs;
    }

    public void allocateBuffers(int bufferSize) {
        for (int i = 0; i < totalInputs; i++) {
            buffers[i] = FloatBuffer.allocate(bufferSize + delays[i] * 2);
            buffers[i].position(delays[i]);
        }
    }
    
    public static FilterSocket createFilterSocket2(AudioFilter filter, FloatBuffer[] buffers, long[] positions) {
        FilterSocket fs = new FilterSocket();
        fs.filters = new AudioFilter[] { filter };
        fs.buffers = buffers;
        fs.positions = positions;
        fs.delays = new int[] { filter.getDelay() };
        fs.totalInputs = filter.getNInputs();
        fs.totalOutputs = filter.getNOutputs();
        return fs;
    }
    
    private FilterSocket() {
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