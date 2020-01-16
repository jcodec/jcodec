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
    private Map<String, MetaValue> keyedMeta;
    private Map<Integer, MetaValue> itunesMeta;
    private Map<Integer, MetaValue> udata;
    private File source;

    public MetadataEditor(File source, Map<String, MetaValue> keyedMeta, Map<Integer, MetaValue> itunesMeta, Map<Integer, MetaValue> udata) {
        this.source = source;
        this.keyedMeta = keyedMeta;
        this.itunesMeta = itunesMeta;
        this.udata = udata;
    }

    public static MetadataEditor createFrom(File f) throws IOException {
        Format format = JCodecUtil.detectFormat(f);
        if (format != Format.MOV) {
            throw new IllegalArgumentException("Unsupported format: " + format);
        }

        Movie movie = MP4Util.parseFullMovie(f);
        MetaBox keyedMeta = NodeBox.findFirst(movie.getMoov(), MetaBox.class, MetaBox.fourcc());
        MetaBox itunesMeta = NodeBox.findFirstPath(movie.getMoov(), MetaBox.class,
                new String[] { "udta", MetaBox.fourcc() });

        UdtaBox udtaBox = NodeBox.findFirst(movie.getMoov(), UdtaBox.class, "udta");

        return new MetadataEditor(f, keyedMeta == null ? new HashMap<String, MetaValue>() : keyedMeta.getKeyedMeta(),
                itunesMeta == null ? new HashMap<Integer, MetaValue>() : itunesMeta.getItunesMeta(),
                udtaBox == null ? new HashMap<Integer, MetaValue>() : udtaBox.getMetadata());
    }

    public void save(boolean fast) throws IOException {
        //In Javascript you cannot access a field from the outer type.
        final MetadataEditor self = this;
        MP4Edit edit = new MP4Edit() {
            @Override
            public void applyToFragment(MovieBox mov, MovieFragmentBox[] fragmentBox) {
            }

            @Override
            public void apply(MovieBox movie) {
                MetaBox meta1 = NodeBox.findFirst(movie, MetaBox.class, MetaBox.fourcc());
                MetaBox meta2 = NodeBox.findFirstPath(movie, MetaBox.class, new String[] { "udta", MetaBox.fourcc() });
                if (self.keyedMeta != null && self.keyedMeta.size() > 0) {
                    if (meta1 == null) {
                        meta1 = MetaBox.createMetaBox();
                        movie.add(meta1);
                    }
                    meta1.setKeyedMeta(self.keyedMeta);
                }

                if (self.itunesMeta != null && self.itunesMeta.size() > 0) {
                    UdtaBox udta = NodeBox.findFirst(movie, UdtaBox.class, "udta");
                    if (meta2 == null) {
                        meta2 = UdtaMetaBox.createUdtaMetaBox();
                        if (udta == null) {
                            udta = UdtaBox.createUdtaBox();
                            movie.add(udta);
                        }
                        udta.add(meta2);
                    }
                    meta2.setItunesMeta(self.itunesMeta);
                    udta.setMetadata(self.udata);
                }
            }
        };
        if (fast) {
            new RelocateMP4Editor().modifyOrRelocate(source, edit);
        } else {
            new ReplaceMP4Editor().modifyOrReplace(source, edit);
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
