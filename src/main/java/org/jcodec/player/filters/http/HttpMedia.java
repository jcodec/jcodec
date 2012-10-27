package org.jcodec.player.filters.http;

import static org.apache.commons.lang.StringUtils.trim;
import static org.jcodec.player.filters.http.HttpUtils.getHttpClient;
import static org.jcodec.player.filters.http.HttpUtils.privilegedExecute;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.util.EntityUtils;
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

        String data = requestInfo(url, getHttpClient(url.toExternalForm()));

        for (int i = 0; i < Integer.parseInt(trim(data)); i++) {
            try {
                HttpPacketSource ps = new HttpPacketSource(url.toExternalForm() + "/" + i, new File(cacheWhere + "_"
                        + i));
                tracks.add(ps);
                if (ps.getMediaInfo() instanceof VideoInfo)
                    videoTrack = ps;
                else
                    audioTracks.add(ps);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String requestInfo(URL url, HttpClient client) throws IOException {
        HttpGet get = new HttpGet(url.toExternalForm());
        get.getParams().setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.IGNORE_COOKIES);
        HttpResponse response = privilegedExecute(client, get);
        if (response.getStatusLine().getStatusCode() == 200) {
            return EntityUtils.toString(response.getEntity());
        } else
            throw new IOException("Could not get the media info [" + url.toExternalForm() + "]:"
                    + response.getStatusLine().getStatusCode() + " " + response.getStatusLine().getReasonPhrase());
    }

    public HttpPacketSource getVideoTrack() {
        return videoTrack;
    }

    public List<HttpPacketSource> getAudioTracks() {
        return audioTracks;
    }
}