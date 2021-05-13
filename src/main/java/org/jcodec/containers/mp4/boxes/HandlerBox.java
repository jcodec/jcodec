package org.jcodec.containers.mp4.boxes;

import static org.jcodec.common.JCodecUtil2.asciiString;

import org.jcodec.common.io.NIOUtils;
import org.jcodec.containers.mp4.boxes.Box.AtomField;

import java.nio.ByteBuffer;

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

        out.put(fourcc(componentType));
        out.put(fourcc(componentSubType));
        out.put(fourcc(componentManufacturer));

        out.putInt(componentFlags);
        out.putInt(componentFlagsMask);
        if (componentName != null) {
            out.put(fourcc(componentName));
        }
    }
    
    public byte[] fourcc(String fourcc) {
        byte[] dst = new byte[4];
        if (fourcc != null) {
            byte[] tmp = asciiString(fourcc);
            for (int i = 0; i < Math.min(tmp.length, 4); i++)
                dst[i] = tmp[i];
        }
        return dst;
    }
    
    @Override
    public int estimateSize() {
        return 32 + (componentName != null ? 4 : 0);
    }

    @AtomField(idx=0)
    public String getComponentType() {
        return componentType;
    }

    @AtomField(idx=1)
    public String getComponentSubType() {
        return componentSubType;
    }

    @AtomField(idx=2)
    public String getComponentManufacturer() {
        return componentManufacturer;
    }

    @AtomField(idx=3)
    public int getComponentFlags() {
        return componentFlags;
    }

    @AtomField(idx=4)
    public int getComponentFlagsMask() {
        return componentFlagsMask;
    }
}