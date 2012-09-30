package org.jcodec.common.io;

import java.io.IOException;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntIntHashMap;

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

    private TIntIntHashMap forward = new TIntIntHashMap();
    private TIntIntHashMap inverse = new TIntIntHashMap();
    private TIntArrayList codes = new TIntArrayList();
    private TIntArrayList codesSizes = new TIntArrayList();

    public VLCBuilder() {
    }

    public VLCBuilder(int[] codes, int[] lens, int[] vals) {
        for (int i = 0; i < codes.length; i++) {
            set(codes[i], lens[i], vals[i]);
        }
    }

    public void set(int val, String code) {
        set(Integer.parseInt(code, 2), code.length(), val);
    }

    public void set(int code, int len, int val) {
        codes.add(code << (32 - len));
        codesSizes.add(len);
        forward.put(val, codes.size() - 1);
        inverse.put(codes.size() - 1, val);

    }

    public VLC getVLC() {
        return new VLC(codes.toArray(), codesSizes.toArray()) {
            public int readVLC(InBits in) throws IOException {
                return inverse.get(super.readVLC(in));
            }

            public void writeVLC(OutBits out, int code) throws IOException {
                super.writeVLC(out, forward.get(code));
            }
        };
    }
}