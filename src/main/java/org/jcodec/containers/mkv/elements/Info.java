package org.jcodec.containers.mkv.elements;

import java.util.Date;

import org.jcodec.containers.mkv.Type;
import org.jcodec.containers.mkv.ebml.DateElement;
import org.jcodec.containers.mkv.ebml.Element;
import org.jcodec.containers.mkv.ebml.FloatElement;
import org.jcodec.containers.mkv.ebml.MasterElement;
import org.jcodec.containers.mkv.ebml.StringElement;
import org.jcodec.containers.mkv.ebml.UnsignedIntegerElement;

public class Info extends MasterElement {

    private String segmentTitle;
    private Date segmentDate;
    private String muxingApp;
    private String writingApp;
    private double duration;
    private long timecodeScale;

    public Info(byte[] type) {
        super(type);
    }

    @Override
    public void addChildElement(Element elem) {
        if (elem.isSameMatroskaType(Type.Title)) {
            segmentTitle = ((StringElement) elem).get();

        } else if (elem.isSameMatroskaType(Type.DateUTC)) {
            segmentDate = ((DateElement) elem).getDate();

        } else if (elem.isSameMatroskaType(Type.MuxingApp)) {
            muxingApp = ((StringElement) elem).get();

        } else if (elem.isSameMatroskaType(Type.WritingApp)) {
            writingApp = ((StringElement) elem).get();

        } else if (elem.isSameMatroskaType(Type.Duration)) {
            duration = ((FloatElement) elem).get();

        } else if (elem.isSameMatroskaType(Type.TimecodeScale)) {
            timecodeScale = ((UnsignedIntegerElement) elem).get();
        }

        super.addChildElement(elem);
    }

    public String getSegmentTitle() {
        return segmentTitle;
    }

    public Date getSegmentDate() {
        return segmentDate;
    }

    public String getMuxingApp() {
        return muxingApp;
    }

    public String getWritingApp() {
        return writingApp;
    }

    public double getDuration() {
        return duration;
    }

    public long getTimecodeScale() {
        return timecodeScale;
    }
}
