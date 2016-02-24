package org.jcodec.containers.mp4.boxes;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.LinkedList;

import org.jcodec.common.JCodecUtil;
import org.jcodec.common.io.NIOUtils;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * File type box
 * 
 * 
 * @author The JCodec project
 * 
 */
public class SegmentTypeBox extends Box {
    public SegmentTypeBox(Header header) {
        super(header);
    }

    private String majorBrand;
    private int minorVersion;
    private Collection<String> compBrands;

    public static String fourcc() {
        return "styp";
    }

    public void parse(ByteBuffer input) {
        majorBrand = NIOUtils.readString(input, 4);
        minorVersion = input.getInt();

        String brand;
        while (input.hasRemaining() && (brand = NIOUtils.readString(input, 4)) != null) {
            compBrands.add(brand);
        }
    }

    public String getMajorBrand() {
        return majorBrand;
    }

    public Collection<String> getCompBrands() {
        return compBrands;
    }

    public void doWrite(ByteBuffer out) {
        out.put(JCodecUtil.asciiString(majorBrand));
        out.putInt(minorVersion);

        for (String string : compBrands) {
            out.put(JCodecUtil.asciiString(string));
        }
    }
}