package org.jcodec.movtool;
import java.lang.IllegalStateException;
import java.lang.System;


import org.jcodec.containers.mp4.MP4Util;
import org.jcodec.containers.mp4.MP4Util.Movie;
import org.jcodec.containers.mp4.boxes.MovieBox;

import java.io.File;
import java.io.IOException;

/**
 * A full fledged MP4 editor.
 * 
 * Parses MP4 file, applies the edit and saves the result in a new file.
 * 
 * Unlike InplaceMP4Edit any changes are allowed. This class will take care of
 * adjusting all the sample offsets so the result file will be correct.
 * 
 * @author The JCodec project
 * 
 */
public class ReplaceMP4Editor {

    public void modifyOrReplace(File src, MP4Edit edit) throws IOException {
        boolean modify = new InplaceMP4Editor().modify(src, edit);
        if (!modify)
            replace(src, edit);
    }

    public void replace(File src, MP4Edit edit) throws IOException {
        File tmp = new File(src.getParentFile(), "." + src.getName());
        copy(src, tmp, edit);
        tmp.renameTo(src);
    }

    public void copy(File src, File dst, MP4Edit edit) throws IOException {
        final Movie movie = MP4Util.createRefMovieFromFile(src);
        edit.apply(movie.getMoov());
        Flattern fl = new Flattern();

        fl.flattern(movie, dst);
    }
}