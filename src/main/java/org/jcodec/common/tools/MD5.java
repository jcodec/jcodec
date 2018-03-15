package org.jcodec.common.tools;
import java.lang.StringBuilder;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * This class is part of JCodec ( www.jcodec.org )
 * This software is distributed under FreeBSD License
 * 
 * @author The JCodec project
 *
 */
public class MD5 {
    public static String md5sumBytes(byte[] bytes) {
        MessageDigest md5 = getDigest();
        md5.update(bytes);
        return digestToString(md5.digest());
    }

    private static String digestToString(byte[] digest) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < digest.length; i++) {
            byte item = digest[i];
            int b = item & 0xFF;
            if (b < 0x10)
                sb.append("0");
            sb.append(Integer.toHexString(b));
        }

        return sb.toString();
    }

    public static String md5sum(ByteBuffer bytes) {
        MessageDigest md5 = getDigest();
        md5.update(bytes);
        byte[] digest = md5.digest();
        return digestToString(digest);
    }

    public static MessageDigest getDigest() {
        MessageDigest md5;
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        return md5;

    }
}
