package org.jcodec.containers.mp4.boxes;

import java.io.DataOutput;
import java.io.IOException;
import java.io.InputStream;

import org.jcodec.common.io.ReaderBE;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Load setting atom
 * 
 * @author The JCodec project
 * 
 */
public class LoadSettingsBox extends Box {
    private int preloadStartTime;
    private int preloadDuration;
    private int preloadFlags;
    private int defaultHints;

    public static String fourcc() {
        return "load";
    }

    public LoadSettingsBox(Header header) {
        super(header);
    }

    public LoadSettingsBox() {
        super(new Header(fourcc()));
    }

    public void parse(InputStream input) throws IOException {
        preloadStartTime = (int) ReaderBE.readInt32(input);
        preloadDuration = (int) ReaderBE.readInt32(input);
        preloadFlags = (int) ReaderBE.readInt32(input);
        defaultHints = (int) ReaderBE.readInt32(input);
    }

    protected void doWrite(DataOutput out) throws IOException {
        out.writeInt(preloadStartTime);
        out.writeInt(preloadDuration);
        out.writeInt(preloadFlags);
        out.writeInt(defaultHints);
    }

    public int getPreloadStartTime() {
        return preloadStartTime;
    }

    public int getPreloadDuration() {
        return preloadDuration;
    }

    public int getPreloadFlags() {
        return preloadFlags;
    }

    public int getDefaultHints() {
        return defaultHints;
    }
}
