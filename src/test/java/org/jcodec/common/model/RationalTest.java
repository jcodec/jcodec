package org.jcodec.common.model;

import static org.junit.Assert.*;

import org.junit.Test;

public class RationalTest {
    @Test
    public void testParse() throws Exception {
        Rational parse = Rational.parse("42:43");
        assertEquals(42, parse.num);
        assertEquals(43, parse.den);

        Rational parse2 = Rational.parse("42/43");
        assertEquals(42, parse2.num);
        assertEquals(43, parse2.den);

        Rational parse3 = Rational.parse("42");
        assertEquals(42, parse3.num);
        assertEquals(1, parse3.den);
    }

    @Test(expected = NumberFormatException.class)
    public void testParseError() throws Exception {
        Rational.parse("42.43");
    }
    
    @Test(expected = NumberFormatException.class)
    public void testParseError2() throws Exception {
        Rational.parse("42/a");
    }

    @Test(expected = NumberFormatException.class)
    public void testParseError3() throws Exception {
        Rational.parse("42:a");
    }

}
