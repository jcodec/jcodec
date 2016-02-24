package org.jcodec.audio;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jcodec.audio.Audio.DummyFilter;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Audio filter graph
 * 
 * Represents a combination of filters as on 'uber' filter
 * 
 * @author The JCodec project
 * 
 */
public class FilterGraph implements AudioFilter {

    public static Factory addLevel(AudioFilter first) {
        return new Factory(first);
    }

    public static class Factory {
        private List<FilterSocket> sockets;

        protected Factory(AudioFilter firstFilter) {
            this.sockets = new ArrayList<FilterSocket>();

            if (firstFilter.getDelay() != 0) {
                // Removing first filter delay using filter socket zero stuffing
                // features
                sockets.add(new FilterSocket(new DummyFilter(firstFilter.getNInputs())));
                addLevel(firstFilter);
            } else
                sockets.add(new FilterSocket(firstFilter));
        }

//@formatter:off
        /**
         * Adds filters to the next level in the graph
         * 
         * The filters are added to from the left to the right, i.e.
         * <pre>
         *           L0
         *     L1 L1 L1 L1 L1 L1
         *     L2 L2 -->
         * </pre>     
         * As a consequence if the filters in this level contain less inputs then
         * there are outputs in a previous level the graph will throw exception
         * because the configuration it's misconfigured.
         * 
         * @param filters
         * @return
         */
//@formatter:on
        public Factory addLevel(AudioFilter... arguments) {
            FilterSocket socket = new FilterSocket(arguments);
            socket.allocateBuffers(4096);
            sockets.add(socket);
            return this;
        }

        /**
         * Adds n of this filter as the next level in a graph
         * 
         * @param filter
         * @param n
         * @return
         */
        public Factory addLevels(AudioFilter filter, int n) {
            AudioFilter[] filters = new AudioFilter[n];
            Arrays.fill(filters, filter);
            return addLevel(filters);
        }

        /**
         * Adds a level to the graph and tries to fill it with as many of the
         * current filter as will be needed to take all the outputs of the
         * previous level
         * 
         * @param filter
         * @return
         */
        public Factory addLevelSpan(AudioFilter filter) {
            int prevLevelOuts = sockets.get(sockets.size() - 1).getTotalOutputs();
            if ((prevLevelOuts % filter.getNInputs()) != 0)
                throw new IllegalArgumentException("Can't fill " + prevLevelOuts + " with multiple of "
                        + filter.getNInputs());

            return addLevels(filter, prevLevelOuts / filter.getNInputs());
        }

        public FilterGraph create() {
            return new FilterGraph(sockets.toArray(new FilterSocket[0]));
        }
    }

    private FilterSocket[] sockets;

    private FilterGraph(FilterSocket[] sockets) {
        this.sockets = sockets;
    }

    @Override
    public void filter(FloatBuffer[] ins, long[] pos, FloatBuffer[] outs) {
        sockets[0].setBuffers(ins, pos);
        for (int i = 0; i < sockets.length; i++) {
            FloatBuffer[] curOut = i < sockets.length - 1 ? sockets[i + 1].getBuffers() : outs;

            sockets[i].filter(curOut);

            if (i > 0) {
                sockets[i].rotate();
            }

            if (i < sockets.length - 1) {
                for (FloatBuffer b : curOut)
                    b.flip();
            }
        }
    }

    @Override
    public int getDelay() {
        return sockets[0].getFilters()[0].getDelay();
    }

    @Override
    public int getNInputs() {
        return sockets[0].getTotalInputs();
    }

    @Override
    public int getNOutputs() {
        return sockets[sockets.length - 1].getTotalOutputs();
    }
}
