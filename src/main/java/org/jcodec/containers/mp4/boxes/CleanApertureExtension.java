package org.jcodec.containers.mp4.boxes;

import static org.jcodec.common.io.ReaderBE.readInt32;

import java.io.DataOutput;
import java.io.IOException;
import java.io.InputStream;

public class CleanApertureExtension extends Box {
    private long vertOffsetDenominator;
    private long vertOffsetNumerator;
    private long horizOffsetDenominator;
    private long horizOffsetNumerator;
    private long apertureHeightDenominator;
    private long apertureHeightNumerator;
    private long apertureWidthDenominator;
    private long apertureWidthNumerator;
    
    public CleanApertureExtension(long apertureWidthN, long apertureWidthD, long apertureHeightN, long apertureHeightD, long horizOffN, long horizOffD, long vertOffN, long vertOffD){
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
    public void parse(InputStream is) throws IOException {
        this.apertureWidthNumerator = readInt32(is);
        this.apertureWidthDenominator = readInt32(is);

        this.apertureHeightNumerator = readInt32(is);
        this.apertureHeightDenominator = readInt32(is);

        this.horizOffsetNumerator = readInt32(is);
        this.horizOffsetDenominator = readInt32(is);

        this.vertOffsetNumerator = readInt32(is);
        this.vertOffsetDenominator = readInt32(is);
    }
    
    public static String fourcc(){
        return "clap";
    }
    
    @Override
    public void doWrite(DataOutput out) throws IOException {
        out.writeInt((int) this.apertureWidthNumerator); 
        out.writeInt((int) this.apertureWidthDenominator);

        out.writeInt((int) this.apertureHeightNumerator);
        out.writeInt((int) this.apertureHeightDenominator);

        out.writeInt((int) this.horizOffsetNumerator);
        out.writeInt((int) this.horizOffsetDenominator);

        out.writeInt((int) this.vertOffsetNumerator);
        out.writeInt((int) this.vertOffsetDenominator);        
    }

}
