package org.jcodec.android.sample;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;

import org.jcodec.api.android.FrameGrab;
import org.jcodec.common.SeekableByteChannel;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import org.jcodec.android.sample.R;

public class MainActivity extends AppCompatActivity {
    private static final String MOVIE_LOCATION = "http://jcodec.org/downloads/sample.mov";

    FrameGrab frameGrab;

    /**
     * This class implements a read-only SeekableBytChannel for downloading a HTTP resource.
     *
     * Basically it wraps a URLConnection. It provides random access functionality by closing
     * the connection and re-establishing a new one with HTTP 1.1 Range header to download a stream
     * from a certain offset.
     *
     * This class is the dummyest possible implementation and lacks some important features:
     *   - caching;
     *   - re-connecting vs reading ahead for forward seeks;
     *   - readahead and grouping of the small reads;
     */
    private class HttpChannel implements SeekableByteChannel {
        private static final String LOG_TAG_HTTP_CHANNEL = "HttpChannel";

        private URL url;
        private ReadableByteChannel ch;
        private long pos;
        private long length;

        public HttpChannel(URL url) {
            this.url = url;
        }

        @Override
        public long position() throws IOException {
            return pos;
        }

        @Override
        public SeekableByteChannel position(long newPosition) throws IOException {
            if(newPosition == pos)
                return this;
            if(ch != null) {
                ch.close();
                ch = null;
            }
            pos = newPosition;
            Log.d(LOG_TAG_HTTP_CHANNEL, "Seeking to: " + newPosition);
            return this;
        }

        @Override
        public long size() throws IOException {
            return length;
        }

        @Override
        public SeekableByteChannel truncate(long size) throws IOException {
            throw new IOException("Truncate on HTTP is not supported.");
        }

        @Override
        public int read(ByteBuffer buffer) throws IOException {
            ensureOpen();
            int read = ch.read(buffer);
            if(read != -1)
                pos += read;
            return read;
        }

        @Override
        public int write(ByteBuffer buffer) throws IOException {
            throw new IOException("Write to HTTP is not supported.");
        }

        @Override
        public boolean isOpen() {
            return ch != null && ch.isOpen();
        }

        @Override
        public void close() throws IOException {
            ch.close();
        }

        private void ensureOpen() throws IOException {
            if(ch == null) {
                URLConnection connection = url.openConnection();
                if(pos > 0)
                    connection.addRequestProperty("Range", "bytes=" + pos + "-");
                ch = Channels.newChannel(connection.getInputStream());
                String resp = connection.getHeaderField("Content-Range");
                if(resp != null) {
                    Log.d(LOG_TAG_HTTP_CHANNEL, resp);
                    length = Long.parseLong(resp.split("/")[1]);
                } else {
                    resp = connection.getHeaderField("Content-Length");
                    Log.d(LOG_TAG_HTTP_CHANNEL, resp);
                    length = Long.parseLong(resp);
                }
            }
        }
    }

    class DecodeTask extends AsyncTask<URL, Void, Bitmap> {
        private static final String LOG_TAG_DECODE_TASK = "DecodeTask";

        private Exception exception;

        protected Bitmap doInBackground(URL ...url) {
            try {
                if(frameGrab == null)
                    frameGrab = new FrameGrab(new HttpChannel(url[0]));
                return frameGrab.getFrame();
            } catch (Exception e) {
                Log.e(LOG_TAG_DECODE_TASK, "Could not decode one frame.", e);
                this.exception = e;
                return null;
            }
        }

        protected void onPostExecute(Bitmap feed) {
            if(feed != null) {
                new DecodeTask().execute();
                Log.d(LOG_TAG_DECODE_TASK, "Decoded image size: [" + feed.getWidth() + ", " + feed.getHeight() + "].");
                ImageView img = (ImageView) findViewById(R.id.frame);
                img.setImageBitmap(feed);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        try {
            new DecodeTask().execute(new URL(MOVIE_LOCATION));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
