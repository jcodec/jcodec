package org.jcodec.containers.mp4.boxes;

import java.io.DataOutput;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.LinkedList;

import org.jcodec.common.io.ReaderBE;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * File type box
 * 
 * 
 * @author Jay Codec
 * 
 */
public class FileTypeBox extends Box {
    private String majorBrand;
    private int minorVersion;
    private Collection<String> compBrands = new LinkedList<String>();

    public static String fourcc() {
        return "ftyp";
    }

    public FileTypeBox(String majorBrand, int minorVersion, Collection<String> compBrands) {
        super(new Header(fourcc()));
        this.majorBrand = majorBrand;
        this.minorVersion = minorVersion;
        this.compBrands = compBrands;
    }

    public FileTypeBox() {
        super(new Header(fourcc()));
    }

    public void parse(InputStream input) throws IOException {
        majorBrand = ReaderBE.readString(input, 4);
        minorVersion = (int) ReaderBE.readInt32(input);

        String brand;
        while ((brand = ReaderBE.readString(input, 4)) != null) {
            compBrands.add(brand);
        }
    }

    public String getMajorBrand() {
        return majorBrand;
    }

    public Collection<String> getCompBrands() {
        return compBrands;
    }

    public void doWrite(DataOutput out) throws IOException {
        out.write(majorBrand.getBytes());
        out.writeInt(minorVersion);

        for (String string : compBrands) {
            out.write(string.getBytes());
        }
    }
}