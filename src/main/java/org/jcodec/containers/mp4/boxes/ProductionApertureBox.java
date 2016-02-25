package org.jcodec.containers.mp4.boxes;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class ProductionApertureBox extends ClearApertureBox {

    static final String PROF = "prof";

    public static ProductionApertureBox createProductionApertureBox(int width, int height) {
        ProductionApertureBox prof = new ProductionApertureBox(new Header(PROF));
        prof.width = width;
        prof.height = height;
        return prof;
    }

    public ProductionApertureBox(Header atom) {
        super(atom);
    }

}
