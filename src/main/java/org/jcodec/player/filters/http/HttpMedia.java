package org.jcodec.player.filters.http;

import static org.apache.commons.lang.StringUtils.trim;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.jcodec.player.filters.MediaInfo;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * A media resource exposed via JCodec streaming
 * 
 * @author The JCodec project
 * 
 */
public class HttpMedia {

    private List<HttpPacketSource> tracks = new ArrayList<HttpPacketSource>();

    public HttpMedia(URL url, File cacheWhere) {
        try {
            cacheWhere = new File(cacheWhere, url.getHost() + "_" + url.getPath().replace("/", "_"));

            URLConnection con = url.openConnection();
            String data = IOUtils.toString(con.getInputStream());

            for (int i = 0; i < Integer.parseInt(trim(data)); i++) {
                tracks.add(new HttpPacketSource(new URL(url.toExternalForm() + "/" + i), new File(cacheWhere + "_" + i)));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public HttpPacketSource getVideoTrack() {
        for (HttpPacketSource packetSource : tracks) {
            if (packetSource.getMediaInfo() instanceof MediaInfo.VideoInfo)
                return packetSource;
        }
        return null;
    }

    public List<HttpPacketSource> getAudioTracks() {
        ArrayList<HttpPacketSource> result = new ArrayList<HttpPacketSource>();
        for (HttpPacketSource packetSource : tracks) {
            if (packetSource.getMediaInfo() instanceof MediaInfo.AudioInfo)
                result.add(packetSource);
        }
        return result;
    }
}