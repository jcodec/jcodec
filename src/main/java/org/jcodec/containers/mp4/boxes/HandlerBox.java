package org.jcodec.containers.mp4.boxes;

import static org.jcodec.common.JCodecUtil2.asciiString;

import org.jcodec.common.io.NIOUtils;

import js.nio.ByteBuffer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * A handler description box
 * 
 * @author The JCodec project
 * 
 */
public class HandlerBox extends FullBox {
    public HandlerBox(Header atom) {
        super(atom);
    }

    private String componentType;
    private String componentSubType;
    private String componentManufacturer;
    private int componentFlags;
    private int componentFlagsMask;
    private String componentName;

    public static String fourcc() {
        return "hdlr";
    }

    public static HandlerBox createHandlerBox(String componentType, String componentSubType,
            String componentManufacturer, int componentFlags, int componentFlagsMask) {
        HandlerBox hdlr = new HandlerBox(new Header(fourcc()));
        hdlr.componentType = componentType;
        hdlr.componentSubType = componentSubType;
        hdlr.componentManufacturer = componentManufacturer;
        hdlr.componentFlags = componentFlags;
        hdlr.componentFlagsMask = componentFlagsMask;
        hdlr.componentName = "";
        return hdlr;
    }

    public void parse(ByteBuffer input) {
        super.parse(input);

        componentType = NIOUtils.readString(input, 4);
        componentSubType = NIOUtils.readString(input, 4);
        componentManufacturer = NIOUtils.readString(input, 4);

        componentFlags = input.getInt();
        componentFlagsMask = input.getInt();
        componentName = NIOUtils.readString(input, input.remaining());
    }

    public void doWrite(ByteBuffer out) {
        super.doWrite(out);

        out.putArr(asciiString(componentType));
        out.putArr(asciiString(componentSubType));
        out.putArr(asciiString(componentManufacturer));

        out.putInt(componentFlags);
        out.putInt(componentFlagsMask);
        if (componentName != null) {
            out.putArr(asciiString(componentName));
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