package org.jcodec.common.model;

import org.apache.commons.lang.StringUtils;

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
    private final int num;
    private final int den;

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
        String[] split = StringUtils.split(string, ":");
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
        return (num * val) / den;
    }
    
    public int divideS(int val) {
        return (den * val) / num;
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
    
    public static RationalLarge R(long num, long den) {
        return new RationalLarge(num, den);
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
        return new Rational(num * other.den + other.num * den, den * other.den);
    }

    public Rational minus(Rational other) {
        return new Rational(num * other.den - other.num * den, den * other.den);
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
        return new Rational(num * other.num, den * other.den);
    }

    public Rational divide(Rational other) {
        return new Rational(other.num * den, other.den * num);
    }

    public Rational divideBy(Rational other) {
        return new Rational(num * other.den, den * other.num);
    }
}
