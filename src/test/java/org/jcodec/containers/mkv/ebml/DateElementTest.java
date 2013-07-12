package org.jcodec.containers.mkv.ebml;

import static org.jcodec.containers.mkv.Type.DateUTC;
import static org.jcodec.containers.mkv.Type.Info;
import static org.jcodec.containers.mkv.Type.Segment;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.jcodec.containers.mkv.MKVTestSuite;
import org.jcodec.containers.mkv.SimpleEBMLParser;
import org.jcodec.containers.mkv.Type;
import org.junit.Assert;
import org.junit.Test;

public class DateElementTest {


    public void test() throws IOException {
        MKVTestSuite suite = MKVTestSuite.read();
        if (!suite.isSuitePresent())
            Assert.fail("MKV test suite is missing, please download from http://www.matroska.org/downloads/test_w1.html, and save to the path recorded in src/test/resources/mkv/suite.properties");

        FileInputStream inputStream = new FileInputStream(suite.test1);
        SimpleEBMLParser parser = new SimpleEBMLParser(inputStream.getChannel());
        try {
            parser.parse();
        } finally {
            if (inputStream != null)
                inputStream.close();
            inputStream = null;
        }
        DateElement origDate = Type.findFirst(parser.getTree(), Segment, Info, DateUTC);
        assertNotNull(origDate);
        assertArrayEquals(new byte[]{0x04, 0x38, 0x67, (byte)0x86, (byte)0xAE, (byte)0x98, 0x3E, 0x00}, origDate.data.array());
       
        DateElement dateElem = (DateElement) Type.createElementByType(DateUTC);
        dateElem.setValue(origDate.getValue());
        Assert.assertEquals(origDate.getValue(), dateElem.getValue());
        Assert.assertEquals(origDate.getDate(), dateElem.getDate());
    }
    
    @Test
    public void testFewBytesToLong() throws Exception {
        ByteBuffer bb = ByteBuffer.wrap(new byte[]{0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x01,0x00,0x00});
        long l = bb.getLong();
        Assert.assertEquals(1, l);
    }

}
