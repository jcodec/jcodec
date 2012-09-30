package org.jcodec.containers.mp4;

import java.util.Arrays;

import org.jcodec.containers.mp4.boxes.FileTypeBox;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 *
 */
public enum Brand {
    MOV("qt  ", 0x00000200, new String[] { "qt  " }), MP4("isom", 0x00000200, new String[] { "isom", "iso2", "avc1",
            "mp41" });

    private FileTypeBox ftyp;

    private Brand(String majorBrand, int version, String[] compatible) {
        ftyp = new FileTypeBox(majorBrand, version, Arrays.asList(compatible));
    }

    public FileTypeBox getFileTypeBox() {
        return ftyp;
    }
}
