package org.jcodec.codecs.util;
import java.io.ByteArrayOutputStream;

public class BinUtil {

    /**
     * Converts a string in the form "[01]*" into bits and packs them into byte
     * array
     * 
     * @param str
     * @return
     */
    public static byte[] binaryStringToBytes(String str) {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int curByte = 0;

        int i, bc = 0;
        for (i = 0; i < str.length(); i++) {
            int bit;
            char charAt = str.charAt(i);
            if (charAt == '1')
                bit = 1;
            else if (charAt == '0')
                bit = 0;
            else
                continue;

            curByte |= bit << (7 - bc % 8);
            if (bc % 8 == 7) {
                baos.write(curByte);
                curByte = 0;
            }
            bc++;
        }
        if (bc % 8 != 0)
            baos.write(curByte);

        return baos.toByteArray();
    }

}
