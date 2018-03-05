package org.jcodec.common.tools;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jcodec.common.StringUtils;
import org.jcodec.common.io.IOUtils;
import org.jcodec.platform.Platform;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class MainUtils {

    private static final String KEY_GIT_REVISION = "git.commit.id.abbrev";
    private static final String JCODEC_LOG_SINK_COLOR = "jcodec.colorPrint";
    private static final String GIT_PROPERTIES = "git.properties";

    public static boolean isColorSupported = System.console() != null
            || Boolean.parseBoolean(System.getProperty(JCODEC_LOG_SINK_COLOR));

    public static enum FlagType {
        VOID, STRING, INT, LONG, DOUBLE, MULT, ENUM, ANY
    }

    public static class Flag {
        private String longName;
        private String shortName;
        private String description;
        private FlagType type;

        public Flag(String longName, String shortName, String description, FlagType type) {
            this.longName = longName;
            this.shortName = shortName;
            this.description = description;
            this.type = type;
        }

        public static Flag flag(String longName, String shortName, String description) {
            return new Flag(longName, shortName, description, FlagType.ANY);
        }

        public String getLongName() {
            return longName;
        }

        public String getDescription() {
            return description;
        }

        public String getShortName() {
            return shortName;
        }

        public FlagType getType() {
            return type;
        }
    }

    public static class Cmd {
        public Map<String, String> longFlags;
        public Map<String, String> shortFlags;
        public String[] args;
        private Map<String, String>[] longArgFlags;
        private Map<String, String>[] shortArgFlags;

        public Cmd(Map<String, String> longFlags, Map<String, String> shortFlags, String[] args,
                Map<String, String>[] longArgFlags, Map<String, String>[] shortArgFlags) {
            this.args = args;
            this.longFlags = longFlags;
            this.shortFlags = shortFlags;
            this.longArgFlags = longArgFlags;
            this.shortArgFlags = shortArgFlags;
        }

        private Long getLongFlagInternal(Map<String, String> longFlags, Map<String, String> shortFlags, Flag flag,
                Long defaultValue) {
            return longFlags.containsKey(flag.getLongName()) ? new Long(longFlags.get(flag.getLongName()))
                    : (shortFlags.containsKey(flag.getShortName()) ? new Long(shortFlags.get(flag.getShortName()))
                            : defaultValue);
        }

        private Integer getIntegerFlagInternal(Map<String, String> longFlags, Map<String, String> shortFlags, Flag flag,
                Integer defaultValue) {
            return longFlags.containsKey(flag.getLongName()) ? new Integer(longFlags.get(flag.getLongName()))
                    : (shortFlags.containsKey(flag.getShortName()) ? new Integer(shortFlags.get(flag.getShortName()))
                            : defaultValue);
        }

        private Boolean getBooleanFlagInternal(Map<String, String> longFlags, Map<String, String> shortFlags, Flag flag,
                Boolean defaultValue) {
            return longFlags.containsKey(flag.getLongName())
                    ? !"false".equalsIgnoreCase(longFlags.get(flag.getLongName()))
                    : (shortFlags.containsKey(flag.getShortName())
                            ? !"false".equalsIgnoreCase(shortFlags.get(flag.getShortName())) : defaultValue);
        }

        private Double getDoubleFlagInternal(Map<String, String> longFlags, Map<String, String> shortFlags, Flag flag,
                Double defaultValue) {
            return longFlags.containsKey(flag.getLongName()) ? new Double(longFlags.get(flag.getLongName()))
                    : (shortFlags.containsKey(flag.getShortName()) ? new Double(shortFlags.get(flag.getShortName()))
                            : defaultValue);
        }

        private String getStringFlagInternal(Map<String, String> longFlags, Map<String, String> shortFlags, Flag flag,
                String defaultValue) {
            return longFlags.containsKey(flag.getLongName()) ? longFlags.get(flag.getLongName())
                    : (shortFlags.containsKey(flag.getShortName()) ? shortFlags.get(flag.getShortName())
                            : defaultValue);
        }

        private int[] getMultiIntegerFlagInternal(Map<String, String> longFlags, Map<String, String> shortFlags,
                Flag flag, int[] defaultValue) {
            String flagValue;
            if (longFlags.containsKey(flag.getLongName()))
                flagValue = longFlags.get(flag.getLongName());
            else if (shortFlags.containsKey(flag.getShortName()))
                flagValue = shortFlags.get(flag.getShortName());
            else
                return defaultValue;
            String[] split = StringUtils.splitS(flagValue, ",");
            int[] result = new int[split.length];
            for (int i = 0; i < split.length; i++)
                result[i] = Integer.parseInt(split[i]);
            return result;
        }

        private <T extends Enum<T>> T getEnumFlagInternal(Map<String, String> longFlags, Map<String, String> shortFlags,
                Flag flag, T defaultValue, Class<T> class1) {
            String flagValue;
            if (longFlags.containsKey(flag.getLongName()))
                flagValue = longFlags.get(flag.getLongName());
            else if (shortFlags.containsKey(flag.getShortName()))
                flagValue = shortFlags.get(flag.getShortName());
            else
                return defaultValue;

            String strVal = flagValue.toLowerCase();
            EnumSet<T> allOf = EnumSet.allOf(class1);
            for (T val : allOf) {
                if (val.name().toLowerCase().equals(strVal))
                    return val;
            }
            return null;
        }

        public Long getLongFlagD(Flag flagName, Long defaultValue) {
            return this.getLongFlagInternal(longFlags, shortFlags, flagName, defaultValue);
        }

        public Long getLongFlag(Flag flagName) {
            return this.getLongFlagInternal(longFlags, shortFlags, flagName, null);
        }

        public Long getLongFlagID(int arg, Flag flagName, Long defaultValue) {
            return this.getLongFlagInternal(longArgFlags[arg], shortArgFlags[arg], flagName, defaultValue);
        }

        public Long getLongFlagI(int arg, Flag flagName) {
            return this.getLongFlagInternal(longArgFlags[arg], shortArgFlags[arg], flagName, null);
        }

        public Integer getIntegerFlagD(Flag flagName, Integer defaultValue) {
            return getIntegerFlagInternal(longFlags, shortFlags, flagName, defaultValue);
        }

        public Integer getIntegerFlag(Flag flagName) {
            return getIntegerFlagInternal(longFlags, shortFlags, flagName, null);
        }

        public Integer getIntegerFlagID(int arg, Flag flagName, Integer defaultValue) {
            return getIntegerFlagInternal(longArgFlags[arg], shortArgFlags[arg], flagName, defaultValue);
        }

        public Integer getIntegerFlagI(int arg, Flag flagName) {
            return getIntegerFlagInternal(longArgFlags[arg], shortArgFlags[arg], flagName, null);
        }

        public Boolean getBooleanFlagD(Flag flagName, Boolean defaultValue) {
            return getBooleanFlagInternal(longFlags, shortFlags, flagName, defaultValue);
        }

        public Boolean getBooleanFlag(Flag flagName) {
            return getBooleanFlagInternal(longFlags, shortFlags, flagName, false);
        }

        public Boolean getBooleanFlagID(int arg, Flag flagName, Boolean defaultValue) {
            return getBooleanFlagInternal(longArgFlags[arg], shortArgFlags[arg], flagName, defaultValue);
        }

        public Boolean getBooleanFlagI(int arg, Flag flagName) {
            return getBooleanFlagInternal(longArgFlags[arg], shortArgFlags[arg], flagName, false);
        }

        public Double getDoubleFlagD(Flag flagName, Double defaultValue) {
            return getDoubleFlagInternal(longFlags, shortFlags, flagName, defaultValue);
        }

        public Double getDoubleFlag(Flag flagName) {
            return getDoubleFlagInternal(longFlags, shortFlags, flagName, null);
        }

        public Double getDoubleFlagID(int arg, Flag flagName, Double defaultValue) {
            return getDoubleFlagInternal(longArgFlags[arg], shortArgFlags[arg], flagName, defaultValue);
        }

        public Double getDoubleFlagI(int arg, Flag flagName) {
            return getDoubleFlagInternal(longArgFlags[arg], shortArgFlags[arg], flagName, null);
        }

        public String getStringFlagD(Flag flagName, String defaultValue) {
            return getStringFlagInternal(longFlags, shortFlags, flagName, defaultValue);
        }

        public String getStringFlag(Flag flagName) {
            return getStringFlagInternal(longFlags, shortFlags, flagName, null);
        }

        public String getStringFlagID(int arg, Flag flagName, String defaultValue) {
            return getStringFlagInternal(longArgFlags[arg], shortArgFlags[arg], flagName, defaultValue);
        }

        public String getStringFlagI(int arg, Flag flagName) {
            return getStringFlagInternal(longArgFlags[arg], shortArgFlags[arg], flagName, null);
        }

        public int[] getMultiIntegerFlagD(Flag flagName, int[] defaultValue) {
            return getMultiIntegerFlagInternal(longFlags, shortFlags, flagName, defaultValue);
        }

        public int[] getMultiIntegerFlag(Flag flagName) {
            return getMultiIntegerFlagInternal(longFlags, shortFlags, flagName, new int[0]);
        }

        public int[] getMultiIntegerFlagID(int arg, Flag flagName, int[] defaultValue) {
            return getMultiIntegerFlagInternal(longArgFlags[arg], shortArgFlags[arg], flagName, defaultValue);
        }

        public int[] getMultiIntegerFlagI(int arg, Flag flagName) {
            return getMultiIntegerFlagInternal(longArgFlags[arg], shortArgFlags[arg], flagName, new int[0]);
        }

        public <T extends Enum<T>> T getEnumFlagD(Flag flagName, T defaultValue, Class<T> class1) {
            return getEnumFlagInternal(longFlags, shortFlags, flagName, defaultValue, class1);
        }

        public <T extends Enum<T>> T getEnumFlag(Flag flagName, Class<T> class1) {
            return getEnumFlagInternal(longFlags, shortFlags, flagName, null, class1);
        }

        public <T extends Enum<T>> T getEnumFlagID(int arg, Flag flagName, T defaultValue, Class<T> class1) {
            return getEnumFlagInternal(longArgFlags[arg], shortArgFlags[arg], flagName, defaultValue, class1);
        }

        public <T extends Enum<T>> T getEnumFlagI(int arg, Flag flagName, Class<T> class1) {
            return getEnumFlagInternal(longArgFlags[arg], shortArgFlags[arg], flagName, null, class1);
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

    public static Cmd parseArguments(String[] args, Flag[] flags) {
        Map<String, String> longFlags = new HashMap<String, String>();
        Map<String, String> shortFlags = new HashMap<String, String>();
        Map<String, String> allLongFlags = new HashMap<String, String>();
        Map<String, String> allShortFlags = new HashMap<String, String>();
        List<String> outArgs = new ArrayList<String>();
        List<Map<String, String>> argLongFlags = new ArrayList<Map<String, String>>();
        List<Map<String, String>> argShortFlags = new ArrayList<Map<String, String>>();
        int arg = 0;
        for (; arg < args.length; arg++) {
            if (args[arg].startsWith("--")) {
                Matcher matcher = flagPattern.matcher(args[arg]);
                if (matcher.matches()) {
                    longFlags.put(matcher.group(1), matcher.group(2));
                } else {
                    longFlags.put(args[arg].substring(2), "true");
                }
            } else if (args[arg].startsWith("-")) {
                String shortName = args[arg].substring(1);
                boolean found = false;
                for (Flag flag : flags) {
                    if (shortName.equals(flag.getShortName())) {
                        found = true;
                        if (flag.getType() != FlagType.VOID)
                            shortFlags.put(shortName, args[++arg]);
                        else
                            shortFlags.put(shortName, "true");
                    }
                }
                if (!found)
                    ++arg;
            } else {
                allLongFlags.putAll(longFlags);
                allShortFlags.putAll(shortFlags);
                outArgs.add(args[arg]);
                argLongFlags.add(longFlags);
                argShortFlags.add(shortFlags);
                longFlags = new HashMap<String, String>();
                shortFlags = new HashMap<String, String>();
            }
        }

        return new Cmd(allLongFlags, allShortFlags, outArgs.toArray(new String[0]),
                argLongFlags.toArray((Map<String, String>[]) Array.newInstance(longFlags.getClass(), 0)),
                argShortFlags.toArray((Map<String, String>[]) Array.newInstance(shortFlags.getClass(), 0)));
    }

    public static void printHelpArgs(Flag[] flags, String[] arguments) {
        printHelpOut(System.out, "", flags, Arrays.asList(arguments));
    }

    public static void printHelp(Flag[] flags, List<String> params) {
        printHelpOut(System.out, "", flags, params);
    }

    public static void printHelpNoFlags(String... arguments) {
        printHelpOut(System.out, "", new Flag[] {}, Arrays.asList(arguments));
    }

    public static void printHelpCmdVa(String command, Flag[] flags, String arguments) {
        printHelpOut(System.out, command, flags, Collections.singletonList(arguments));
    }
    
    public static void printHelpCmd(String command, Flag[] flags, List<String> params) {
        printHelpOut(System.out, command, flags, params);
    }
    
    private static String getGitRevision() {
        InputStream is = null;
        try {
            is = Thread.currentThread().getContextClassLoader().getResourceAsStream(GIT_PROPERTIES);
            if (is == null)
                return null;
            Properties properties = new Properties();
            properties.load(is);
            return (String) properties.get(KEY_GIT_REVISION);
        } catch (IOException e) {
        } finally {
            IOUtils.closeQuietly(is);
        }
        return null;
    }

    public static void printHelpOut(PrintStream out, String command, Flag[] flags, List<String> params) {
        String version = MainUtils.class.getPackage().getImplementationVersion();
        String gitRevision = getGitRevision();
        if (command == null || command.isEmpty())
            command = "jcodec";
        if (gitRevision != null || version != null) {
            out.println(command + bold((version != null ? " v." + version : "")
                    + (gitRevision != null ? " rev. " + gitRevision : "")));
            out.println();
        }
        out.print(bold("Syntax: " + command));
        StringBuilder sample = new StringBuilder();
        StringBuilder detail = new StringBuilder();
        for (Flag flag : flags) {
            sample.append(" [");
            detail.append("\t");
            if (flag.getLongName() != null) {
                sample.append(bold(color("--" + flag.getLongName() + "=<value>", ANSIColor.MAGENTA)));
                detail.append(bold(color("--" + flag.getLongName(), ANSIColor.MAGENTA)));
            }
            if (flag.getShortName() != null) {
                if (flag.getLongName() != null) {
                    sample.append(" (");
                    detail.append(" (");
                }
                sample.append(bold(color("-" + flag.getShortName() + " <value>", ANSIColor.MAGENTA)));
                detail.append(bold(color("-" + flag.getShortName(), ANSIColor.MAGENTA)));
                if (flag.getLongName() != null) {
                    sample.append(")");
                    detail.append(")");
                }
            }
            sample.append("]");
            detail.append("\t\t" + flag.getDescription() + "\n");
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
        return isColorSupported ? "\033[" + (30 + (fg.ordinal() & 0x7)) + ";" + (bright ? 1 : 2) + "m" + str + "\033[0m"
                : str;
    }

    public static String color3(String str, ANSIColor fg, ANSIColor bg) {
        return isColorSupported
                ? "\033[" + (30 + (fg.ordinal() & 0x7)) + ";" + (40 + (bg.ordinal() & 0x7)) + ";1m" + str + "\033[0m"
                : str;
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