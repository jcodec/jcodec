package org.jcodec.containers.mp4.boxes;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.jcodec.common.tools.ToJSON;
import org.jcodec.containers.mp4.BoxUtil;
import org.jcodec.containers.mp4.IBoxFactory;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * A node box
 * 
 * A box containing children, no data
 * 
 * @author The JCodec project
 * 
 */
public class NodeBox extends Box {
    public static final int MAX_BOX_SIZE = 128 * 1024 * 1024;
    protected List<Box> boxes;
    protected IBoxFactory factory;

    public NodeBox(Header atom) {
        super(atom);
        this.boxes = new LinkedList<Box>();
    }

    public void setFactory(IBoxFactory factory) {
        this.factory = factory;
    }
    
    public void parse(ByteBuffer input) {

        while (input.remaining() >= 8) {
            Box child = BoxUtil.parseChildBox(input, factory);
            if (child != null)
                boxes.add(child);
        }
    }

    public List<Box> getBoxes() {
        return boxes;
    }

    public void add(Box box) {
        boxes.add(box);
    }

    protected void doWrite(ByteBuffer out) {
        for (Box box : boxes) {
            box.write(out);
        }
    }

    public void addFirst(MovieHeaderBox box) {
        boxes.add(0, box);
    }

    public void replace(String fourcc, Box box) {
        removeChildren(fourcc);
        add(box);
    }
    
    public void replaceBox(Box box) {
        removeChildren(box.getFourcc());
        add(box);
    }

    protected void dump(StringBuilder sb) {
        sb.append("{\"tag\":\"" + header.getFourcc() + "\",");
        List<String> fields = new ArrayList<String>(0);
        collectModel(this.getClass(), fields);
        ToJSON.fieldsToJSON(this, sb, fields.toArray(new String[0]));
        sb.append("\"boxes\": [");
        dumpBoxes(sb);
        sb.append("]");
        sb.append("}");
    }

    protected void getModelFields(List<String> model) {

    }

    protected void dumpBoxes(StringBuilder sb) {
        for (int i = 0; i < boxes.size(); i++) {
            boxes.get(i).dump(sb);
            if (i < boxes.size() - 1)
                sb.append(",");
        }
    }

    public void removeChildren(String... arguments) {
        for (Iterator<Box> it = boxes.iterator(); it.hasNext();) {
            Box box = it.next();
            String fcc = box.getFourcc();
            for (String cand : arguments) {
                if (cand.equals(fcc)) {
                    it.remove();
                    break;
                }
            }
        }
    }
}