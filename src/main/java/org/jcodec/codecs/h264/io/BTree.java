package org.jcodec.codecs.h264.io;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Simple BTree implementation needed for haffman tables
 * 
 * @author Jay Codec
 * 
 */
public class BTree {
    private BTree zero;
    private BTree one;
    private Object value;

    /**
     * Adds a leaf value to a binary path specified by path
     * 
     * @param str
     * @param value
     */
    public void addString(String path, Object value) {
        if (path.length() == 0) {
            this.value = value;
            return;
        }
        char charAt = path.charAt(0);
        BTree branch;
        if (charAt == '0') {
            if (zero == null)
                zero = new BTree();
            branch = zero;
        } else {
            if (one == null)
                one = new BTree();
            branch = one;
        }
        branch.addString(path.substring(1), value);
    }

    public BTree down(int b) {
        if (b == 0)
            return zero;
        else
            return one;
    }

    public Object getValue() {
        return value;
    }
}