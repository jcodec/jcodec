package org.jcodec.samples.streaming;

import java.io.IOException;
import java.util.List;

import org.jcodec.common.model.Packet;
import org.jcodec.player.filters.MediaInfo;

/**
 * Random access interface to media resource. Abstracts out the specific
 * container details.
 * 
 * @author The JCodec project
 * 
 */
public interface Adapter {

    public interface AdapterTrack {
        /**
         * Returns metadata about this media track
         * 
         * @return
         * @throws IOException
         */
        MediaInfo getMediaInfo() throws IOException;

        /**
         * Finds the frame with the given pts.
         * 
         * The pts doesn't necessarily need to fall on a frame, it can be
         * somewhere betrween the adjacent frames
         * 
         * @param pts
         * @return Frame number of a frame with a given pts or -1 if none is
         *         found
         * @throws IOException
         */
        int search(long pts) throws IOException;
    }

    public interface VideoAdapterTrack extends AdapterTrack {
        /**
         * Gets gop by GOP id or gop alias. GOP id is a frame number of the
         * first ( I ) frame in the gop. GOP alias is the frame number of any
         * other frame belonging to this GOP.
         * 
         * In the case stream contains B frames this will always return a closed
         * GOP except when this is the last GOP in a file, in which case the
         * result will depend on whether the GOP is closed in the underlying
         * file
         * 
         * @param gopId
         * @return
         * @throws IOException
         */
        Packet[] getGOP(int gopId) throws IOException;

        /**
         * Returns GOP id by frame number
         * 
         * @return
         */
        int gopId(int frameNo);
    }

    public interface AudioAdapterTrack extends AdapterTrack {
        /**
         * Gets audio frame with a given number
         * 
         * @param frameId
         * @return
         * @throws IOException
         */
        Packet getFrame(int frameId) throws IOException;
    }

    AdapterTrack getTrack(int trackNo);

    /**
     * Get a list of ( interleaved ) media track making up a media resource
     * 
     * Typically consists of one video track and one or more audio tracks
     * 
     * @return
     */
    List<AdapterTrack> getTracks();

    /**
     * Closes this adapter and all associated resources
     */
    void close();
}
