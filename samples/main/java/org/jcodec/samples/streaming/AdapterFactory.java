package org.jcodec.samples.streaming;

import java.io.File;
import java.io.IOException;

public class AdapterFactory {
    private MTSAdapterFactory mtsAdapterFactory;

    public Adapter createAdaptor(File mvFile) throws IOException {
        if (mvFile.getName().endsWith("m2t")) {
            File mtsCache = new File(System.getProperty("user.home"), "mpegts_cache");
            mtsCache.mkdir();
            return mtsAdapterFactory.create(mvFile, new File(mtsCache, mvFile.getName()));
        } else if (mvFile.getName().endsWith("mov"))
            return new QTAdapter(mvFile);
        throw new RuntimeException("Shit man!");
    }
}