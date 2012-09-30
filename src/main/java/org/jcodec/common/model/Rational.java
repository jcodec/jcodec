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

    public int multiply(int val) {
        return (num * val) / den;
    }
    
    public int divide(int val) {
        return (den * val) / num;
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
}
