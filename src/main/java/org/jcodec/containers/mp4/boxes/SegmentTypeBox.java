package org.jcodec.containers.mp4.boxes;

import org.jcodec.common.JCodecUtil2;
import org.jcodec.common.io.NIOUtils;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.LinkedList;

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
        this.compBrands = new LinkedList<String>();
    }

    private String majorBrand;
    private int minorVersion;
    private Collection<String> compBrands;
    
    public static SegmentTypeBox createSegmentTypeBox(String majorBrand, int minorVersion, Collection<String> compBrands) {
        SegmentTypeBox styp = new SegmentTypeBox(new Header(fourcc()));
        styp.majorBrand = majorBrand;
        styp.minorVersion = minorVersion;
        styp.compBrands = compBrands;
        return styp;
    }

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
        out.put(JCodecUtil2.asciiString(majorBrand));
        out.putInt(minorVersion);

        for (String string : compBrands) {
            out.put(JCodecUtil2.asciiString(string));
        }
    }

    @Override
    public int estimateSize() {
        int sz = 13;

        for (String string : compBrands) {
            sz += JCodecUtil2.asciiString(string).length;
        }
        return sz;
    }
}