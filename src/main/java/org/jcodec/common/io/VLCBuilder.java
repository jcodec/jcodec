package org.jcodec.common.io;

import org.jcodec.common.IntArrayList;
import org.jcodec.common.IntIntMap;


/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * prefix VLC reader builder
 * 
 * @author The JCodec project
 * 
 */
public class VLCBuilder {

    private IntIntMap forward = new IntIntMap();
    private IntIntMap inverse = new IntIntMap();
    private IntArrayList codes = new IntArrayList();
    private IntArrayList codesSizes = new IntArrayList();

    public VLCBuilder() {
    }

    public VLCBuilder(int[] codes, int[] lens, int[] vals) {
        for (int i = 0; i < codes.length; i++) {
            set(codes[i], lens[i], vals[i]);
        }
    }

    public VLCBuilder set(int val, String code) {
        set(Integer.parseInt(code, 2), code.length(), val);
        
        return this;
    }

    public VLCBuilder set(int code, int len, int val) {
        codes.add(code << (32 - len));
        codesSizes.add(len);
        forward.put(val, codes.size() - 1);
        inverse.put(codes.size() - 1, val);

        return this;
    }

    public VLC getVLC() {
        return new VLC(codes.toArray(), codesSizes.toArray()) {
            public int readVLC(BitReader in) {
                return inverse.get(super.readVLC(in));
            }

            public void writeVLC(BitWriter out, int code) {
                super.writeVLC(out, forward.get(code));
            }
        };
    }
}