package org.jcodec.codecs.h264.io.model;

import static org.jcodec.common.model.Rational.ONE;
import static org.jcodec.common.model.Rational.R;

import org.jcodec.common.model.Rational;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Aspect ratio
 * 
 * dynamic enum
 * 
 * @author The JCodec project
 * 
 */
public class AspectRatio {

    public static final AspectRatio Extended_SAR = new AspectRatio(255);

    private int value;

    public static Rational[] values = new Rational[] { null, ONE, R(12, 11), R(10, 11), R(16, 11), R(40, 33), R(24, 11),
            R(20, 11), R(32, 11), R(80, 33), R(18, 11), R(15, 11), R(64, 33), R(160, 99), R(4, 3), R(3, 2), R(2, 1) };

    private AspectRatio(int value) {
        this.value = value;
    }

    public Rational toRational() {
        if (values.length > value) {
            return values[value];
        }
        return null;
    }

    public static AspectRatio fromValue(int value) {
        if (value == Extended_SAR.value) {
            return Extended_SAR;
        }
        return new AspectRatio(value);
    }

    public int getValue() {
        return value;
    }
}
