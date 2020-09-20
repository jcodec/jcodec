package org.jcodec.containers.mp4.boxes;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

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

    public static EditListBox createEditListBox(List<Edit> edits) {
        EditListBox elst = new EditListBox(new Header(fourcc()));
        elst.edits = edits;
        return elst;
    }

    public EditListBox(Header atom) {
        super(atom);
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
    
    @Override
    public int estimateSize() {
        return 12 + 4 + edits.size() * 12;
    }

    @AtomField(idx=0)
    public List<Edit> getEdits() {
        return edits;
    }
}
