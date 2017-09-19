package org.jcodec.movtool;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jcodec.common.io.IOUtils;
import org.jcodec.common.tools.MainUtils;
import org.jcodec.common.tools.MainUtils.Cmd;
import org.jcodec.common.tools.MainUtils.Flag;
import org.jcodec.common.tools.MainUtils.FlagType;
import org.jcodec.containers.mp4.boxes.MetaValue;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class MetadataEditorMain {
    private static final String TYPENAME_FLOAT = "float";
    private static final String TYPENAME_INT2 = "integer";
    private static final String TYPENAME_INT = "int";
    private static final Flag FLAG_SET_KEYED = new Flag("set-keyed", "sk",
            "key1=value1[(type1)]:key2=value2[(type2)]. Sets the metadata piece into a file.");
    private static final Flag FLAG_SET_ITUNES = new Flag("set-itunes", "si",
            "fourcc1=value1[(type1)]:fourcc2=value2[(type2)]. Sets the metadata piece into a file.");
    private static final Flag FLAG_SET_ITUNES_BLOB = new Flag("set-itunes-blob", "sib",
            "fourcc[(type)]. Sets the metadata piece into as a blob read from the stdin.");
    private static final Flag FLAG_QUERY = new Flag("query", "q", "Query the value of one key from the metadata set.");
    private static final Flag FLAG_FAST = new Flag("fast", "f",
            "Fast edit, will move the " + "header to the end of the file when ther's no room to fit it.",
            FlagType.VOID);
    private static final Flag[] flags = { FLAG_SET_KEYED, FLAG_SET_ITUNES, FLAG_QUERY, FLAG_FAST,
            FLAG_SET_ITUNES_BLOB };

    private static final Pattern valuePattern = Pattern.compile("(.+)=([^\\(]+)(\\(.*\\))?");
    private static final Pattern blobPattern = Pattern.compile("(.+)(\\(.*\\))?");

    private static Map<String, Integer> strToType = new HashMap<String, Integer>();

    static {
        strToType.put(TYPENAME_FLOAT, 23);
        strToType.put(TYPENAME_INT, 21);
        strToType.put(TYPENAME_INT2, 21);
        strToType.put("jpeg", 13);
        strToType.put("jpg", 13);
        strToType.put("jpg", 14);
        strToType.put("bmp", 27);
    }

    public static void main(String[] args) throws IOException {
        Cmd cmd = MainUtils.parseArguments(args, flags);
        if (cmd.argsLength() < 1) {
            MainUtils.printHelpVarArgs(flags, "file name");
            System.exit(-1);
            return;
        }

        MetadataEditor mediaMeta = MetadataEditor.createFrom(new File(cmd.getArg(0)));
        boolean save = false;
        String flagSetKeyed = cmd.getStringFlag(FLAG_SET_KEYED);
        if (flagSetKeyed != null) {
            Map<String, MetaValue> map = parseMetaSpec(flagSetKeyed);
            save |= map.size() > 0;
            mediaMeta.getKeyedMeta().putAll(map);
        }

        String flagSetFourcc = cmd.getStringFlag(FLAG_SET_ITUNES);
        if (flagSetFourcc != null) {
            Map<Integer, MetaValue> map = toFourccMeta(parseMetaSpec(flagSetFourcc));
            save |= map.size() > 0;
            mediaMeta.getItunesMeta().putAll(map);
        }

        String flagSetItunesBlob = cmd.getStringFlag(FLAG_SET_ITUNES_BLOB);
        if (flagSetItunesBlob != null) {
            Matcher matcher = valuePattern.matcher(flagSetItunesBlob);
            if (matcher.matches()) {
                Integer type = 1;
                String typeRaw = matcher.group(2);
                if (typeRaw != null) {
                    typeRaw = typeRaw.substring(1, typeRaw.length() - 1);
                    type = strToType.get(typeRaw);
                }
                if (type != null) {
                    String name = matcher.group(1);
                    byte[] data = readStdin();
                    mediaMeta.getItunesMeta().put(stringToFourcc(name), MetaValue.createOther(type, data));
                } else {
                    System.err.println("Unsupported metadata type: " + typeRaw);
                }
            }
        }

        if (save) {
            mediaMeta.save(cmd.getBooleanFlag(FLAG_FAST));
            mediaMeta = MetadataEditor.createFrom(new File(cmd.getArg(0)));
        }

        Map<String, MetaValue> keyedMeta = mediaMeta.getKeyedMeta();
        if (keyedMeta != null) {
            String flagQuery = cmd.getStringFlag(FLAG_QUERY);
            if (flagQuery == null) {
                System.out.println("Keyed metadata:");
                for (Entry<String, MetaValue> entry : keyedMeta.entrySet()) {
                    System.out.println(entry.getKey() + ": " + entry.getValue());
                }
            } else {
                printValue(keyedMeta.get(flagQuery));
            }
        }

        Map<Integer, MetaValue> itunesMeta = mediaMeta.getItunesMeta();
        if (itunesMeta != null) {
            String flagQuery = cmd.getStringFlag(FLAG_QUERY);
            if (flagQuery == null) {
                System.out.println("iTunes metadata:");
                for (Entry<Integer, MetaValue> entry : itunesMeta.entrySet()) {
                    System.out.println(fourccToString(entry.getKey()) + ": " + entry.getValue());
                }
            } else {
                printValue(itunesMeta.get(stringToFourcc(flagQuery)));
            }
        }
    }

    private static byte[] readStdin() throws IOException {
        return IOUtils.toByteArray(System.in);
    }

    private static void printValue(MetaValue value) throws IOException {
        if (value == null)
            return;
        if (value.isBlob())
            System.out.write(value.getData());
        else
            System.out.println(value);
    }

    private static Map<Integer, MetaValue> toFourccMeta(Map<String, MetaValue> keyed) {
        HashMap<Integer, MetaValue> ret = new HashMap<Integer, MetaValue>();
        for (Entry<String, MetaValue> entry : keyed.entrySet()) {
            ret.put(stringToFourcc(entry.getKey()), entry.getValue());
        }
        return ret;
    }

    private static Map<String, MetaValue> parseMetaSpec(String flagSetKeyed) {
        Map<String, MetaValue> map = new HashMap<String, MetaValue>();
        for (String value : flagSetKeyed.split(":")) {
            Matcher matcher = valuePattern.matcher(value);
            if (!matcher.matches())
                continue;
            String type = matcher.group(3);
            if (type != null) {
                type = type.substring(1, type.length() - 1);
            }
            map.put(matcher.group(1), typedValue(matcher.group(2), type));
        }
        return map;
    }

    private static String fourccToString(int key) {
        try {
            byte[] bytes = new byte[4];
            ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).putInt(key);
            return new String(bytes, "iso8859-1");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private static int stringToFourcc(String fourcc) {
        if (fourcc.length() != 4)
            return 0;
        try {
            byte[] bytes = fourcc.getBytes("iso8859-1");
            return ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).getInt();
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private static MetaValue typedValue(String value, String type) {
        if (TYPENAME_INT.equalsIgnoreCase(type) || TYPENAME_INT2.equalsIgnoreCase(type))
            return MetaValue.createInt(Integer.parseInt(value));
        if (TYPENAME_FLOAT.equalsIgnoreCase(type))
            return MetaValue.createFloat(Float.parseFloat(value));
        return MetaValue.createString(value);
    }
}
