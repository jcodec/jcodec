package org.jcodec.containers.mp4.boxes;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.jcodec.common.tools.ToJSON;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class EditListBox extends FullBox {
    private List<Edit> edits;

    public static String fourcc() {
        return "elst";
    }

    public EditListBox(Header atom) {
        super(atom);
    }

    public EditListBox() {
        this(new Header(fourcc()));
    }

    public EditListBox(List<Edit> edits) {
        this();
        this.edits = edits;
    }

    public void parse(ByteBuffer input) {
        super.parse(input);

        edits = new ArrayList<Edit>();
        long num = input.getInt();
        for (int i = 0; i < num; i++) {
            int duration = input.getInt();
            int mediaTime = input.getInt();
            float rate = input.getInt() / 65536f;
            edits.add(new Edit(duration, mediaTime, rate));
        }
    }

    protected void doWrite(ByteBuffer out) {
        super.doWrite(out);

        out.putInt(edits.size());
        for (Edit edit : edits) {
            out.putInt((int) edit.getDuration());
            out.putInt((int) edit.getMediaTime());
            out.putInt((int) (edit.getRate() * 65536));
        }
    }

    public List<Edit> getEdits() {
        return edits;
    }

    protected void dump(StringBuilder sb) {
        super.dump(sb);
        sb.append(": ");
        ToJSON.toJSON(this, sb, "edits");
    }
}
