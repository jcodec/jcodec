package org.jcodec.player.filters.http;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.impl.client.DefaultHttpClient;

public class HttpUtils {

    public static HttpClient getHttpClient(String trackUrl) throws IOException {
        DefaultHttpClient client = new DefaultHttpClient();
        try {
            for (Proxy proxy : ProxySelector.getDefault().select(new URI(trackUrl))) {
                if (proxy == proxy.NO_PROXY)
                    continue;
                InetSocketAddress address = (InetSocketAddress) proxy.address();
                HttpHost httpHost = new HttpHost(address.getAddress().getHostAddress(), address.getPort());
                client.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, httpHost);
                System.out.println("Using proxy server: " + address.getHostName() + ":" + address.getPort());
                break;
            }
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
        return client;
    }

    public static HttpResponse privilegedExecute(final HttpClient client, final HttpGet get) throws IOException {
        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<HttpResponse>() {
                public HttpResponse run() throws IOException {
                    return client.execute(get);
                }
            });
        } catch (PrivilegedActionException e) {
            throw (IOException) e.getCause();
        }
    }
}
