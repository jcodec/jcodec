package org.jcodec.common.model;

import org.apache.commons.lang.StringUtils;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class RationalLarge {

    public static final Rational ONE = new Rational(1, 1);
    public static final Rational HALF = new Rational(1, 2);
    public static final RationalLarge ZERO = new RationalLarge(0, 1);

    private final long num;
    private final long den;

    public RationalLarge(long num, long den) {
        this.num = num;
        this.den = den;
    }

    public long getNum() {
        return num;
    }

    public long getDen() {
        return den;
    }

    public static Rational parse(String string) {
        String[] split = StringUtils.split(string, ":");
        return new Rational(Integer.parseInt(split[0]), Integer.parseInt(split[1]));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (den ^ (den >>> 32));
        result = prime * result + (int) (num ^ (num >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        RationalLarge other = (RationalLarge) obj;
        if (den != other.den)
            return false;
        if (num != other.num)
            return false;
        return true;
    }

    public long multiplyS(long scalar) {
        return (num * scalar) / den;
    }

    public long divideS(long scalar) {
        return (den * scalar) / num;
    }

    public long divideByS(long scalar) {
        return num / (den * scalar);
    }

    public RationalLarge flip() {
        return new RationalLarge(den, num);
    }

    public static RationalLarge R(long num, long den) {
        return new RationalLarge(num, den);
    }

    public boolean lessThen(RationalLarge sec) {
        return num * sec.den < sec.num * den;
    }

    public boolean greaterThen(RationalLarge sec) {
        return num * sec.den > sec.num * den;
    }

    public boolean smallerOrEqualTo(RationalLarge sec) {
        return num * sec.den <= sec.num * den;
    }

    public boolean greaterOrEqualTo(RationalLarge sec) {
        return num * sec.den >= sec.num * den;
    }

    public boolean equals(RationalLarge other) {
        return num * other.den == other.num * den;
    }

    public RationalLarge plus(RationalLarge other) {
        return new RationalLarge(num * other.den + other.num * den, den * other.den);
    }

    public RationalLarge minus(RationalLarge other) {
        return new RationalLarge(num * other.den - other.num * den, den * other.den);
    }

    public RationalLarge plus(long scalar) {
        return new RationalLarge(num + scalar * den, den);
    }

    public RationalLarge minus(long scalar) {
        return new RationalLarge(num - scalar * den, den);
    }

    public RationalLarge multiply(long scalar) {
        return new RationalLarge(num * scalar, den);
    }

    public RationalLarge divide(long scalar) {
        return new RationalLarge(den * scalar, num);
    }

    public RationalLarge divideBy(long scalar) {
        return new RationalLarge(num, den * scalar);
    }

    public RationalLarge multiply(RationalLarge other) {
        return new RationalLarge(num * other.num, den * other.den);
    }

    public RationalLarge divide(RationalLarge other) {
        return new RationalLarge(other.num * den, other.den * num);
    }

    public RationalLarge divideBy(RationalLarge other) {
        return new RationalLarge(num * other.den, den * other.num);
    }

    public double scalar() {
        return ((double) num) / den;
    }
}
