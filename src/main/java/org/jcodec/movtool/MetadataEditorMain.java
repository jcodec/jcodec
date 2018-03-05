package org.jcodec.movtool;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.jcodec.common.io.IOUtils;
import org.jcodec.common.tools.MainUtils;
import org.jcodec.common.tools.MainUtils.Cmd;
import org.jcodec.common.tools.MainUtils.Flag;
import org.jcodec.common.tools.MainUtils.FlagType;
import org.jcodec.containers.mp4.boxes.MetaValue;
import org.jcodec.platform.Platform;

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
    private static final Flag FLAG_SET_KEYED = Flag.flag("set-keyed", "sk",
            "key1[,type1]=value1:key2[,type2]=value2[,...] Sets the metadata piece into a file.");
    private static final Flag FLAG_SET_ITUNES = Flag.flag("set-itunes", "si",
            "key1[,type1]=value1:key2[,type2]=value2[,...] Sets the metadata piece into a file.");
    private static final Flag FLAG_SET_ITUNES_BLOB = Flag.flag("set-itunes-blob", "sib",
            "key[,type]=file Sets the data read from a file into the metadata field 'key'. If file is not present stdin is read.");
    private static final Flag FLAG_QUERY = Flag.flag("query", "q", "Query the value of one key from the metadata set.");
    private static final Flag FLAG_FAST = new Flag("fast", "f",
            "Fast edit, will move the " + "header to the end of the file when ther's no room to fit it.",
            FlagType.VOID);
    private static final Flag FLAG_DROP_KEYED = Flag.flag("drop-keyed", "dk", "Drop the field(s) from keyed metadata,"
            + " format: key1,key2,key3,...");
    private static final Flag FLAG_DROP_ITUNES = Flag.flag("drop-itunes", "di",
            "Drop the field(s) from iTunes metadata," + " format: key1,key2,key3,...");
    private static final Flag[] flags = { FLAG_SET_KEYED, FLAG_SET_ITUNES, FLAG_QUERY, FLAG_FAST, FLAG_SET_ITUNES_BLOB,
            FLAG_DROP_KEYED, FLAG_DROP_ITUNES };

    private static Map<String, Integer> strToType = new HashMap<String, Integer>();

    static {
        strToType.put("utf8", 1);
        strToType.put("utf16", 2);
        strToType.put(TYPENAME_FLOAT, 23);
        strToType.put(TYPENAME_INT, 21);
        strToType.put(TYPENAME_INT2, 21);
        strToType.put("jpeg", 13);
        strToType.put("jpg", 13);
        strToType.put("png", 14);
        strToType.put("bmp", 27);
    }

    public static void main(String[] args) throws IOException {
        Cmd cmd = MainUtils.parseArguments(args, flags);
        if (cmd.argsLength() < 1) {
            MainUtils.printHelpCmdVa("metaedit", flags, "file name");
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
        
        String flagDropKeyed = cmd.getStringFlag(FLAG_DROP_KEYED);
        if (flagDropKeyed != null) {
            String[] keys = flagDropKeyed.split(",");
            Map<String, MetaValue> keyedMeta = mediaMeta.getKeyedMeta();
            
            for (String key : keys) {
                save |= keyedMeta.remove(key) != null;
            }
        }
        
        String flagDropItunes = cmd.getStringFlag(FLAG_DROP_ITUNES);
        if (flagDropItunes != null) {
            String[] keys = flagDropItunes.split(",");
            Map<Integer, MetaValue> itunesMeta = mediaMeta.getItunesMeta();
            
            for (String key : keys) {
                int fourcc = stringToFourcc(key);
                save |= itunesMeta.remove(fourcc) != null;
            }
        }

        String flagSetItunes = cmd.getStringFlag(FLAG_SET_ITUNES);
        if (flagSetItunes != null) {
            Map<Integer, MetaValue> map = toFourccMeta(parseMetaSpec(flagSetItunes));
            save |= map.size() > 0;
            mediaMeta.getItunesMeta().putAll(map);
        }

        String flagSetItunesBlob = cmd.getStringFlag(FLAG_SET_ITUNES_BLOB);
        if (flagSetItunesBlob != null) {
            String[] lr = flagSetItunesBlob.split("=");
            String[] kt = lr[0].split(",");
            String key = kt[0];

            Integer type = 1;
            if (kt.length > 1) {
                type = strToType.get(kt[1]);
            }
            if (type != null) {
                byte[] data = readStdin(lr.length > 1 ? lr[1] : null);
                mediaMeta.getItunesMeta().put(stringToFourcc(key), MetaValue.createOther(type, data));
                save = true;
            } else {
                System.err.println("Unsupported metadata type: " + kt[1]);
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

    private static byte[] readStdin(String fileName) throws IOException {
        InputStream fis = null;
        try {
            if (fileName != null) {
                fis = new FileInputStream(new File(fileName));
                return IOUtils.toByteArray(fis);
            } else {
                return IOUtils.toByteArray(Platform.stdin());
            }
        } finally {
            IOUtils.closeQuietly(fis);
        }
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
            String[] lr = value.split("=");
            String[] kt = lr[0].split(",");
            map.put(kt[0], typedValue(lr.length > 1 ? lr[1] : null, kt.length > 1 ? kt[1] : null));
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
