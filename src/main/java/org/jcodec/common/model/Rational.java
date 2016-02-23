package org.jcodec.common.model;

import static org.jcodec.common.StringUtils.splitS;
import static org.jcodec.common.StringUtils.splitC;
import static org.jcodec.common.tools.MathUtil.reduce;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class Rational {

    public static final Rational ONE = new Rational(1, 1);
    public static final Rational HALF = new Rational(1, 2);
    public static final Rational ZERO = new Rational(0, 1);
    final int num;
    final int den;

    public static Rational R(int num, int den) {
        return new Rational(num, den);
    }

    public static Rational R(int num) {
        return R(num, 1);
    }

    public Rational(int num, int den) {
        this.num = num;
        this.den = den;
    }

    public int getNum() {
        return num;
    }

    public int getDen() {
        return den;
    }

    public static Rational parse(String string) {
        String[] split = splitS(string, ":");
        return new Rational(Integer.parseInt(split[0]), Integer.parseInt(split[1]));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + den;
        result = prime * result + num;
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
        Rational other = (Rational) obj;
        if (den != other.den)
            return false;
        if (num != other.num)
            return false;
        return true;
    }

    public int multiplyS(int val) {
        return (int) (((long) num * val) / den);
    }

    public int divideS(int val) {
        return (int) (((long) den * val) / num);
    }

    public int divideByS(int val) {
        return num / (den * val);
    }

    public long multiply(long val) {
        return (num * val) / den;
    }

    public long divide(long val) {
        return (den * val) / num;
    }

    public Rational flip() {
        return new Rational(den, num);
    }

    public boolean smallerThen(Rational sec) {
        return num * sec.den < sec.num * den;
    }

    public boolean greaterThen(Rational sec) {
        return num * sec.den > sec.num * den;
    }

    public boolean smallerOrEqualTo(Rational sec) {
        return num * sec.den <= sec.num * den;
    }

    public boolean greaterOrEqualTo(Rational sec) {
        return num * sec.den >= sec.num * den;
    }

    public boolean equals(Rational other) {
        return num * other.den == other.num * den;
    }

    public Rational plus(Rational other) {
        return reduce(num * other.den + other.num * den, den * other.den);
    }

    public RationalLarge plus(RationalLarge other) {
        return reduce(num * other.den + other.num * den, den * other.den);
    }

    public Rational minus(Rational other) {
        return reduce(num * other.den - other.num * den, den * other.den);
    }

    public RationalLarge minus(RationalLarge other) {
        return reduce(num * other.den - other.num * den, den * other.den);
    }

    public Rational plus(int scalar) {
        return new Rational(num + scalar * den, den);
    }

    public Rational minus(int scalar) {
        return new Rational(num - scalar * den, den);
    }

    public Rational multiply(int scalar) {
        return new Rational(num * scalar, den);
    }

    public Rational divide(int scalar) {
        return new Rational(den * scalar, num);
    }

    public Rational divideBy(int scalar) {
        return new Rational(num, den * scalar);
    }

    public Rational multiply(Rational other) {
        return reduce(num * other.num, den * other.den);
    }

    public RationalLarge multiply(RationalLarge other) {
        return reduce(num * other.num, den * other.den);
    }

    public Rational divide(Rational other) {
        return reduce(other.num * den, other.den * num);
    }

    public RationalLarge divide(RationalLarge other) {
        return reduce(other.num * den, other.den * num);
    }

    public Rational divideBy(Rational other) {
        return reduce(num * other.den, den * other.num);
    }

    public RationalLarge divideBy(RationalLarge other) {
        return reduce(num * other.den, den * other.num);
    }

    public float scalar() {
        return (float) num / den;
    }

    public int scalarClip() {
        return num / den;
    }
}
