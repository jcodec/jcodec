package org.jcodec.containers.mp4;

import org.jcodec.containers.mp4.boxes.Box;
import org.jcodec.containers.mp4.boxes.MetaBox;
import org.jcodec.containers.mp4.boxes.MovieBox;
import org.jcodec.containers.mp4.boxes.NodeBox;
import org.jcodec.containers.mp4.boxes.UdtaBox;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class AppleGpsTest {

    @Test
    public void testAppleGps() throws IOException {
        MovieBox moov = MP4Util.parseMovie(new File("src/test/resources/applegps/gps1.mp4"));
        UdtaBox udta = NodeBox.findFirst(moov, UdtaBox.class, "udta");
        Box gps = findGps(udta);
        ByteBuffer data = getData(gps);
        assertNotNull(data);
        data.getInt(); //skip 4 bytes
        byte[] coordsBytes = new byte[data.remaining()];
        data.get(coordsBytes);
        String latlng = new String(coordsBytes);
        assertEquals("-35.2840+149.1215/", latlng);
    }

    static ByteBuffer getData(Box box) {
        if (box instanceof Box.LeafBox) {
            Box.LeafBox leaf = (Box.LeafBox) box;
            return leaf.getData();
        }
        return null;
    }

    static Box findGps(UdtaBox udta) {
        List<Box> boxes1 = udta.getBoxes();
        for (Box box : boxes1) {
            if (box.getFourcc().endsWith("xyz")) {
                return box;
            }
        }
        return null;
    }

    @Test
    public void testAppleGps2Meta() throws IOException {
        MovieBox moov = MP4Util.parseMovie(new File("src/test/resources/applegps/gps2.MOV"));
        MetaBox meta = NodeBox.findFirst(moov, MetaBox.class, "meta");
        String latlng1 = meta.getKeyedMeta().get("com.apple.quicktime.location.ISO6709").getString();
        assertEquals("-35.2844+149.1214+573.764/", latlng1);
        String latlng2 = meta.getItunesMeta().get(1).getString();
        assertEquals("-35.2844+149.1214+573.764/", latlng2);
    }
}
