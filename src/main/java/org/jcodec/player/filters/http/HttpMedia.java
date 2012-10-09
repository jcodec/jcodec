package org.jcodec.player.filters.http;

import static org.apache.commons.lang.StringUtils.trim;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.jcodec.player.filters.MediaInfo.VideoInfo;

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
    private HttpPacketSource videoTrack;
    private List<HttpPacketSource> audioTracks = new ArrayList<HttpPacketSource>();

    public HttpMedia(URL url, File cacheWhere) throws IOException {
        cacheWhere = new File(cacheWhere, url.getHost() + "_" + url.getPath().replace("/", "_"));

        URLConnection con = url.openConnection();
        String data = IOUtils.toString(con.getInputStream());

        for (int i = 0; i < Integer.parseInt(trim(data)); i++) {
            HttpPacketSource ps = new HttpPacketSource(url.toExternalForm() + "/" + i, new File(cacheWhere + "_" + i));
            tracks.add(ps);
            if (ps.getMediaInfo() instanceof VideoInfo)
                videoTrack = ps;
            else
                audioTracks.add(ps);
        }
    }

    public HttpPacketSource getVideoTrack() {
        return videoTrack;
    }

    public List<HttpPacketSource> getAudioTracks() {
        return audioTracks;
    }
}