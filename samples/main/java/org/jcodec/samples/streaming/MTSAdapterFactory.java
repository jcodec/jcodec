package org.jcodec.samples.streaming;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.jcodec.player.util.ThreadUtil;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Creates MTS adaptors
 * 
 * @author The JCodec project
 * 
 */
public class MTSAdapterFactory {
    private Map<File, MTSIndexer> indexerCache = new HashMap<File, MTSIndexer>();

    public MTSAdapter create(File mtsFile, File indexFile) throws IOException {
        MTSIndexer indexer = indexerCache.get(indexFile);
        if (indexer != null) {
            if (indexer.isDone())
                indexerCache.remove(indexFile);
            waitForStreams(indexer.getIndex());
            return new MTSAdapter(mtsFile, indexer.getIndex());
        }

        MTSIndex index;
        if (indexFile.exists())
            index = MTSIndex.read(indexFile);
        else {
            index = new MTSIndex();
            indexerCache.put(indexFile, buildIndex(mtsFile, index, indexFile));
        }
        waitForStreams(index);

        return new MTSAdapter(mtsFile, index);
    }

    private void waitForStreams(MTSIndex index) {
        while (index.getStreamIds().size() == 0)
            ThreadUtil.sleepNoShit(500000);
        while (true) {
            boolean ready = true;
            for (Integer sid : index.getStreamIds()) {
                ready &= index.frame(sid, 20) != null;
            }
            if (ready)
                return;
            ThreadUtil.sleepNoShit(500000);
        }
    }

    private MTSIndexer buildIndex(File mtsFile, final MTSIndex index, final File indexFile) {
        final MTSIndexer indexer = new MTSIndexer(mtsFile, index);
        Thread ii = new Thread() {

            public void run() {
                try {
                    indexer.index();
                    index.write(indexFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        ii.setDaemon(true);
        ii.setName(mtsFile.getName() + " indexer");
        ii.start();

        return indexer;
    }
}
