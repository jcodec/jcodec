package org.jcodec.platform;

import org.jcodec.api.transcode.filters.ScaleFilter;
import org.jcodec.common.model.Size;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PlatformTest {
    @Test
    public void testNewInstance() {
        ScaleFilter scaleFilter = Platform.newInstance(ScaleFilter.class, new Object[]{42, 43});
        Size target = scaleFilter.getTarget();
        assertEquals(42, target.getWidth());
        assertEquals(43, target.getHeight());
    }
}
