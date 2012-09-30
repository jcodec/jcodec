package org.jcodec.common.tools;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class ToJSON {
    static Set<Class> good = new HashSet<Class>();

    static {
        good.add(String.class);
        good.add(Byte.class);
        good.add(Short.class);
        good.add(Integer.class);
        good.add(Long.class);
        good.add(Float.class);
        good.add(Double.class);
        good.add(Character.class);
    }

    public static void toJSON(Object obj, StringBuilder builder, String... fields) {
        builder.append("{\n");
        Set<String> fld = new HashSet<String>(Arrays.asList(fields));
        for (Method method : obj.getClass().getMethods()) {
            if (!isGetter(method))
                continue;
            try {
                String name = toName(method);
                if (fields.length > 0 && !fld.contains(name))
                    continue;

                Object invoke = method.invoke(obj);
                if (!invoke.getClass().isPrimitive() && !good.contains(invoke.getClass())
                        && !(invoke instanceof Iterable))
                    continue;
                builder.append(name + ": ");
                value(builder, invoke);
                builder.append(",\n");
            } catch (Exception e) {
            }
        }
        builder.append("}");
    }

    private static void value(StringBuilder builder, Object invoke) {
        if (invoke == null) {
            builder.append("null");
        } else if (invoke == String.class) {
            builder.append("'");
            builder.append((String) invoke);
            builder.append("'");
        } else if (invoke instanceof Iterable) {
            Iterator it = ((Iterable) invoke).iterator();
            builder.append("[");
            while (it.hasNext()) {
                toJSON(it.next(), builder);
                if (it.hasNext())
                    builder.append(",");
            }
            builder.append("]");
        } else {
            builder.append(String.valueOf(invoke));
        }
    }

    private static String toName(Method method) {
        String ss = method.getName().substring(3);
        return ss.substring(0, 1).toLowerCase() + ss.substring(1);
    }

    public static boolean isGetter(Method method) {
        if (!method.getName().startsWith("get"))
            return false;
        if (method.getParameterTypes().length != 0)
            return false;
        if (void.class.equals(method.getReturnType()))
            return false;
        return true;
    }
}
