package org.jcodec.codecs.mjpeg;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/**
 * This class is part of JCodec ( www.jcodec.org )
 * This software is distributed under FreeBSD License
 * 
 * This header specifies the source image characteristics (see A.1), the
 * components in the frame, and the sampling factors for each component, and
 * specifies the destinations from which the quantized tables to be used with
 * each component are retrieved.
 */
public class FrameHeader {
    /**
     * Frame header length – Specifies the length of the frame header shown in
     * Figure B.3 (see B.1.1.4).
     */
    int lf;
    /**
     * Sample precision – Specifies the precision in bits for the samples of the
     * components in the frame.
     */
    int p;

    /**
     * Number of lines – Specifies the maximum number of lines in the source
     * image. This shall be equal to the number of lines in the component with
     * the maximum number of vertical samples (see A.1.1). Value 0 indicates
     * that the number of lines shall be defined by the DNL marker and
     * parameters at the end of the first scan (see B.2.5).
     */
    int y;

    /**
     * Number of samples per line – Specifies the maximum number of samples per
     * line in the source image. This shall be equal to the number of samples
     * per line in the component with the maximum number of horizontal samples
     * (see A.1.1).
     */
    int x;

    /**
     * Number of image components in frame – Specifies the number of source
     * image components in the frame. The value of Nf shall be equal to the
     * number of sets of frame component specification parameters (Ci, Hi, Vi,
     * and Tq) present in the frame header.
     */
    int nf;

    public static class Component {
        /**
         * Component identifier – Assigns a unique label to the ith component in
         * the sequence of frame component specification parameters. These
         * values shall be used in the scan headers to identify the components
         * in the scan. The value of Ci shall be different from the values of C1
         * through Ci − 1.
         */
        int ci;
        /**
         * Horizontal sampling factor – Specifies the relationship between the
         * component horizontal dimension and maximum image dimension X (see
         * A.1.1); also specifies the number of horizontal data units of
         * component Ci in each MCU, when more than one component is encoded in
         * a scan.
         */
        int h;
        /**
         * Vertical sampling factor – Specifies the relationship between the
         * component vertical dimension and maximum image dimension Y (see
         * A.1.1); also specifies the number of vertical data units of component
         * Ci in each MCU, when more than one component is encoded in a scan.
         */
        int v;

        /**
         * Quantization table destination selector – Specifies one of four
         * possible quantization table destinations from which the quantization
         * table to use for dequantization of DCT coefficients of component Ci
         * is retrieved. If the decoding process uses the dequantization
         * procedure, this table shall have been installed in this destination
         * by the time the decoder is ready to decode the scan(s) containing
         * component Ci. The destination shall not be re- specified, or its
         * contents changed, until all scans containing Ci have been completed.
         */
        int tq;

        public String toString() {
            return ToStringBuilder.reflectionToString(this,
                    ToStringStyle.SHORT_PREFIX_STYLE);
        }
    }

    public int getHmax() {
        int max = 0;
        for (Component c : components) {
            max = Math.max(max, c.h);
        }
        return max;
    }

    public int getVmax() {
        int max = 0;
        for (Component c : components) {
            max = Math.max(max, c.v);
        }
        return max;
    }

    Component[] components;

    public static FrameHeader read(InputStream is) throws IOException {
        FrameHeader frame = new FrameHeader();
        frame.lf = readShort(is);
        frame.p = is.read();
        frame.y = readShort(is);
        frame.x = readShort(is);
        frame.nf = is.read();
        frame.components = new Component[frame.nf];
        for (int i = 0; i < frame.components.length; i++) {
            Component c = frame.components[i] = new Component();
            c.ci = is.read();
            int hv = is.read();
            c.h = (hv & 0xf0) >>> 4;
            c.v = (hv & 0x0f);
            c.tq = is.read();
        }
        return frame;
    }

    public String toString() {
        return ToStringBuilder.reflectionToString(this,
                ToStringStyle.SHORT_PREFIX_STYLE);
    }

    private static int readShort(InputStream is) throws IOException {
        int b1 = is.read();
        int b2 = is.read();

        return (b1 << 8) + b2;
    }
}
