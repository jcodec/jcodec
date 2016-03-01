package org.jcodec.containers.mp4.boxes;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.jcodec.common.Assert;
import org.jcodec.common.UsedViaReflection;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.logging.Logger;
import org.jcodec.common.tools.ToJSON;
import org.jcodec.platform.Platform;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * An MP4 file struncture (box).
 * 
 * @author The JCodec project
 * 
 */
public abstract class Box {
    private static final String GET_MODEL_FIELDS = "getModelFields";
    public Header header;
    
    @UsedViaReflection
    public Box(Header header) {
        this.header = header;
    }

    public Header getHeader() {
        return header;
    }

    public abstract void parse(ByteBuffer buf);

    public void write(ByteBuffer buf) {
        ByteBuffer dup = buf.duplicate();
        NIOUtils.skip(buf, 8);
        doWrite(buf);

        header.setBodySize(buf.position() - dup.position() - 8);
        Assert.assertEquals(header.headerSize(), 8);
        header.write(dup);
    }

    protected abstract void doWrite(ByteBuffer out);

    public String getFourcc() {
        return header.getFourcc();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        dump(sb);
        return sb.toString();

    }

    protected void dump(StringBuilder sb) {
        sb.append("{\"tag\":\"" + header.getFourcc() + "\",");
        List<String> fields = new ArrayList<String>(0);
        collectModel(this.getClass(), fields);
        ToJSON.fieldsToJSON(this, sb, fields.toArray(new String[0]));
        sb.append("}");
    }

    protected void collectModel(Class claz, List<String> model) {
        if (Box.class == claz || !Box.class.isAssignableFrom(claz))
            return;

        collectModel(claz.getSuperclass(), model);

        try {
            Platform.invokeMethod(this, GET_MODEL_FIELDS, new Object[]{model});
        } catch (NoSuchMethodException e) {
            checkWrongSignature(claz);
            model.addAll(ToJSON.allFields(claz));
        } catch (Exception e) {
        }
    }

    private void checkWrongSignature(Class claz) {
        for (Method method : Platform.getDeclaredMethods(claz)) {
            if (method.getName().equals(GET_MODEL_FIELDS)) {
                Logger.warn("Class " + claz.getCanonicalName() + " contains 'getModelFields' of wrong signature.\n"
                        + "Did you mean to define 'protected void " + GET_MODEL_FIELDS + "(List<String> model) ?");
                break;
            }
        }
    }

    public static <T extends Box> T asBox(Class<T> class1, Box box) {
        try {
            T res = class1.getConstructor(Header.class).newInstance(box.getHeader());
            ByteBuffer buffer = ByteBuffer.allocate((int)box.getHeader().getBodySize());
            box.doWrite(buffer);
            buffer.flip();
            res.parse(buffer);
            return res;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    
}