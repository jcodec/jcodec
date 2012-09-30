package org.jcodec.containers.mp4.boxes;

import java.io.DataOutput;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.jcodec.common.io.ReaderBE;
import org.jcodec.containers.mp4.boxes.channel.Label;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class ChannelBox extends FullBox {
    private int channelLayout;
    private int channelBitmap;
    private List<ChannelDescription> descriptions = new ArrayList<ChannelDescription>();

    public static class ChannelDescription {
        private int channelLabel;
        private int channelFlags;
        private float[] coordinates = new float[3];

        public ChannelDescription(int channelLabel, int channelFlags, float[] coordinates) {
            this.channelLabel = channelLabel;
            this.channelFlags = channelFlags;
            this.coordinates = coordinates;
        }

        public int getChannelLabel() {
            return channelLabel;
        }

        public int getChannelFlags() {
            return channelFlags;
        }

        public float[] getCoordinates() {
            return coordinates;
        }
        
        public Label getLabel() {
            return Label.getByVal(channelLabel);
        }
    }

    public ChannelBox(Header atom) {
        super(atom);
    }

    public ChannelBox() {
        super(new Header(fourcc()));
    }

    public static String fourcc() {
        return "chan";
    }

    public void parse(InputStream input) throws IOException {
        super.parse(input);

        channelLayout = (int) ReaderBE.readInt32(input);
        channelBitmap = (int) ReaderBE.readInt32(input);
        long numDescriptions = ReaderBE.readInt32(input);

        for (int i = 0; i < numDescriptions; i++) {
            descriptions.add(new ChannelDescription((int) ReaderBE.readInt32(input), (int) ReaderBE.readInt32(input),
                    new float[] { Float.intBitsToFloat((int) ReaderBE.readInt32(input)),
                            Float.intBitsToFloat((int) ReaderBE.readInt32(input)),
                            Float.intBitsToFloat((int) ReaderBE.readInt32(input)) }));
        }
    }

    protected void doWrite(DataOutput out) throws IOException {
        super.doWrite(out);
        out.writeInt(channelLayout);
        out.writeInt(channelBitmap);
        out.writeInt(descriptions.size());

        List<ChannelDescription> descriptions2 = descriptions;
        for (ChannelDescription channelDescription : descriptions2) {
            out.writeInt(channelDescription.getChannelLabel());
            out.writeInt(channelDescription.getChannelFlags());

            out.writeFloat(channelDescription.getCoordinates()[0]);
            out.writeFloat(channelDescription.getCoordinates()[1]);
            out.writeFloat(channelDescription.getCoordinates()[2]);
        }
    }

    public int getChannelLayout() {
        return channelLayout;
    }

    public int getChannelBitmap() {
        return channelBitmap;
    }

    public List<ChannelDescription> getDescriptions() {
        return descriptions;
    }

    public void setChannelLayout(int channelLayout) {
        this.channelLayout = channelLayout;
    }

    public void setDescriptions(List<ChannelDescription> descriptions) {
        this.descriptions = descriptions;
    }
}