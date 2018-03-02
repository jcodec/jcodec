package org.jcodec.containers.dpx;

import java.util.Date;

public class ImageSourceHeader {
    public int xOffset;
    public int yOffset;
    public float xCenter;
    public float yCenter;
    public int xOriginal;
    public int yOriginal;
    public String sourceImageFilename;
    public Date sourceImageDate;
    public String deviceName;
    public String deviceSerial;
    public short[] borderValidity;
    public int[] aspectRatio;
}
