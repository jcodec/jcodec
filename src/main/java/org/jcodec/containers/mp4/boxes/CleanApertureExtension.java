package org.jcodec.containers.mp4.boxes;

import java.nio.ByteBuffer;

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
        out.putInt(this.apertureWidthNumerator);
        out.putInt(this.apertureWidthDenominator);

        out.putInt(this.apertureHeightNumerator);
        out.putInt(this.apertureHeightDenominator);

        out.putInt(this.horizOffsetNumerator);
        out.putInt(this.horizOffsetDenominator);

        out.putInt(this.vertOffsetNumerator);
        out.putInt(this.vertOffsetDenominator);
    }
}
