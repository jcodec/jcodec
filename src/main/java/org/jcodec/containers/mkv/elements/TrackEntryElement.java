package org.jcodec.containers.mkv.elements;

import org.jcodec.containers.mkv.Type;
import org.jcodec.containers.mkv.ebml.Element;
import org.jcodec.containers.mkv.ebml.MasterElement;
import org.jcodec.containers.mkv.ebml.UnsignedIntegerElement;

public class TrackEntryElement extends MasterElement {
    public static enum TrackType {
        VIDEO, AUDIO, COMPLEX, LOGO, SUBTITLE, BUTTON, CONTROL;
    }

    private TrackType type;

    public TrackEntryElement(byte[] type) {
        super(type);
    }

    @Override
    public void addChildElement(Element elem) {
        if (elem == null)
            return;
        if (elem.type.equals(Type.TrackType)) {

            // 0x01 track is a video track
            // 0x02 track is an audio track
            // 0x03 track is a complex track, i.e. a combined video and audio track
            // 0x10 track is a logo track
            // 0x11 track is a subtitle track
            // 0x12 track is a button track
            // 0x20 track is a control track

            UnsignedIntegerElement trackType = (UnsignedIntegerElement) elem;
            int val = (int) trackType.get();
            switch (val) {
            case 0x01:
                type = TrackType.VIDEO;
                break;
            case 0x02:
                type = TrackType.AUDIO;
                break;
            case 0x03:
                type = TrackType.COMPLEX;
                break;
            case 0x10:
                type = TrackType.LOGO;
                break;
            case 0x11:
                type = TrackType.SUBTITLE;
                break;
            case 0x12:
                type = TrackType.BUTTON;
                break;
            case 0x20:
                type = TrackType.CONTROL;
                break;
            }
        } else if (elem.type.equals(Type.ContentCompAlgo)) {
            // 0 zlib
            // 1 bzlib
            // 2 lzo1x
            // 3 header striping
            UnsignedIntegerElement trackType = (UnsignedIntegerElement) elem;
            int val = (int) trackType.get();
            String[] algs = {"zlib", "bzlib", "lzo1x", "headerstripping"};
            System.err.println("Track content compression algorithm: "+ ((0 <= val && val <= 3) ? algs[val] : ""));
        }
        super.addChildElement(elem);
    }

}
