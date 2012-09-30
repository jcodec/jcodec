package org.jcodec.containers.mp4.boxes;

import java.io.DataOutput;
import java.io.IOException;
import java.io.InputStream;

import org.jcodec.common.io.ReaderBE;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * A handler description box
 * 
 * @author Jay Codec
 * 
 */
public class HandlerBox extends FullBox {
    private String componentType;
    private String componentSubType;
    private String componentManufacturer;
    private int componentFlags;
    private int componentFlagsMask;
    private String componentName;

    public static String fourcc() {
        return "hdlr";
    }

    public HandlerBox(String componentType, String componentSubType, String componentManufacturer, int componentFlags,
            int componentFlagsMask) {
        super(new Header("hdlr"));
        this.componentType = componentType;
        this.componentSubType = componentSubType;
        this.componentManufacturer = componentManufacturer;
        this.componentFlags = componentFlags;
        this.componentFlagsMask = componentFlagsMask;
        this.componentName = "";
    }

    public HandlerBox() {
        super(new Header(fourcc()));
    }

    public void parse(InputStream input) throws IOException {
        super.parse(input);

        componentType = readType(input);
        componentSubType = readType(input);
        componentManufacturer = readType(input);

        componentFlags = (int) ReaderBE.readInt32(input);
        componentFlagsMask = (int) ReaderBE.readInt32(input);
        componentName = ReaderBE.readPascalString(input);
    }

    private String readType(InputStream input) throws IOException {
        byte[] b = new byte[4];
        input.read(b);
        return new String(b);
    }

    public void doWrite(DataOutput out) throws IOException {
        super.doWrite(out);

        out.write(componentType.getBytes());
        out.write(componentSubType.getBytes());
        out.write(componentManufacturer.getBytes());

        out.writeInt(componentFlags);
        out.writeInt(componentFlagsMask);
        if (componentName != null) {
            out.write(componentName.length());
            out.write(componentName.getBytes());
        }
    }

    public String getComponentType() {
        return componentType;
    }

    public String getComponentSubType() {
        return componentSubType;
    }

    public String getComponentManufacturer() {
        return componentManufacturer;
    }

    public int getComponentFlags() {
        return componentFlags;
    }

    public int getComponentFlagsMask() {
        return componentFlagsMask;
    }
}