package org.jcodec.movtool;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jcodec.common.Format;
import org.jcodec.common.JCodecUtil;
import org.jcodec.common.tools.MainUtils;
import org.jcodec.common.tools.MainUtils.Cmd;
import org.jcodec.common.tools.MainUtils.Flag;
import org.jcodec.containers.mp4.MP4Util;
import org.jcodec.containers.mp4.boxes.MetaBox;
import org.jcodec.containers.mp4.boxes.MovieBox;
import org.jcodec.containers.mp4.boxes.MovieFragmentBox;
import org.jcodec.containers.mp4.boxes.NodeBox;
import org.jcodec.containers.mp4.boxes.MetaValue;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class MetadataEditor {
    private static final String TYPENAME_FLOAT = "float";
    private static final String TYPENAME_INT2 = "integer";
    private static final String TYPENAME_INT = "int";
    private static final Flag FLAG_SET = new Flag("set", "s",
            "key1=value1[(type1)]:key2=value2[(type2)]. Sets the metadat piece into a file.");
    private static final Flag FLAG_QUERY = new Flag("query", "q", "Query the value of one key from the metadata set.");
    private static final Flag[] flags = { FLAG_SET, FLAG_QUERY };

    private static final Pattern valuePattern = Pattern.compile("(.+)=([^\\(]+)(\\(.*\\))?");

    public static void main(String[] args) throws IOException {
        Cmd cmd = MainUtils.parseArguments(args, flags);
        if (cmd.argsLength() < 1) {
            MainUtils.printHelpVarArgs(flags, "file name");
            System.exit(-1);
            return;
        }
        MetadataEditor mediaMeta = new MetadataEditor();

        String flagSet = cmd.getStringFlag(FLAG_SET);
        if (flagSet != null) {
            Map<String, MetaValue> map = new HashMap<String, MetaValue>();
            for (String value : flagSet.split(":")) {
                Matcher matcher = valuePattern.matcher(value);
                if (!matcher.matches())
                    continue;
                String type = matcher.group(3);
                if (type != null) {
                    type = type.substring(1, type.length() - 1);
                }
                map.put(matcher.group(1), typedValue(matcher.group(2), type));
            }
            mediaMeta.setMediaMeta(new File(cmd.getArg(0)), map);
        }

        Map<String, MetaValue> meta2 = mediaMeta.getMediaMeta(new File(cmd.getArg(0)));
        if (meta2 != null) {
            String flagQuery = cmd.getStringFlag(FLAG_QUERY);
            if (flagQuery == null) {
                for (Entry<String, MetaValue> entry : meta2.entrySet()) {
                    System.out.println(entry.getKey() + ": " + entry.getValue());
                }
            } else {
                Object value = meta2.get(flagQuery);
                if (value != null) {
                    System.out.println(value);
                }
            }
        }
    }

    private static MetaValue typedValue(String value, String type) {
        if (TYPENAME_INT.equalsIgnoreCase(type) || TYPENAME_INT2.equalsIgnoreCase(type))
            return MetaValue.createInt(Integer.parseInt(value));
        if (TYPENAME_FLOAT.equalsIgnoreCase(type))
            return MetaValue.createFloat(Float.parseFloat(value));
        return MetaValue.createString(value);
    }

    public Map<String, MetaValue> getMediaMeta(File f) throws IOException {
        Format format = JCodecUtil.detectFormat(f);
        if (format != Format.MOV) {
            throw new IllegalArgumentException("Unsupported format: " + format);
        }
        MovieBox movie = MP4Util.parseMovie(f).getMoov();
        MetaBox meta = NodeBox.findFirst(movie, MetaBox.class, MetaBox.fourcc());
        if (meta != null)
            return meta.getMeta();
        return null;
    }

    public void setMediaMeta(final File file, final Map<String, MetaValue> map) throws IOException {
        Format format = JCodecUtil.detectFormat(file);
        if (format != Format.MOV) {
            throw new IllegalArgumentException("Unsupported format: " + format);
        }

        ReplaceMP4Editor mp4Editor = new ReplaceMP4Editor();
        mp4Editor.modifyOrReplace(file, new MP4Edit() {
            @Override
            public void applyToFragment(MovieBox mov, MovieFragmentBox[] fragmentBox) {
            }

            @Override
            public void apply(MovieBox movie) {
                MetaBox meta = NodeBox.findFirst(movie, MetaBox.class, MetaBox.fourcc());
                if (meta != null) {
                    map.putAll(meta.getMeta());
                }
                meta.setMeta(map);
            }
        });
    }
}
