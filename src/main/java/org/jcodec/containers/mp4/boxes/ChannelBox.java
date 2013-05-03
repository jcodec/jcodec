package org.jcodec.containers.mp4.boxes;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

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

    public void parse(ByteBuffer input) {
        super.parse(input);

        channelLayout = input.getInt();
        channelBitmap = input.getInt();
        long numDescriptions = input.getInt();

        for (int i = 0; i < numDescriptions; i++) {
            descriptions.add(new ChannelDescription(input.getInt(), input.getInt(), new float[] {
                    Float.intBitsToFloat(input.getInt()), Float.intBitsToFloat(input.getInt()),
                    Float.intBitsToFloat(input.getInt()) }));
        }
    }

    protected void doWrite(ByteBuffer out) {
        super.doWrite(out);
        out.putInt(channelLayout);
        out.putInt(channelBitmap);
        out.putInt(descriptions.size());

        List<ChannelDescription> descriptions2 = descriptions;
        for (ChannelDescription channelDescription : descriptions2) {
            out.putInt(channelDescription.getChannelLabel());
            out.putInt(channelDescription.getChannelFlags());

            out.putFloat(channelDescription.getCoordinates()[0]);
            out.putFloat(channelDescription.getCoordinates()[1]);
            out.putFloat(channelDescription.getCoordinates()[2]);
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