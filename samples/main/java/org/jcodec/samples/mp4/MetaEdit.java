package org.jcodec.samples.mp4;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.jcodec.containers.mp4.boxes.MetaValue;
import org.jcodec.movtool.MetadataEditor;

public class MetaEdit {
    public static void main(String[] args) throws IOException {
        File infile = new File(args[0]);
        MetadataEditor mediaMeta = MetadataEditor.createFrom(infile);
        Map<Integer, MetaValue> meta = mediaMeta.getItunesMeta();
        meta.put(0xa9616c62, MetaValue.createString("My recorder album"));
        meta.put(0xa96e616d, MetaValue.createString("My recorder title"));
        Map<Integer, MetaValue> udata = mediaMeta.getUdata();
        udata.put(0xa978797a,MetaValue.createStringWithLocale("+81.1000-015.5999/", 0x15c7));
        mediaMeta.save(false); // fast mode is off
    }
}