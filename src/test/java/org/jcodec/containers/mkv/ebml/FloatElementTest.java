package org.jcodec.containers.mkv.ebml;

import junit.framework.Assert;

import org.jcodec.containers.mkv.Type;
import org.junit.Test;

public class FloatElementTest {

    @Test
    public void test() {
        FloatElement durationElem = (FloatElement) Type.createElementByType(Type.Duration);
        durationElem.set(5 * 1000.0);
        
        Assert.assertEquals(5000.0, durationElem.get(),  0.0001);
        
    }

}
