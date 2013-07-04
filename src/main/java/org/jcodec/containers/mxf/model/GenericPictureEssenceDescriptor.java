package org.jcodec.containers.mxf.model;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.jcodec.common.model.Rational;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class GenericPictureEssenceDescriptor extends FileDescriptor {

    public static enum LayoutType {
        FullFrame, SeparateFields, OneField, MixedFields, SegmentedFrame
    }

    private byte signalStandard;
    private LayoutType frameLayout;
    private int storedWidth;
    private int storedHeight;
    private int storedF2Offset;
    private int sampledWidth;
    private int sampledHeight;
    private int sampledXOffset;
    private int sampledYOffset;
    private int displayHeight;
    private int displayWidth;
    private int displayXOffset;
    private int displayYOffset;
    private int displayF2Offset;
    private Rational aspectRatio;
    private byte activeFormatDescriptor;
    private int[] videoLineMap;
    private byte alphaTransparency;
    private UL transferCharacteristic;
    private int imageAlignmentOffset;
    private int imageStartOffset;
    private int imageEndOffset;
    private byte fieldDominance;
    private UL pictureEssenceCoding;
    private UL codingEquations;
    private UL colorPrimaries;

    public GenericPictureEssenceDescriptor(UL ul) {
        super(ul);
    }

    protected void read(Map<Integer, ByteBuffer> tags) {
        super.read(tags);

        for (Iterator<Entry<Integer, ByteBuffer>> it = tags.entrySet().iterator(); it.hasNext();) {
            Entry<Integer, ByteBuffer> entry = it.next();

            ByteBuffer _bb = entry.getValue();

            switch (entry.getKey()) {
            case 0x3215:
                signalStandard = _bb.get();
                break;
            case 0x320c:
                frameLayout = LayoutType.values()[_bb.get()];
                break;
            case 0x3203:
                storedWidth = _bb.getInt();
                break;
            case 0x3202:
                storedHeight = _bb.getInt();
                break;
            case 0x3216:
                storedF2Offset = _bb.getInt();
                break;
            case 0x3205:
                sampledWidth = _bb.getInt();
                break;
            case 0x3204:
                sampledHeight = _bb.getInt();
                break;
            case 0x3206:
                sampledXOffset = _bb.getInt();
                break;
            case 0x3207:
                sampledYOffset = _bb.getInt();
                break;
            case 0x3208:
                displayHeight = _bb.getInt();
                break;
            case 0x3209:
                displayWidth = _bb.getInt();
                break;
            case 0x320a:
                displayXOffset = _bb.getInt();
                break;
            case 0x320b:
                displayYOffset = _bb.getInt();
                break;
            case 0x3217:
                displayF2Offset = _bb.getInt();
                break;
            case 0x320e:
                aspectRatio = new Rational(_bb.getInt(), _bb.getInt());
                break;
            case 0x3218:
                activeFormatDescriptor = _bb.get();
                break;
            case 0x320d:
                videoLineMap = readInt32Batch(_bb);
                break;
            case 0x320f:
                alphaTransparency = _bb.get();
                break;
            case 0x3210:
                transferCharacteristic = UL.read(_bb);
                break;
            case 0x3211:
                imageAlignmentOffset = _bb.getInt();
                break;
            case 0x3213:
                imageStartOffset = _bb.getInt();
                break;
            case 0x3214:
                imageEndOffset = _bb.getInt();
                break;
            case 0x3212:
                fieldDominance = _bb.get();
                break;
            case 0x3201:
                pictureEssenceCoding = UL.read(_bb);
                break;
            case 0x321a:
                codingEquations = UL.read(_bb);
                break;
            case 0x3219:
                colorPrimaries = UL.read(_bb);
                break;
            default:
//                System.out.println(String.format("Unknown tag [ GenericPictureEssenceDescriptor: " + ul + "]: %04x", entry.getKey()));
                continue;
            }
            it.remove();
        }
    }

    public byte getSignalStandard() {
        return signalStandard;
    }

    public LayoutType getFrameLayout() {
        return frameLayout;
    }

    public int getStoredWidth() {
        return storedWidth;
    }

    public int getStoredHeight() {
        return storedHeight;
    }

    public int getStoredF2Offset() {
        return storedF2Offset;
    }

    public int getSampledWidth() {
        return sampledWidth;
    }

    public int getSampledHeight() {
        return sampledHeight;
    }

    public int getSampledXOffset() {
        return sampledXOffset;
    }

    public int getSampledYOffset() {
        return sampledYOffset;
    }

    public int getDisplayHeight() {
        return displayHeight;
    }

    public int getDisplayWidth() {
        return displayWidth;
    }

    public int getDisplayXOffset() {
        return displayXOffset;
    }

    public int getDisplayYOffset() {
        return displayYOffset;
    }

    public int getDisplayF2Offset() {
        return displayF2Offset;
    }

    public Rational getAspectRatio() {
        return aspectRatio;
    }

    public byte getActiveFormatDescriptor() {
        return activeFormatDescriptor;
    }

    public int[] getVideoLineMap() {
        return videoLineMap;
    }

    public byte getAlphaTransparency() {
        return alphaTransparency;
    }

    public UL getTransferCharacteristic() {
        return transferCharacteristic;
    }

    public int getImageAlignmentOffset() {
        return imageAlignmentOffset;
    }

    public int getImageStartOffset() {
        return imageStartOffset;
    }

    public int getImageEndOffset() {
        return imageEndOffset;
    }

    public byte getFieldDominance() {
        return fieldDominance;
    }

    public UL getPictureEssenceCoding() {
        return pictureEssenceCoding;
    }

    public UL getCodingEquations() {
        return codingEquations;
    }

    public UL getColorPrimaries() {
        return colorPrimaries;
    }
}