package org.jcodec.movtool;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.jcodec.common.Format;
import org.jcodec.common.JCodecUtil;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.containers.mp4.MP4Util;
import org.jcodec.containers.mp4.MP4Util.Movie;
import org.jcodec.containers.mp4.boxes.Header;
import org.jcodec.containers.mp4.boxes.MetaBox;
import org.jcodec.containers.mp4.boxes.MetaValue;
import org.jcodec.containers.mp4.boxes.MovieBox;
import org.jcodec.containers.mp4.boxes.MovieFragmentBox;
import org.jcodec.containers.mp4.boxes.NodeBox;
import org.jcodec.containers.mp4.boxes.UdtaBox;
import org.jcodec.containers.mp4.boxes.UdtaMetaBox;
import org.jcodec.containers.mp4.boxes.Box.LeafBox;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class MetadataEditor {
    public static class MovieEditor {
        private Map<String, MetaValue> keyedMeta;
        private Map<Integer, MetaValue> itunesMeta;
        private Map<Integer, MetaValue> udata;

        public MovieEditor(Map<String, MetaValue> keyedMeta, Map<Integer, MetaValue> itunesMeta,
                Map<Integer, MetaValue> udata) {
            this.keyedMeta = keyedMeta;
            this.itunesMeta = itunesMeta;
            this.udata = udata;
        }

        public static MovieEditor createFromMovie(MovieBox moov) {
            MetaBox keyedMeta = (MetaBox) NodeBox.findFirst(moov, MetaBox.fourcc());
            MetaBox itunesMeta = (MetaBox) NodeBox.findFirstPath(moov, new String[] { "udta", MetaBox.fourcc() });

            UdtaBox udtaBox = (UdtaBox) NodeBox.findFirst(moov, "udta");

            return new MovieEditor(keyedMeta == null ? new HashMap<String, MetaValue>() : keyedMeta.getKeyedMeta(),
                    itunesMeta == null ? new HashMap<Integer, MetaValue>() : itunesMeta.getItunesMeta(),
                    udtaBox == null ? new HashMap<Integer, MetaValue>() : udtaBox.getMetadata());
        }

        public void apply(MovieBox movie) {
            MetaBox meta1 = (MetaBox) NodeBox.findFirst(movie, MetaBox.fourcc());
            MetaBox meta2 = (MetaBox) NodeBox.findFirstPath(movie, new String[] { "udta", MetaBox.fourcc() });
            if (keyedMeta != null && keyedMeta.size() > 0) {
                if (meta1 == null) {
                    meta1 = MetaBox.createMetaBox();
                    movie.add(meta1);
                }
                meta1.setKeyedMeta(keyedMeta);
            }

            if (itunesMeta != null && itunesMeta.size() > 0) {
                UdtaBox udta = (UdtaBox) NodeBox.findFirst(movie, "udta");
                if (meta2 == null) {
                    meta2 = UdtaMetaBox.createUdtaMetaBox();
                    if (udta == null) {
                        udta = UdtaBox.createUdtaBox();
                        movie.add(udta);
                    }
                    udta.add(meta2);
                }
                meta2.setItunesMeta(itunesMeta);
                udta.setMetadata(udata);
            }
        }

        public Map<Integer, MetaValue> getItunesMeta() {
            return itunesMeta;
        }

        public Map<String, MetaValue> getKeyedMeta() {
            return keyedMeta;
        }

        public Map<Integer, MetaValue> getUdata() {
            return udata;
        }
    }

    private File source;
    private MovieEditor movieEditor;

    public MetadataEditor(File source, MovieEditor movieEditor) {
        this.source = source;
        this.movieEditor = movieEditor;
    }

    public static MetadataEditor createFrom(File f) throws IOException {
        Format format = JCodecUtil.detectFormat(f);
        if (format != Format.MOV) {
            throw new IllegalArgumentException("Unsupported format: " + format);
        }

        Movie movie = MP4Util.parseFullMovie(f);
        return new MetadataEditor(f, MovieEditor.createFromMovie(movie.getMoov()));
    }

    public void save(boolean fast) throws IOException {
        // In Javascript you cannot access a field from the outer type.
        final MetadataEditor self = this;
        MP4Edit edit = new MP4Edit() {
            @Override
            public void applyToFragment(MovieBox mov, MovieFragmentBox[] fragmentBox) {
            }

            @Override
            public void apply(MovieBox movie) {
                movieEditor.apply(movie);
            }
        };
        if (fast) {
            new RelocateMP4Editor().modifyOrRelocate(source, edit);
        } else {
            new ReplaceMP4Editor().modifyOrReplace(source, edit);
        }
    }

    public File getSource() {
        return source;
    }

    public MovieEditor getMovieEditor() {
        return movieEditor;
    }

    public Map<Integer, MetaValue> getItunesMeta() {
        return movieEditor.getItunesMeta();
    }

    public Map<String, MetaValue> getKeyedMeta() {
        return movieEditor.getKeyedMeta();
    }

    public Map<Integer, MetaValue> getUdata() {
        return movieEditor.getUdata();
    }
}
