package org.jcodec.common.tools;

import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jcodec.common.StringUtils;
import org.jcodec.platform.Platform;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class MainUtils {

    private static final String JCODEC_LOG_SINK_COLOR = "jcodec.colorPrint";

    public static boolean isColorSupported = System.console() != null
            || Boolean.parseBoolean(System.getProperty(JCODEC_LOG_SINK_COLOR));

    public static class Cmd {
        public Map<String, String> flags;
        public String[] args;
        private Map<String, String>[] argFlags;

        public Cmd(Map<String, String> flags, String[] args, Map<String, String>[] argFlags) {
            this.flags = flags;
            this.args = args;
            this.argFlags = argFlags;
        }

        private Long getLongFlagInternal(Map<String, String> flags, String flagName, Long defaultValue) {
            return flags.containsKey(flagName) ? new Long(flags.get(flagName)) : defaultValue;
        }

        private Integer getIntegerFlagInternal(Map<String, String> flags, String flagName, Integer defaultValue) {
            return flags.containsKey(flagName) ? new Integer(flags.get(flagName)) : defaultValue;
        }

        private Boolean getBooleanFlagInternal(Map<String, String> flags, String flagName, Boolean defaultValue) {
            return flags.containsKey(flagName) ? !"false".equalsIgnoreCase(flags.get(flagName)) : defaultValue;
        }

        private Double getDoubleFlagInternal(Map<String, String> flags, String flagName, Double defaultValue) {
            return flags.containsKey(flagName) ? new Double(flags.get(flagName)) : defaultValue;
        }

        private String getStringFlagInternal(Map<String, String> flags, String flagName, String defaultValue) {
            return flags.containsKey(flagName) ? flags.get(flagName) : defaultValue;
        }

        private int[] getMultiIntegerFlagInternal(Map<String, String> flags, String flagName, int[] defaultValue) {
            if (!flags.containsKey(flagName))
                return defaultValue;
            String[] split = StringUtils.splitS(flags.get(flagName), ",");
            int[] result = new int[split.length];
            for (int i = 0; i < split.length; i++)
                result[i] = Integer.parseInt(split[i]);
            return result;
        }

        private <T extends Enum<T>> T getEnumFlagInternal(Map<String, String> flags, String flagName, T defaultValue,
                Class<T> class1) {
            if (!flags.containsKey(flagName))
                return defaultValue;

            String strVal = flags.get(flagName).toLowerCase();
            EnumSet<T> allOf = EnumSet.allOf(class1);
            for (T val : allOf) {
                if (val.name().toLowerCase().equals(strVal))
                    return val;
            }
            return null;
        }

        public Long getLongFlagD(String flagName, Long defaultValue) {
            return this.getLongFlagInternal(flags, flagName, defaultValue);
        }

        public Long getLongFlag(String flagName) {
            return this.getLongFlagInternal(flags, flagName, null);
        }

        public Long getLongFlagID(int arg, String flagName, Long defaultValue) {
            return this.getLongFlagInternal(argFlags[arg], flagName, defaultValue);
        }

        public Long getLongFlagI(int arg, String flagName) {
            return this.getLongFlagInternal(argFlags[arg], flagName, null);
        }

        public Integer getIntegerFlagD(String flagName, Integer defaultValue) {
            return getIntegerFlagInternal(flags, flagName, defaultValue);
        }

        public Integer getIntegerFlag(String flagName) {
            return getIntegerFlagInternal(flags, flagName, null);
        }

        public Integer getIntegerFlagID(int arg, String flagName, Integer defaultValue) {
            return getIntegerFlagInternal(argFlags[arg], flagName, defaultValue);
        }

        public Integer getIntegerFlagI(int arg, String flagName) {
            return getIntegerFlagInternal(argFlags[arg], flagName, null);
        }

        public Boolean getBooleanFlagD(String flagName, Boolean defaultValue) {
            return getBooleanFlagInternal(flags, flagName, defaultValue);
        }

        public Boolean getBooleanFlag(String flagName) {
            return getBooleanFlagInternal(flags, flagName, null);
        }

        public Boolean getBooleanFlagID(int arg, String flagName, Boolean defaultValue) {
            return getBooleanFlagInternal(argFlags[arg], flagName, defaultValue);
        }

        public Boolean getBooleanFlagI(int arg, String flagName) {
            return getBooleanFlagInternal(argFlags[arg], flagName, null);
        }

        public Double getDoubleFlagD(String flagName, Double defaultValue) {
            return getDoubleFlagInternal(flags, flagName, defaultValue);
        }

        public Double getDoubleFlag(String flagName) {
            return getDoubleFlagInternal(flags, flagName, null);
        }

        public Double getDoubleFlagID(int arg, String flagName, Double defaultValue) {
            return getDoubleFlagInternal(argFlags[arg], flagName, defaultValue);
        }

        public Double getDoubleFlagI(int arg, String flagName) {
            return getDoubleFlagInternal(argFlags[arg], flagName, null);
        }

        public String getStringFlagD(String flagName, String defaultValue) {
            return getStringFlagInternal(flags, flagName, defaultValue);
        }

        public String getStringFlag(String flagName) {
            return getStringFlagInternal(flags, flagName, null);
        }

        public String getStringFlagID(int arg, String flagName, String defaultValue) {
            return getStringFlagInternal(argFlags[arg], flagName, defaultValue);
        }

        public String getStringFlagI(int arg, String flagName) {
            return getStringFlagInternal(argFlags[arg], flagName, null);
        }

        public int[] getMultiIntegerFlagD(String flagName, int[] defaultValue) {
            return getMultiIntegerFlagInternal(flags, flagName, defaultValue);
        }

        public int[] getMultiIntegerFlag(String flagName) {
            return getMultiIntegerFlagInternal(flags, flagName, new int[0]);
        }

        public int[] getMultiIntegerFlagID(int arg, String flagName, int[] defaultValue) {
            return getMultiIntegerFlagInternal(argFlags[arg], flagName, defaultValue);
        }

        public int[] getMultiIntegerFlagI(int arg, String flagName) {
            return getMultiIntegerFlagInternal(argFlags[arg], flagName, new int[0]);
        }

        public <T extends Enum<T>> T getEnumFlagD(String flagName, T defaultValue, Class<T> class1) {
            return getEnumFlagInternal(flags, flagName, defaultValue, class1);
        }

        public <T extends Enum<T>> T getEnumFlag(String flagName, Class<T> class1) {
            return getEnumFlagInternal(flags, flagName, null, class1);
        }

        public <T extends Enum<T>> T getEnumFlagID(int arg, String flagName, T defaultValue, Class<T> class1) {
            return getEnumFlagInternal(argFlags[arg], flagName, defaultValue, class1);
        }

        public <T extends Enum<T>> T getEnumFlagI(int arg, String flagName, Class<T> class1) {
            return getEnumFlagInternal(argFlags[arg], flagName, null, class1);
        }

        public String getArg(int i) {
            return i < args.length ? args[i] : null;
        }

        public int argsLength() {
            return args.length;
        }

        public void popArg() {
            args = Platform.copyOfRangeO(args, 1, args.length);

        }
    }

    private static Pattern flagPattern = Pattern.compile("^--([^=]+)=(.*)$");

    public static Cmd parseArguments(String[] args) {
        Map<String, String> flags = new HashMap<String, String>();
        Map<String, String> allFlags = new HashMap<String, String>();
        List<String> outArgs = new ArrayList<String>();
        List<Map<String, String>> argFlags = new ArrayList<Map<String, String>>();
        int arg = 0;
        for (; arg < args.length; arg++) {
            if (args[arg].startsWith("--")) {
                Matcher matcher = flagPattern.matcher(args[arg]);
                if (matcher.matches()) {
                    flags.put(matcher.group(1), matcher.group(2));
                } else {
                    flags.put(args[arg].substring(2), "true");
                }
            } else if (args[arg].startsWith("-")) {
                flags.put(args[arg].substring(1), args[++arg]);
            } else {
                allFlags.putAll(flags);
                outArgs.add(args[arg]);
                argFlags.add(flags);
            }
        }

        return new Cmd(allFlags, outArgs.toArray(new String[0]), argFlags.toArray((Map<String, String>[]) Array
                .newInstance(flags.getClass(), 0)));
    }

    public static void printHelp(Map<String, String> flags, String... params) {
        printHelpOut(System.out, "", flags, params);
    }
    
    public static void printHelpCmd(String command, Map<String, String> flags, String... params) {
        printHelpOut(System.out, command, flags, params);
    }

    public static void printHelpOut(PrintStream out, String command, Map<String, String> flags, String... params) {
        out.print(bold("Syntax: " + command));
        StringBuilder sample = new StringBuilder();
        StringBuilder detail = new StringBuilder();
        for (Entry<String, String> entry : flags.entrySet()) {
            sample.append(" [" + bold(color("--" + entry.getKey() + "=<value>", ANSIColor.MAGENTA)) + "]");
            detail.append("\t" + bold(color("--" + entry.getKey(), ANSIColor.MAGENTA)) + "\t\t" + entry.getValue()
                    + "\n");
        }
        for (String param : params) {
            if (param.charAt(0) != '?')
                sample.append(bold(" <" + param + ">"));
            else
                sample.append(bold(" [" + param.substring(1) + "]"));
        }
        out.println(sample);
        out.println(bold("Where:"));
        out.println(detail);
    }

    public enum ANSIColor {
        BLACK, RED, GREEN, BROWN, BLUE, MAGENTA, CYAN, GREY
    }

    public static String bold(String str) {
        return isColorSupported ? "\033[1m" + str + "\033[0m" : str;
    }

    public static String colorString(String str, String placeholder) {
        return isColorSupported ? "\033[" + placeholder + "m" + str + "\033[0m" : str;
    }

    public static String color(String str, ANSIColor fg) {
        return isColorSupported ? "\033[" + (30 + (fg.ordinal() & 0x7)) + "m" + str + "\033[0m" : str;
    }

    public static String colorBright(String str, ANSIColor fg, boolean bright) {
        return isColorSupported ? "\033[" + (30 + (fg.ordinal() & 0x7)) + ";" + (bright ? 1 : 2) + "m" + str
                + "\033[0m" : str;
    }

    public static String color3(String str, ANSIColor fg, ANSIColor bg) {
        return isColorSupported ? "\033[" + (30 + (fg.ordinal() & 0x7)) + ";" + (40 + (bg.ordinal() & 0x7)) + ";1m"
                + str + "\033[0m" : str;
    }

    public static String color4(String str, ANSIColor fg, ANSIColor bg, boolean bright) {
        return isColorSupported ? "\033[" + (30 + (fg.ordinal() & 0x7)) + ";" + (40 + (bg.ordinal() & 0x7)) + ";"
                + (bright ? 1 : 2) + "m" + str + "\033[0m" : str;
    }

    public static File tildeExpand(String path) {
        if (path.startsWith("~")) {
            path = path.replaceFirst("~", System.getProperty("user.home"));
        }
        return new File(path);
    }
}