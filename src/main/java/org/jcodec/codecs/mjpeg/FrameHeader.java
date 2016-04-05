package org.jcodec.codecs.mjpeg;
import js.nio.ByteBuffer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * This header specifies the source image characteristics (see A.1), the
 * components in the frame, and the sampling factors for each component, and
 * specifies the destinations from which the quantized tables to be used with
 * each component are retrieved.
 * 
 * @author The JCodec project
 */
public class FrameHeader {
    /**
     * Frame header length. Specifies the length of the frame header shown in
     * Figure B.3 (see B.1.1.4).
     */
    int headerLength;

    /**
     * Sample precision. Specifies the precision in bits for the samples of the
     * components in the frame.
     */
    int bitsPerSample;

    /**
     * Number of lines. Specifies the maximum number of lines in the source
     * image. This shall be equal to the number of lines in the component with
     * the maximum number of vertical samples (see A.1.1). Value 0 indicates
     * that the number of lines shall be defined by the DNL marker and
     * parameters at the end of the first scan (see B.2.5).
     */
    int height;

    /**
     * Number of samples per line. Specifies the maximum number of samples per
     * line in the source image. This shall be equal to the number of samples
     * per line in the component with the maximum number of horizontal samples
     * (see A.1.1).
     */
    int width;

    /**
     * Number of image components in frame Specifies the number of source image
     * components in the frame. The value of Nf shall be equal to the number of
     * sets of frame component specification parameters (Ci, Hi, Vi, and Tq)
     * present in the frame header.
     */
    int nComp;

    public static class Component {
        /**
         * Component identifier. Assigns a unique label to the ith component in
         * the sequence of frame component specification parameters. These
         * values shall be used in the scan headers to identify the components
         * in the scan. The value of Ci shall be different from the values of C1
         * through Ci 1.
         */
        int index;
        /**
         * Horizontal sampling factor. Specifies the relationship between the
         * component horizontal dimension and maximum image dimension X (see
         * A.1.1); also specifies the number of horizontal data units of
         * component Ci in each MCU, when more than one component is encoded in
         * a scan.
         */
        int subH;
        /**
         * Vertical sampling factor. Specifies the relationship between the
         * component vertical dimension and maximum image dimension Y (see
         * A.1.1); also specifies the number of vertical data units of component
         * Ci in each MCU, when more than one component is encoded in a scan.
         */
        int subV;

        /**
         * Quantization table destination selector. Specifies one of four
         * possible quantization table destinations from which the quantization
         * table to use for dequantization of DCT coefficients of component Ci
         * is retrieved. If the decoding process uses the dequantization
         * procedure, this table shall have been installed in this destination
         * by the time the decoder is ready to decode the scan(s) containing
         * component Ci. The destination shall not be re- specified, or its
         * contents changed, until all scans containing Ci have been completed.
         */
        int quantTable;
    }

    public int getHmax() {
        int max = 0;
        for (int i = 0; i < components.length; i++) {
            Component c = components[i];
            max = Math.max(max, c.subH);
        }
        return max;
    }

    public int getVmax() {
        int max = 0;
        for (int i = 0; i < components.length; i++) {
            Component c = components[i];
            max = Math.max(max, c.subV);
        }
        return max;
    }

    Component[] components;

    public static FrameHeader read(ByteBuffer is) {
        FrameHeader frame = new FrameHeader();
        frame.headerLength = is.getShort() & 0xffff;
        frame.bitsPerSample = is.get() & 0xff;
        frame.height = is.getShort() & 0xffff;
        frame.width = is.getShort() & 0xffff;
        frame.nComp = is.get() & 0xff;
        frame.components = new Component[frame.nComp];
        for (int i = 0; i < frame.components.length; i++) {
            Component c = frame.components[i] = new Component();
            c.index = is.get() & 0xff;
            int hv = is.get() & 0xff;
            c.subH = (hv & 0xf0) >>> 4;
            c.subV = (hv & 0x0f);
            c.quantTable = is.get() & 0xff;
        }
        return frame;
    }
}