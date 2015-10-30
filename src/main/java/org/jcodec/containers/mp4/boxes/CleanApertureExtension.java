package org.jcodec.containers.mp4.boxes;

import java.nio.ByteBuffer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 */
public class CleanApertureExtension extends Box {
    private int vertOffsetDenominator;
    private int vertOffsetNumerator;
    private int horizOffsetDenominator;
    private int horizOffsetNumerator;
    private int apertureHeightDenominator;
    private int apertureHeightNumerator;
    private int apertureWidthDenominator;
    private int apertureWidthNumerator;

    public CleanApertureExtension(int apertureWidthN, int apertureWidthD, int apertureHeightN, int apertureHeightD,
            int horizOffN, int horizOffD, int vertOffN, int vertOffD) {
        super(new Header(fourcc()));
        this.apertureWidthNumerator = apertureWidthN;
        this.apertureWidthDenominator = apertureWidthD;
        this.apertureHeightNumerator = apertureHeightN;
        this.apertureHeightDenominator = apertureHeightD;
        this.horizOffsetNumerator = horizOffN;
        this.horizOffsetDenominator = horizOffD;
        this.vertOffsetNumerator = vertOffN;
        this.vertOffsetDenominator = vertOffD;
    }

    public CleanApertureExtension() {
        super(new Header(fourcc()));
    }

    @Override
    public void parse(ByteBuffer is) {
        this.apertureWidthNumerator = is.getInt();
        this.apertureWidthDenominator = is.getInt();

        this.apertureHeightNumerator = is.getInt();
        this.apertureHeightDenominator = is.getInt();

        this.horizOffsetNumerator = is.getInt();
        this.horizOffsetDenominator = is.getInt();

        this.vertOffsetNumerator = is.getInt();
        this.vertOffsetDenominator = is.getInt();
    }

    public static String fourcc() {
        return "clap";
    }

    @Override
    public void doWrite(ByteBuffer out) {
        out.putInt((int) this.apertureWidthNumerator);
        out.putInt((int) this.apertureWidthDenominator);

        out.putInt((int) this.apertureHeightNumerator);
        out.putInt((int) this.apertureHeightDenominator);

        out.putInt((int) this.horizOffsetNumerator);
        out.putInt((int) this.horizOffsetDenominator);

        out.putInt((int) this.vertOffsetNumerator);
        out.putInt((int) this.vertOffsetDenominator);
    }
}
