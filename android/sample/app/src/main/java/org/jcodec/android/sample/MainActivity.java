package org.jcodec.android.sample;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;

import org.jcodec.api.android.BitmapWithMetadata;
import org.jcodec.api.android.FrameGrab8Bit;
import org.jcodec.common.io.HttpChannel;
import org.jcodec.common.io.TiledChannel;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    private static final String MOVIE_LOCATION = "http://jcodec.org/downloads/sample.mov";

    private FrameGrab8Bit frameGrab;
    private List<Bitmap> available = new LinkedList<Bitmap>();
    private List<BitmapWithMetadata> decoded = new LinkedList<BitmapWithMetadata>();
    private Comparator<BitmapWithMetadata> cmp = new Comparator<BitmapWithMetadata>() {
            @Override
            public int compare(BitmapWithMetadata lhs, BitmapWithMetadata rhs) {
            return lhs.getTimestamp() < rhs.getTimestamp() ? -1 :
                    (lhs.getTimestamp() == rhs.getTimestamp() ? 0 : 1);
        }
    };
    private static final ScheduledExecutorService worker =
            Executors.newSingleThreadScheduledExecutor();


    class DecodeTask extends AsyncTask<URL, Void, BitmapWithMetadata> {
        private static final String LOG_TAG_DECODE_TASK = "DecodeTask";

        private Exception exception;

        protected BitmapWithMetadata doInBackground(URL ...url) {
            try {
                if(frameGrab == null)
                    frameGrab = new FrameGrab8Bit(new TiledChannel(new HttpChannel(url[0])));
                if(available.isEmpty())
                    return frameGrab.getFrameWithMetadata();
                else
                    return frameGrab.getFrameWithMetadata(available.remove(0));
            } catch (Exception e) {
                Log.e(LOG_TAG_DECODE_TASK, "Could not decode one frame.", e);
                this.exception = e;
                return null;
            }
        }

        protected void onPostExecute(BitmapWithMetadata bitmapWithMetadata) {
            if (bitmapWithMetadata != null) {
                new DecodeTask().execute();
                Log.d(LOG_TAG_DECODE_TASK,
                        "Decoded image size: [" + bitmapWithMetadata.getBitmap().getWidth() + ", "
                                + bitmapWithMetadata.getBitmap().getHeight() + "].");

                decoded.add(bitmapWithMetadata);
                Collections.sort(decoded, cmp);

                if(decoded.size() > 3) {
                    Bitmap bitmap = decoded.remove(0).getBitmap();
                    display(bitmap);
                    available.add(bitmap);
                }
            } else {
                double delay = 0;
                while (!decoded.isEmpty()) {
                    BitmapWithMetadata bitmap = decoded.remove(0);
                    worker.schedule(new DisplayTask(bitmap.getBitmap()), (int)(delay * 1000),
                            TimeUnit.MILLISECONDS);
                    delay += bitmap.getDuration();
                }
            }
        }

        protected void display(Bitmap bitmap) {
            ImageView img = (ImageView) findViewById(R.id.frame);
            img.setImageBitmap(bitmap);
        }
    }

    class DisplayTask implements Runnable {
        private final Bitmap bitmap;

        public DisplayTask(Bitmap bitmap) {
            this.bitmap = bitmap;
        }
        @Override
        public void run() {
            ImageView img = (ImageView) findViewById(R.id.frame);
            img.setImageBitmap(bitmap);
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
