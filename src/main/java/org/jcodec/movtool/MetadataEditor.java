package org.jcodec.movtool;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.jcodec.common.Format;
import org.jcodec.common.JCodecUtil;
import org.jcodec.containers.mp4.MP4Util;
import org.jcodec.containers.mp4.MP4Util.Movie;
import org.jcodec.containers.mp4.boxes.Header;
import org.jcodec.containers.mp4.boxes.MetaBox;
import org.jcodec.containers.mp4.boxes.MetaValue;
import org.jcodec.containers.mp4.boxes.MovieBox;
import org.jcodec.containers.mp4.boxes.MovieFragmentBox;
import org.jcodec.containers.mp4.boxes.NodeBox;

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
    private File source;

    public MetadataEditor(File source, Map<String, MetaValue> keyedMeta, Map<Integer, MetaValue> itunesMeta) {
        this.source = source;
        this.keyedMeta = keyedMeta;
        this.itunesMeta = itunesMeta;
    }

    public static MetadataEditor createFrom(File f) throws IOException {
        Format format = JCodecUtil.detectFormat(f);
        if (format != Format.MOV) {
            throw new IllegalArgumentException("Unsupported format: " + format);
        }

        Movie movie = MP4Util.parseMovie(f);
        MetaBox keyedMeta = NodeBox.findFirst(movie.getMoov(), MetaBox.class, MetaBox.fourcc());
        MetaBox itunesMeta = NodeBox.findFirstPath(movie.getMoov(), MetaBox.class,
                new String[] { "udta", MetaBox.fourcc() });

        return new MetadataEditor(f, keyedMeta == null ? new HashMap<String, MetaValue>() : keyedMeta.getKeyedMeta(),
                itunesMeta == null ? new HashMap<Integer, MetaValue>() : itunesMeta.getItunesMeta());
    }

    public void save() throws IOException {
        ReplaceMP4Editor mp4Editor = new ReplaceMP4Editor();
        mp4Editor.modifyOrReplace(source, new MP4Edit() {
            @Override
            public void applyToFragment(MovieBox mov, MovieFragmentBox[] fragmentBox) {
            }

            @Override
            public void apply(MovieBox movie) {
                MetaBox meta1 = NodeBox.findFirst(movie, MetaBox.class, MetaBox.fourcc());
                MetaBox meta2 = NodeBox.findFirstPath(movie, MetaBox.class, new String[] { "udta", MetaBox.fourcc() });
                if (keyedMeta != null) {
                    if (meta1 == null) {
                        meta1 = new MetaBox();
                        movie.add(meta1);
                    }
                    meta1.setKeyedMeta(keyedMeta);
                }

                if (itunesMeta != null) {
                    if (meta2 == null) {
                        meta2 = new MetaBox();
                        NodeBox udta = NodeBox.findFirst(movie, NodeBox.class, "udta");
                        if (udta == null) {
                            udta = new NodeBox(Header.createHeader("udta", 0));
                            movie.add(udta);
                        }
                        udta.add(meta2);
                    }
                    meta2.setFourccMeta(itunesMeta);
                }
            }
        });
    }

    public Map<Integer, MetaValue> getFourccMeta() {
        return itunesMeta;
    }

    public Map<String, MetaValue> getKeyedMeta() {
        return keyedMeta;
    }
}
