package org.jcodec.common.tools;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jcodec.common.IntArrayList;
import org.jcodec.common.NIOUtils;

/**
 * Simple JSON serializer, introduced because jcodec can not use dependencies as
 * they bring frastration on some platforms
 * 
 * @author Jay Codec
 * 
 */
public class ToJSON {
    static Set<Class> primitive = new HashSet<Class>();
    static Set<String> omitMethods = new HashSet<String>();

    static {
        primitive.add(Byte.class);
        primitive.add(Short.class);
        primitive.add(Integer.class);
        primitive.add(Long.class);
        primitive.add(Float.class);
        primitive.add(Double.class);
        primitive.add(Character.class);
    }

    static {
        omitMethods.add("getClass");
        omitMethods.add("get");
    }

    public static List<String> allFields(Class claz) {
        return allFieldsExcept(claz);
    }

    public static List<String> allFieldsExcept(Class claz, String... except) {
        List<String> result = new ArrayList<String>();
        for (Method method : claz.getDeclaredMethods()) {
            if (!isGetter(method))
                continue;
            try {
                String name = toName(method);
                result.add(name);
            } catch (Exception e) {
            }
        }
        return result;
    }

    /**
     * Converts an object to JSON
     * 
     * @param obj
     * @return
     */
    public static String toJSON(Object obj) {
        StringBuilder builder = new StringBuilder();
        IntArrayList stack = new IntArrayList();
        toJSONSub(obj, stack, builder);
        return builder.toString();
    }

    /**
     * Converts specified fields of an object to JSON
     * 
     * Useful because it doesn't enclose the field list into JSON object
     * brackets "{}" leaving flexibility to append any other information. Makes
     * it possible to specify only selected fields.
     * 
     * @param obj
     * @param builder
     * @param fields
     */
    public static void fieldsToJSON(Object obj, StringBuilder builder, String... fields) {
        Method[] methods = obj.getClass().getMethods();
        for (String field : fields) {
            Method m = findGetter(methods, field);
            if (m == null)
                continue;
            invoke(obj, new IntArrayList(), builder, m, field);
        }
    }

    private static Method findGetter(Method[] methods, String field) {
        String isGetter = getterName("is", field);
        String getGetter = getterName("get", field);
        for (Method method : methods) {
            if (isGetter.equals(method.getName()) || getGetter.equals(method.getName()))
                return method;
        }
        return null;
    }

    private static String getterName(String pref, String field) {
        if (field == null)
            throw new IllegalArgumentException("Passed null string as field name");
        char[] ch = field.toCharArray();
        if (ch.length == 0)
            return pref;
        if (ch.length > 1 && Character.isUpperCase(ch[1]))
            ch[0] = Character.toLowerCase(ch[0]);
        else
            ch[0] = Character.toUpperCase(ch[0]);
        return pref + new String(ch);
    }

    private static void toJSONSub(Object obj, IntArrayList stack, StringBuilder builder) {
        int id = System.identityHashCode(obj);
        if (stack.contains(id)) {
            builder.append("\"!-!-!-!-LOOP-!-!-!-!\"");
            return;
        }
        stack.push(id);

        if (obj instanceof ByteBuffer)
            obj = NIOUtils.toArray((ByteBuffer) obj);

        if (obj == null) {
            builder.append("null");
        } else if (obj instanceof String) {
            builder.append("\"");
            escape((String) obj, builder);
            builder.append("\"");
        } else if (obj instanceof Map) {
            Iterator it = ((Map) obj).entrySet().iterator();
            builder.append("{");
            while (it.hasNext()) {
                Map.Entry e = (Map.Entry) it.next();
                builder.append("\"");
                builder.append(e.getKey());
                builder.append("\":");
                toJSONSub(e.getValue(), stack, builder);
                if (it.hasNext())
                    builder.append(",");
            }
            builder.append("}");
        } else if (obj instanceof Iterable) {
            Iterator it = ((Iterable) obj).iterator();
            builder.append("[");
            while (it.hasNext()) {
                toJSONSub(it.next(), stack, builder);
                if (it.hasNext())
                    builder.append(",");
            }
            builder.append("]");
        } else if (obj instanceof Object[]) {
            builder.append("[");
            int len = Array.getLength(obj);
            for (int i = 0; i < len; i++) {
                toJSONSub(Array.get(obj, i), stack, builder);
                if (i < len - 1)
                    builder.append(",");
            }
            builder.append("]");
        } else if (obj instanceof long[]) {
            long[] a = (long[]) obj;
            builder.append("[");
            for (int i = 0; i < a.length; i++) {
                builder.append(String.format("0x%016x", a[i]));
                if (i < a.length - 1)
                    builder.append(",");
            }
            builder.append("]");
        } else if (obj instanceof int[]) {
            int[] a = (int[]) obj;
            builder.append("[");
            for (int i = 0; i < a.length; i++) {
                builder.append(String.format("0x%08x", a[i]));
                if (i < a.length - 1)
                    builder.append(",");
            }
            builder.append("]");
        } else if (obj instanceof float[]) {
            float[] a = (float[]) obj;
            builder.append("[");
            for (int i = 0; i < a.length; i++) {
                builder.append(String.format(".3f", a[i]));
                if (i < a.length - 1)
                    builder.append(",");
            }
            builder.append("]");
        } else if (obj instanceof double[]) {
            double[] a = (double[]) obj;
            builder.append("[");
            for (int i = 0; i < a.length; i++) {
                builder.append(String.format(".6f", a[i]));
                if (i < a.length - 1)
                    builder.append(",");
            }
            builder.append("]");
        } else if (obj instanceof short[]) {
            short[] a = (short[]) obj;
            builder.append("[");
            for (int i = 0; i < a.length; i++) {
                builder.append(String.format("0x%04x", a[i]));
                if (i < a.length - 1)
                    builder.append(",");
            }
            builder.append("]");
        } else if (obj instanceof byte[]) {
            byte[] a = (byte[]) obj;
            builder.append("[");
            for (int i = 0; i < a.length; i++) {
                builder.append(String.format("0x%02x", a[i]));
                if (i < a.length - 1)
                    builder.append(",");
            }
            builder.append("]");
        } else if (obj instanceof boolean[]) {
            boolean[] a = (boolean[]) obj;
            builder.append("[");
            for (int i = 0; i < a.length; i++) {
                builder.append(a[i]);
                if (i < a.length - 1)
                    builder.append(",");
            }
            builder.append("]");
        } else {
            builder.append("{");
            for (Method method : obj.getClass().getMethods()) {
                if (omitMethods.contains(method.getName()) || !isGetter(method))
                    continue;

                String name = toName(method);
                invoke(obj, stack, builder, method, name);
            }
            builder.append("}");
        }

        stack.pop();
    }

    private static void invoke(Object obj, IntArrayList stack, StringBuilder builder, Method method, String name) {
        try {
            Object invoke = method.invoke(obj);
            builder.append('"');
            builder.append(name);
            builder.append("\":");
            if (invoke != null && primitive.contains(invoke.getClass()))
                builder.append(invoke);
            else
                toJSONSub(invoke, stack, builder);
            builder.append(",");
            // }
        } catch (Exception e) {
        }
    }

    private static void escape(String invoke, StringBuilder sb) {
        char[] ch = invoke.toCharArray();
        for (char c : ch) {
            if (c < 0x20)
                sb.append(String.format("\\%02x", (int) c));
            else
                sb.append(c);
        }
    }

    private static String toName(Method method) {
        if (!isGetter(method))
            throw new IllegalArgumentException("Not a getter");

        char[] name = method.getName().toCharArray();
        int ind = name[0] == 'g' ? 3 : 2;
        name[ind] = Character.toLowerCase(name[ind]);
        return new String(name, ind, name.length - ind);
    }

    public static boolean isGetter(Method method) {
        if (!Modifier.isPublic(method.getModifiers()))
            return false;
        if (!method.getName().startsWith("get")
                && !(method.getName().startsWith("is") && method.getReturnType() == Boolean.TYPE))
            return false;
        if (method.getParameterTypes().length != 0)
            return false;
        if (void.class.equals(method.getReturnType()))
            return false;
        return true;
    }
}
