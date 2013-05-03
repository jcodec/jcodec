package org.jcodec.containers.mkv.elements;

import java.util.ArrayList;
import java.util.List;

import org.jcodec.containers.mkv.Type;
import org.jcodec.containers.mkv.ebml.MasterElement;

public class CuePoint extends MasterElement {

    public long cueTime;
    public List<CueTrackPosition> positions = new ArrayList<CueTrackPosition>();

    public CuePoint(long cueTime){
        super(Type.CuePoint.id);
        this.cueTime = cueTime;
    }
    
    public CuePoint(byte[] type) {
        super(type);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("CueTime: ").append(cueTime);
        for (CueTrackPosition position : positions)
            sb.append(position.toString());
        return sb.toString();
    }

}
