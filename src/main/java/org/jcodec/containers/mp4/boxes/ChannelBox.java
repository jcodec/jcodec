package org.jcodec.containers.mp4.boxes;

import org.jcodec.common.model.Label;

import java.nio.ByteBuffer;

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
    private ChannelDescription[] descriptions;

    public static class ChannelDescription {
        private int channelLabel;
        private int channelFlags;
        private float[] coordinates;
        
        public ChannelDescription(int channelLabel, int channelFlags, float[] coordinates) {
            this.coordinates = new float[3];
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

    public static String fourcc() {
        return "chan";
    }

    public static ChannelBox createChannelBox() {
        return new ChannelBox(new Header(fourcc()));
    }

    public void parse(ByteBuffer input) {
        super.parse(input);

        channelLayout = input.getInt();
        channelBitmap = input.getInt();
        int numDescriptions = input.getInt();

        descriptions = new ChannelDescription[numDescriptions];
        for (int i = 0; i < numDescriptions; i++) {
            descriptions[i] = new ChannelDescription(input.getInt(), input.getInt(), new float[] {
                    Float.intBitsToFloat(input.getInt()), Float.intBitsToFloat(input.getInt()),
                    Float.intBitsToFloat(input.getInt()) });
        }
    }

    protected void doWrite(ByteBuffer out) {
        super.doWrite(out);
        out.putInt(channelLayout);
        out.putInt(channelBitmap);
        out.putInt(descriptions.length);

        for (int i = 0; i < descriptions.length; i++) {
            ChannelDescription channelDescription = descriptions[i];
            out.putInt(channelDescription.getChannelLabel());
            out.putInt(channelDescription.getChannelFlags());

            out.putFloat(channelDescription.getCoordinates()[0]);
            out.putFloat(channelDescription.getCoordinates()[1]);
            out.putFloat(channelDescription.getCoordinates()[2]);
        }
    }
    
    @Override
    public int estimateSize() {
        return 12 + 12 + descriptions.length * 20;
    }

    public int getChannelLayout() {
        return channelLayout;
    }

    public int getChannelBitmap() {
        return channelBitmap;
    }

    public ChannelDescription[] getDescriptions() {
        return descriptions;
    }

    public void setChannelLayout(int channelLayout) {
        this.channelLayout = channelLayout;
    }

    public void setDescriptions(ChannelDescription[] descriptions) {
        this.descriptions = descriptions;
    }
}