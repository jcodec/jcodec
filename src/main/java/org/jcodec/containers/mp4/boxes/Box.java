package org.jcodec.containers.mp4.boxes;

import org.jcodec.common.Assert;
import org.jcodec.common.StringUtils;
import org.jcodec.common.UsedViaReflection;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.logging.Logger;
import org.jcodec.common.tools.ToJSON;
import org.jcodec.containers.mp4.IBoxFactory;
import org.jcodec.platform.Platform;
import org.stjs.javascript.Global;

import js.lang.StringBuilder;
import js.lang.reflect.Method;
import js.nio.ByteBuffer;
import js.util.ArrayList;
import js.util.List;

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
    public static final int MAX_BOX_SIZE = 128 * 1024 * 1024;
    
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
        } catch (Exception e) {
            checkWrongSignature(claz);
            model.addAll(ToJSON.allFields(claz));
        }
    }

    private void checkWrongSignature(Class claz) {
        Method[] declaredMethods = Platform.getDeclaredMethods(claz);
        for (int i = 0; i < declaredMethods.length; i++) {
            Method method = declaredMethods[i];
            if (method.getName().equals(GET_MODEL_FIELDS)) {
                Logger.warn("Class " + claz.getCanonicalName() + " contains 'getModelFields' of wrong signature.\n"
                        + "Did you mean to define 'protected void " + GET_MODEL_FIELDS + "(List<String> model) ?");
                break;
            }
        }
    }

    public static String[] path(String path) {
        return StringUtils.splitC(path, '.');
    }

    public static LeafBox createLeafBox(Header atom, ByteBuffer data) {
        LeafBox leaf = new LeafBox(atom);
        leaf.data = data;
        return leaf;
    }

    public static Box parseBox(ByteBuffer input, Header childAtom, IBoxFactory factory) {
        Box box = factory.newBox(childAtom);
    
        if (childAtom.getBodySize() < Box.MAX_BOX_SIZE) {
            box.parse(input);
            return box;
        } else {
            return new LeafBox(Header.createHeader("free", 8));
        }
    }

    public static <T extends Box> T asBox(Class<T> class1, Box box) {
        Global.console.log("asBox",box.header);
        try {
            T res = Platform.newInstance(class1, new Object[]{box.getHeader()});
            ByteBuffer buffer = ByteBuffer.allocate((int)box.getHeader().getBodySize());
            box.doWrite(buffer);
            buffer.flip();
            res.parse(buffer);
            return res;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    public static class LeafBox extends Box {
        ByteBuffer data;

        public LeafBox(Header atom) {
            super(atom);
        }

        public void parse(ByteBuffer input) {
            data = NIOUtils.read(input, (int) header.getBodySize());
        }

        public ByteBuffer getData() {
            return data.duplicate();
        }

        @Override
        protected void doWrite(ByteBuffer out) {
            NIOUtils.write(out, data);
        }
    }
    
}