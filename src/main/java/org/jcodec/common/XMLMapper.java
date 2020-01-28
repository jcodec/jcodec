package org.jcodec.common;

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jcodec.common.model.Rational;

public class XMLMapper {

    public static interface TypeHandler {
        Object parse(String value, Class<?> clz);

        boolean supports(Class<?> clz);
    }

    /**
     * Parses XML snippet into the object tree
     * 
     * @throws XMLStreamException
     * @throws ReflectiveOperationException
     */
    public static <T> T map(InputStream is, Class<T> clz, TypeHandler th)
            throws ReflectiveOperationException, XMLStreamException {
        XMLInputFactory xmlInFact = XMLInputFactory.newInstance();
        XMLStreamReader reader = xmlInFact.createXMLStreamReader(is);
        while (!reader.isStartElement())
            reader.next();
        return parseInternal(reader, clz, th);
    }

    public static <T> T parseInternal(XMLStreamReader reader, Class<T> clz, TypeHandler th)
            throws ReflectiveOperationException, XMLStreamException {
        Constructor<T> constructor = (Constructor<T>)clz.getDeclaredConstructors()[0];
        constructor.setAccessible(true);
        T instance = constructor.newInstance();
        Field[] fields = clz.getDeclaredFields();
        Method[] methods = clz.getDeclaredMethods();

        int level = 0;
        int suspend = 0;
        while (reader.hasNext()) {
            if (level != 0)
                reader.next();

            if (reader.isStartElement()) {
                System.out.println("<" + reader.getLocalName() + ">");
                if (level == 0) {
                    for (int i = 0; i < reader.getAttributeCount(); i++) {
                        Field found = findSettableField(reader.getAttributeLocalName(i), fields, th);
                        if (found != null) {
                            found.setAccessible(true);
                            found.set(instance, toType(reader.getAttributeValue(i), found.getType(), th));
                        }
                    }
                } else if (suspend == 0 || level <= suspend) {
                    Field field = findField(reader.getLocalName(), fields);
                    if (field != null) {
                        field.setAccessible(true);
                        Class<?> type = field.getType();
                        Object parsed;
                        if (isParsableType(th, field)) {
                            String str = getStringContent(reader);
                            parsed = toType(str, type, th);
                        } else {
                            parsed = parseInternal(reader, type, th);
                        }
                        field.set(instance, parsed);
                    }
                    Method found = findMethod(reader.getLocalName(), methods);
                    if (found != null) {
                        found.setAccessible(true);
                        Object parsed = parseInternal(reader, found.getParameterTypes()[0], th);
                        found.invoke(instance, parsed);
                    } else {
                        suspend = level;
                    }
                }
                level++;
            }
            if (reader.isEndElement()) {
                System.out.println("</" + reader.getLocalName() + ">");
                --level;
                if (level == 0)
                    break;
                if (level == suspend)
                    suspend = 0;
            }
        }
        return instance;
    }

    private static String getStringContent(XMLStreamReader reader) throws XMLStreamException {
        int level = 0;
        StringBuilder charContent = new StringBuilder();
        while (reader.hasNext()) {
            if (level != 0)
                reader.next();

            if (reader.isCharacters())
                charContent.append(reader.getText());
            if (reader.isStartElement()) {
                level++;
            }
            if (reader.isEndElement()) {
                --level;
                if (level == 0)
                    break;
            }
        }
        return charContent.toString();
    }

    private static Object toType(String attributeValue, Class<?> type, TypeHandler th) {
        if (type == String.class) {
            return attributeValue;
        } else if (type == int.class) {
            return Integer.parseInt(attributeValue);
        } else if (type == long.class) {
            return Long.parseLong(attributeValue);
        } else if (type == double.class) {
            return Double.parseDouble(attributeValue);
        } else if (type == float.class) {
            return Float.parseFloat(attributeValue);
        } else if (type == short.class) {
            return Short.parseShort(attributeValue);
        } else if (type == boolean.class) {
            return Boolean.parseBoolean(attributeValue);
        } else if (type == Rational.class) {
            return Rational.parseRational(attributeValue);
        }
        return th == null ? null : th.parse(attributeValue, type);
    }

    private static Method findMethod(String localName, Method[] methods) {
        for (Method method : methods) {
            if (method.getName().equals("add" + localName)) {
                return method;
            }
        }
        return null;
    }

    private static Field findField(String localName, Field[] fields) {
        for (Field field : fields) {
            if (xml(field.getName()).equals(localName)) {
                return field;
            }
        }
        return null;
    }

    private static String xml(String arg) {
        char[] charArray = arg.toCharArray();
        if (charArray.length > 0)
            charArray[0] = Character.toUpperCase(charArray[0]);
        return new String(charArray);
    }

    private static Field findSettableField(String localName, Field[] fields, TypeHandler th) {
        for (Field field : fields) {
            if (isParsableType(th, field) && field.getName().equals(localName)) {
                return field;
            }
        }
        return null;
    }

    private static boolean isParsableType(TypeHandler th, Field field) {
        return field.getType() == String.class || field.getType().isPrimitive() || field.getType() == Rational.class
                || (th != null && th.supports(field.getType()));
    }
}