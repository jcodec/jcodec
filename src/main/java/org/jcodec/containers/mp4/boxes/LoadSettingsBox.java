package org.jcodec.containers.mp4.boxes;

import js.nio.ByteBuffer;

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

    public void parse(ByteBuffer input) {
        preloadStartTime = input.getInt();
        preloadDuration = input.getInt();
        preloadFlags = input.getInt();
        defaultHints = input.getInt();
    }

    protected void doWrite(ByteBuffer out) {
        out.putInt(preloadStartTime);
        out.putInt(preloadDuration);
        out.putInt(preloadFlags);
        out.putInt(defaultHints);
    }
    
    @Override
    public int estimateSize() {
        return 24;
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