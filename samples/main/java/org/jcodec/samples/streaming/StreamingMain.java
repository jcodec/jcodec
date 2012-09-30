package org.jcodec.samples.streaming;

import java.io.File;
import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Stub streaming server that demonstrates JCodec streaming
 * 
 * @author The JCodec project
 * 
 */
public class StreamingMain {

    public StreamingMain(String[] args) throws Exception {
        System.out.println("JCodec streaming server " + getVersion());

        Server jetty = new Server(8085);
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        jetty.setHandler(context);
        context.addServlet(new ServletHolder(new StreamingServlet(new File(System.getProperty("user.home")))), "/*");
        jetty.start();
        jetty.join();
    }

    private String getVersion() {
        Properties props = new Properties();
        InputStream is = null;
        try {
            is = this.getClass().getClassLoader()
                    .getResourceAsStream("META-INF/maven/org.jcodec/jcodec-samples/pom.properties");
            props.load(is);
            return props.getProperty("version");
        } catch (Throwable t) {
            return "Development";
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    public static void main(String[] args) throws Exception {
        new StreamingMain(args);
    }
}