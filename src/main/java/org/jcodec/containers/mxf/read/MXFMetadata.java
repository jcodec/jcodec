package org.jcodec.containers.mxf.read;

import java.nio.ByteBuffer;

public abstract class MXFMetadata {
    private UL ul;
    protected UL uid;
    
    public MXFMetadata(UL ul) {
        this.ul = ul;
    }

    public abstract void read(ByteBuffer bb);

    /**
     * Utility method to read a collection of UMIDs
     * 
     * @param _bb
     * @return
     */
    protected static UL[] readULs(ByteBuffer _bb) {
        int count = _bb.getInt();
        // useless size of objects, always 16 according to specs
        _bb.getInt();
        UL[] result = new UL[count];
        for (int i = 0; i < count; i++) {
            result[i] = UL.read(_bb);
        }
        return result;
    }

    public UL getUl() {
        return ul;
    }

    public UL getUid() {
        return uid;
    }
}
