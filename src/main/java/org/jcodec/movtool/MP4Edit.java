package org.jcodec.movtool;

import org.jcodec.containers.mp4.boxes.MovieBox;
import org.jcodec.containers.mp4.boxes.MovieFragmentBox;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public interface MP4Edit {

    /**
     * Operation performed on a movie header and fragments
     * 
     * @param mov
     */
    void apply(MovieBox mov, MovieFragmentBox[] fragmentBox);

    /**
     * Operation performed on a movie header
     * 
     * @param mov
     */
    void apply(MovieBox mov);
}
