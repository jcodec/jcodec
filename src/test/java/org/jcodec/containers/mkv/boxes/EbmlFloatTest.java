package org.jcodec.containers.mkv.boxes;

import static org.jcodec.containers.mkv.MKVType.Duration;
import static org.jcodec.containers.mkv.MKVType.createByType;
import junit.framework.Assert;

import org.jcodec.containers.mkv.boxes.EbmlFloat;
import org.junit.Test;

public class EbmlFloatTest {

    @Test
    public void test() {
        EbmlFloat durationElem = (EbmlFloat) createByType(Duration);
        durationElem.setDouble(5 * 1000.0);
        
        Assert.assertEquals(5000.0, durationElem.getDouble(),  0.0001);
        
    }

}
