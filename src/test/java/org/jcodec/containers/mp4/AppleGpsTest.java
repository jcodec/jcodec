package org.jcodec.containers.mp4;

import org.jcodec.containers.mp4.boxes.MetaBox;
import org.jcodec.containers.mp4.boxes.MovieBox;
import org.jcodec.containers.mp4.boxes.NodeBox;
import org.jcodec.containers.mp4.boxes.UdtaBox;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class AppleGpsTest {

    @Test
    public void testAppleGps() throws IOException {
        MovieBox moov = MP4Util.parseMovie(new File("src/test/resources/applegps/gps1.mp4"));
        UdtaBox udta = (UdtaBox) NodeBox.findFirst(moov, "udta");
        String latlng = udta.latlng();
        assertEquals("-35.2840+149.1215/", latlng);
    }

    @Test
    public void testAppleGps2Meta() throws IOException {
        MovieBox moov = MP4Util.parseMovie(new File("src/test/resources/applegps/gps2.MOV"));
        MetaBox meta = (MetaBox) NodeBox.findFirst(moov, "meta");
        String latlng1 = meta.getKeyedMeta().get("com.apple.quicktime.location.ISO6709").getString();
        assertEquals("-35.2844+149.1214+573.764/", latlng1);
        String latlng2 = meta.getItunesMeta().get(1).getString();
        assertEquals("-35.2844+149.1214+573.764/", latlng2);
    }
}
