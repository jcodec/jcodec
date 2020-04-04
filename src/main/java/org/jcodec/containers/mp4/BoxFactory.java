package org.jcodec.containers.mp4;

import org.jcodec.containers.mp4.boxes.*;
import org.jcodec.platform.Platform;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Default box factory
 * 
 * @author The JCodec project
 * 
 */
public class BoxFactory implements IBoxFactory {

    private static IBoxFactory instance = new BoxFactory(new DefaultBoxes());
    private static IBoxFactory audio = new BoxFactory(new AudioBoxes());
    private static IBoxFactory data = new BoxFactory(new DataBoxes());
    private static IBoxFactory sample = new BoxFactory(new SampleBoxes());
    private static IBoxFactory timecode = new BoxFactory(new TimecodeBoxes());
    private static IBoxFactory video = new BoxFactory(new VideoBoxes());
    private static IBoxFactory waveext = new BoxFactory(new WaveExtBoxes());
    private static IBoxFactory metadata = new BoxFactory(new MetaDataBoxes());

    private Boxes boxes;

    public static IBoxFactory getDefault() {
        return instance;
    }

    public BoxFactory(Boxes boxes) {
        this.boxes = boxes;
    }

    @Override
    public Box newBox(Header header) {
        Class<? extends Box> claz = boxes.toClass(header.getFourcc());
        if (claz == null)
            return new Box.LeafBox(header);
        Box box = Platform.newInstance(claz, new Object[] { header });
        if (box instanceof NodeBox) {
            NodeBox nodebox = (NodeBox) box;
            if (nodebox instanceof SampleDescriptionBox) {
                nodebox.setFactory(sample);
            } else if (nodebox instanceof VideoSampleEntry) {
                nodebox.setFactory(video);
            } else if (nodebox instanceof AudioSampleEntry) {
                nodebox.setFactory(audio);
            } else if (nodebox instanceof TimecodeSampleEntry) {
                nodebox.setFactory(timecode);
            } else if (nodebox instanceof MetaDataSampleEntry) {
                nodebox.setFactory(metadata);
            } else if (nodebox instanceof DataRefBox) {
                nodebox.setFactory(data);
            } else if (nodebox instanceof WaveExtension) {
                nodebox.setFactory(waveext);
            } else if (nodebox instanceof KeysBox) {
                //keys box has its own box factory
            } else {
                nodebox.setFactory(this);
            }
        }
        return box;
    }
}