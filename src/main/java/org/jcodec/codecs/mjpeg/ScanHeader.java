package org.jcodec.codecs.mjpeg;
import js.nio.ByteBuffer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * This header specifies which component(s) are contained in the scan, specifies
 * the destinations from which the entropy tables to be used with each component
 * are retrieved, and (for the progressive DCT) which part of the DCT quantized
 * coefficient data is contained in the scan. For lossless processes the scan
 * parameters specify the predictor and the point transform.
 * 
 * @author The JCodec project
 */
public class ScanHeader {
    /**
     * Scan header length. Specifies the length of the scan header shown in
     * Figure B.4 (see B.1.1.4).
     */
    int ls;

    /**
     * Number of image components in scan. Specifies the number of source image
     * components in the scan. The value of Ns shall be equal to the number of
     * sets of scan component specification parameters (Csj, Tdj, and Taj)
     * present in the scan header.
     */
    int ns;

    public boolean isInterleaved() {
        return ns > 1;
    }

    public static class Component {
        /**
         * Scan component selector. Selects which of the Nf image components
         * specified in the frame parameters shall be the jth component in the
         * scan. Each Csj shall match one of the Ci values specified in the
         * frame header, and the ordering in the scan header shall follow the
         * ordering in the frame header. If Ns > 1, the order of interleaved
         * components in the MCU is Cs1 first, Cs2 second, etc. If Ns > 1, the
         * following restriction shall be placed on the image components
         * contained in the scan:
         * 
         * <pre>
         * [j=1..Ns](Hj x Vj) &lt;= 10
         * </pre>
         * 
         * where Hj and Vj are the horizontal and vertical sampling factors for
         * scan component j. These sampling factors are specified in the frame
         * header for component i, where i is the frame component specification
         * index for which frame component identifier Ci matches scan component
         * selector Csj. As an example, consider an image having 3 components
         * with maximum dimensions of 512 lines and 512 samples per line, and
         * with the following sampling factors: Component Component 1 Component
         * 2 2 0 4 1 1 2 2 2 0 0 1 1 2 H V H V H V = = = = = = , , Then the
         * summation of Hj Vj is (4 1) + (1 2) + (2 2) = 10. The value of Csj
         * shall be different from the values of Cs1 to Csj 1.
         */
        int cs;

        /**
         * DC entropy coding table destination selector. Specifies one of four
         * possible DC entropy coding table destinations from which the entropy
         * table needed for decoding of the DC coefficients of component Csj is
         * retrieved. The DC entropy table shall have been installed in this
         * destination (see B.2.4.2 and B.2.4.3) by the time the decoder is
         * ready to decode the current scan. This parameter specifies the
         * entropy coding table destination for the lossless processes.
         */
        int td;

        /**
         * AC entropy coding table destination selector. Specifies one of four
         * possible AC entropy coding table destinations from which the entropy
         * table needed for decoding of the AC coefficients of component Csj is
         * retrieved. The AC entropy table selected shall have been installed in
         * this destination (see B.2.4.2 and B.2.4.3) by the time the decoder is
         * ready to decode the current scan. This parameter is zero for the
         * lossless processes.
         */
        int ta;
    }

    Component[] components;

    /**
     * Start of spectral or predictor selection. In the DCT modes of operation,
     * this parameter specifies the first DCT coefficient in each block in
     * zig-zag order which shall be coded in the scan. This parameter shall be
     * set to zero for the sequential DCT processes. In the lossless mode of
     * operations this parameter is used to select the predictor.
     */
    int ss;

    /**
     * End of spectral selection. Specifies the last DCT coefficient in each
     * block in zig-zag order which shall be coded in the scan. This parameter
     * shall be set to 63 for the sequential DCT processes. In the lossless mode
     * of operations this parameter has no meaning. It shall be set to zero.
     */
    int se;

    /**
     * Successive approximation bit position high. This parameter specifies the
     * point transform used in the preceding scan (i.e. successive approximation
     * bit position low in the preceding scan) for the band of coefficients
     * specified by Ss and Se. This parameter shall be set to zero for the first
     * scan of each band of coefficients. In the lossless mode of operations
     * this parameter has no meaning. It shall be set to zero.
     */
    int ah;

    /**
     * Successive approximation bit position low or point transform. In the DCT
     * modes of operation this parameter specifies the point transform, i.e. bit
     * position low, used before coding the band of coefficients specified by Ss
     * and Se. This parameter shall be set to zero for the sequential DCT
     * processes. In the lossless mode of operations, this parameter specifies
     * the point transform, Pt.
     */
    int al;

    public static ScanHeader read(ByteBuffer bb) {
        ScanHeader scan = new ScanHeader();
        scan.ls = bb.getShort() & 0xffff;
        scan.ns = bb.get() & 0xff;
        scan.components = new Component[scan.ns];
        for (int i = 0; i < scan.components.length; i++) {
            Component c = scan.components[i] = new Component();
            c.cs = bb.get() & 0xff;
            int tdta = bb.get() & 0xff;
            c.td = (tdta & 0xf0) >>> 4;
            c.ta = (tdta & 0x0f);
        }
        scan.ss = bb.get() & 0xff;
        scan.se = bb.get() & 0xff;
        int ahal = bb.get() & 0xff;
        scan.ah = (ahal & 0xf0) >>> 4;
        scan.al = (ahal & 0x0f);
        return scan;
    }
}
