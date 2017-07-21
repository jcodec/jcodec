package org.jcodec.containers.mp4.boxes;

import org.jcodec.common.Assert;
import org.jcodec.common.StringUtils;
import org.jcodec.common.UsedViaReflection;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.logging.Logger;
import org.jcodec.common.tools.ToJSON;
import org.jcodec.containers.mp4.IBoxFactory;
import org.jcodec.platform.Platform;

import java.lang.StringBuilder;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

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
        ToJSON.fieldsToJSON(this, sb, fields.toArray(new String[0]));
        sb.append("}");
    }

    public static Box terminatorAtom() {
        return createLeafBox(new Header(Platform.stringFromBytes(new byte[4])), ByteBuffer.allocate(0));
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