package org.jcodec.movtool;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.jcodec.common.io.IOUtils;
import org.jcodec.containers.mp4.MP4Util;
import org.jcodec.containers.mp4.MP4Util.Atom;
import org.jcodec.containers.mp4.boxes.MetaValue;
import org.junit.Assert;
import org.junit.Test;

public class MetadataEditorTest {
    public static final String KEYED_PATH = "src/test/resources/metaedit/keyed.mp4";
    public static final String ITUNES_PATH = "src/test/resources/metaedit/itunes.mov";
    
    public static final int FOURCC_ART = 0xa9415254;
    public static final int FOURCC_NAM = 0xa96e616d;
    public static final int FOURCC_COVR = 0x636f7672;
    
    @Test
    public void testKeyedRead() throws IOException {
        MetadataEditor editor = MetadataEditor.createFrom(new File(KEYED_PATH));
        Map<String, MetaValue> keyedMeta = editor.getKeyedMeta();
        Assert.assertEquals("7.1.2", keyedMeta.get("com.android.version").getString());
        Assert.assertEquals(30f, keyedMeta.get("com.android.capture.fps").getFloat(), .00001);
    }
    
    @Test
    public void testItunesRead() throws IOException {
        MetadataEditor editor = MetadataEditor.createFrom(new File(ITUNES_PATH));
        Map<Integer, MetaValue> itunesMeta = editor.getItunesMeta();
        Assert.assertEquals("Stan", itunesMeta.get(FOURCC_ART).getString());
        Assert.assertEquals("Cool cool", itunesMeta.get(FOURCC_NAM).getString());
    }
    
    @Test
    public void testInplace() throws IOException {
        File file = File.createTempFile("metaedit_test", ".mp4");
        file.deleteOnExit();
        IOUtils.copyFile(new File(KEYED_PATH), file);
        
        MetadataEditor editor = MetadataEditor.createFrom(file);
        Atom mdat1 = MP4Util.findFirstAtomInFile("mdat", file);
        editor.save(true);
        Atom mdat2 = MP4Util.findFirstAtomInFile("mdat", file);
        Assert.assertEquals(mdat1.getOffset(), mdat2.getOffset());
        editor.save(false);
        Atom mdat3 = MP4Util.findFirstAtomInFile("mdat", file);
        Assert.assertEquals(mdat1.getOffset(), mdat3.getOffset());
    }

    @Test
    public void testKeyedWriteSlow() throws IOException {
        keyedWriteSub(false);
    }

    @Test
    public void testKeyedWriteFast() throws IOException {
        keyedWriteSub(true);
    }

    private void keyedWriteSub(boolean fast) throws IOException {
        File file = File.createTempFile("metaedit_test", ".mp4");
        file.deleteOnExit();
        IOUtils.copyFile(new File(KEYED_PATH), file);

        {
            MetadataEditor editor = MetadataEditor.createFrom(file);
            Map<String, MetaValue> keyedMeta = editor.getKeyedMeta();
            keyedMeta.put("val_string", MetaValue.createString("Value1"));
            keyedMeta.put("val_int", MetaValue.createInt(42));
            keyedMeta.put("val_float", MetaValue.createFloat(42.42f));
            editor.save(fast);
        }

        MetadataEditor editor = MetadataEditor.createFrom(file);
        Map<String, MetaValue> keyedMeta = editor.getKeyedMeta();
        Assert.assertEquals("7.1.2", keyedMeta.get("com.android.version").getString());
        Assert.assertEquals(30f, keyedMeta.get("com.android.capture.fps").getFloat(), .00001);
        Assert.assertEquals("Value1", keyedMeta.get("val_string").getString());
        Assert.assertEquals(42, keyedMeta.get("val_int").getInt());
        Assert.assertEquals(42.42f, keyedMeta.get("val_float").getFloat(), .00001);
    }
    
    public void testItunesdWrite() throws IOException {
        File file = File.createTempFile("metaedit_test", ".mp4");
        file.deleteOnExit();
        IOUtils.copyFile(new File(ITUNES_PATH), file);

        byte[] jpeg = new byte[] {0, 1, 2, 3};
        
        {
            MetadataEditor editor = MetadataEditor.createFrom(file);
            Map<Integer, MetaValue> itunesMeta = editor.getItunesMeta();
            itunesMeta.put(FOURCC_COVR, MetaValue.createOther(MetaValue.TYPE_JPEG, jpeg));
            editor.save(false);
        }

        MetadataEditor editor = MetadataEditor.createFrom(file);
        Map<Integer, MetaValue> itunesMeta = editor.getItunesMeta();
        Assert.assertArrayEquals(jpeg, itunesMeta.get(FOURCC_COVR).getData());
    }

}
