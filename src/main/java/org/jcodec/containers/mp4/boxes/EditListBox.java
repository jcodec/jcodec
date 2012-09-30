package org.jcodec.containers.mp4.boxes;

import java.io.DataOutput;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.jcodec.common.io.ReaderBE;
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

    public void parse(InputStream input) throws IOException {
        super.parse(input);

        edits = new ArrayList<Edit>();
        long num = ReaderBE.readInt32(input);
        for (int i = 0; i < num; i++) {
            int duration = (int) ReaderBE.readInt32(input);
            int mediaTime = (int) ReaderBE.readInt32(input);
            float rate = ReaderBE.readInt32(input) / 65536f;
            edits.add(new Edit(duration, mediaTime, rate));
        }
    }

    protected void doWrite(DataOutput out) throws IOException {
        super.doWrite(out);

        out.writeInt(edits.size());
        for (Edit edit : edits) {
            out.writeInt((int) edit.getDuration());
            out.writeInt((int) edit.getMediaTime());
            out.writeInt((int) (edit.getRate() * 65536));
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
