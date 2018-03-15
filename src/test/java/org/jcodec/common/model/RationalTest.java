package org.jcodec.common.model;

import static org.junit.Assert.*;

import org.junit.Test;

public class RationalTest {
    @Test
    public void testParse() {
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

    @Test
    public void testParseError() {
        try {
            Rational.parse("42.43");
            fail("NumberFormatException expected");
        } catch (NumberFormatException e) {
            //expected
        }
    }

    @Test
    public void testParseError2() {
        try {
            Rational.parse("42/a");
            fail("NumberFormatException expected");
        } catch (NumberFormatException e) {
            //expected
        }
    }

    @Test
    public void testParseError3() {
        try {
            Rational.parse("42:a");
        } catch (NumberFormatException e) {
            //expected
        }
    }

}
