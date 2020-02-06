package org.jcodec.common;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Assert;
import org.junit.Test;

public class CollectionsTest {

    @Test
    public void testIntObjectMap() {
        IntObjectMap<Integer> test = new IntObjectMap<Integer>();

        test.put(24, 11);
        assertEquals(Integer.valueOf(11), test.get(24));

        test.put(248, 21);
        assertEquals(Integer.valueOf(11), test.get(24));
        assertEquals(Integer.valueOf(21), test.get(248));

        assertEquals(test.size(), 2);

        assertArrayEquals(test.keys(), new int[] { 24, 248 });
        assertArrayEquals(test.values(new Integer[0]), new Integer[] { 11, 21 });
    }

    @Test
    public void testIntIntMap() {
        IntIntMap test = new IntIntMap();

        test.put(24, 11);
        assertEquals(11, test.get(24));

        test.put(248, 21);
        assertEquals(11, test.get(24));
        assertEquals(21, test.get(248));

        assertEquals(test.size(), 2);

        assertArrayEquals(test.keys(), new int[] { 24, 248 });
        assertArrayEquals(test.values(), new int[] { 11, 21 });
    }
    
    @Test
    public void testByteArrayList() {
        ByteArrayList byteal = new ByteArrayList(3);
        byte[] arr = new byte[197];
        for (int i = 0; i < 200; i++) {
            byteal.add((byte)i);
            if (i >= 3)
                arr[i - 3] = (byte)i;
        }
        assertEquals(0, byteal.shift());
        assertEquals(1, byteal.shift());
        assertEquals(2, byteal.shift());
        
        assertEquals(20, byteal.get(17));
        assertEquals(40, byteal.get(37));
        assertEquals((byte)151, byteal.get(148));
        assertEquals(197, byteal.size());
        assertArrayEquals(arr, byteal.toArray());
        byte[] other = new byte[] { 1, 2, 3, 4, 5 };
        byteal.addAll(other);
        assertEquals(202, byteal.size());
        assertEquals(5, byteal.get(201));
        byteal.fill(50, 100, (byte)99);
        assertEquals(202, byteal.size());
        for (int i = 50; i < 100; i++)
            assertEquals(99, byteal.get(i));
        byteal.fill(199, 250, (byte)99);
        assertEquals(250, byteal.size());
        for (int i = 199; i < 250; i++)
            assertEquals(99, byteal.get(i));
        byteal.fill(500, 650, (byte)99);
        assertEquals(650, byteal.size());
        for (int i = 500; i < 650; i++)
            assertEquals(99, byteal.get(i));
        byteal.set(450, (byte)42);
        assertEquals(42, byteal.get(450));
        for (int i = 0; i < 30; i++)
            byteal.push((byte)(42 + i));
        assertEquals(680, byteal.size());
        for (int i = 0; i < 30; i++)
            assertEquals(42 + i, byteal.get(650 + i));
        
        Assert.assertTrue(byteal.contains((byte)99));
        Assert.assertTrue(byteal.contains((byte)42));
        Assert.assertFalse(byteal.contains((byte)73));
        
        for (int i = 0; i < 30; i++)
            assertEquals((byte)(i + 3), byteal.shift());
            
        for (int i = 0; i < 479; i++)
            byteal.pop();
        assertEquals(171, byteal.size());
        assertEquals(1, byteal.get(167));
        for (int i = 0; i < 161; i++)
            byteal.pop();
        assertEquals(10, byteal.size());
        byteal.clear();
        assertEquals(0, byteal.size());
    }

    @Test
    public void testIntArrayList() {
        IntArrayList intal = new IntArrayList(3);
        int[] arr = new int[197];
        for (int i = 0; i < 200; i++) {
            intal.add(i * 10);
            if (i >= 3)
                arr[i - 3] = i * 10;
        }
        assertEquals(0, intal.shift());
        assertEquals(10, intal.shift());
        assertEquals(20, intal.shift());
        
        assertEquals(200, intal.get(17));
        assertEquals(400, intal.get(37));
        assertEquals(1510, intal.get(148));
        assertEquals(197, intal.size());
        assertArrayEquals(arr, intal.toArray());
        int[] other = new int[] { 1, 2, 3, 4, 5 };
        intal.addAll(other);
        assertEquals(202, intal.size());
        assertEquals(5, intal.get(201));
        intal.fill(50, 100, 99);
        assertEquals(202, intal.size());
        for (int i = 50; i < 100; i++)
            assertEquals(99, intal.get(i));
        intal.fill(199, 250, 99);
        assertEquals(250, intal.size());
        for (int i = 199; i < 250; i++)
            assertEquals(99, intal.get(i));
        intal.fill(500, 650, 99);
        assertEquals(650, intal.size());
        for (int i = 500; i < 650; i++)
            assertEquals(99, intal.get(i));
        intal.set(450, 42);
        assertEquals(42, intal.get(450));
        for (int i = 0; i < 30; i++)
            intal.push(42 + i);
        assertEquals(680, intal.size());
        for (int i = 0; i < 30; i++)
            assertEquals(42 + i, intal.get(650 + i));
        
        Assert.assertTrue(intal.contains(99));
        Assert.assertTrue(intal.contains(42));
        Assert.assertFalse(intal.contains(73));
        
        for (int i = 0; i < 30; i++)
            assertEquals((i + 3) * 10, intal.shift());
            
        for (int i = 0; i < 479; i++)
            intal.pop();
        assertEquals(171, intal.size());
        assertEquals(1, intal.get(167));
        for (int i = 0; i < 161; i++)
            intal.pop();
        assertEquals(10, intal.size());
        intal.clear();
        assertEquals(0, intal.size());
    }
    
    @Test
    public void testLongArrayList() {
        LongArrayList longal = new LongArrayList(3);
        long[] arr = new long[197];
        for (int i = 0; i < 200; i++) {
            longal.add(i * 10);
            if (i >= 3)
                arr[i - 3] = i * 10;
        }
        assertEquals(0, longal.shift());
        assertEquals(10, longal.shift());
        assertEquals(20, longal.shift());
        
        assertEquals(200, longal.get(17));
        assertEquals(400, longal.get(37));
        assertEquals(1510, longal.get(148));
        assertEquals(197, longal.size());
        assertArrayEquals(arr, longal.toArray());
        long[] other = new long[] { 1, 2, 3, 4, 5 };
        longal.addAll(other);
        assertEquals(202, longal.size());
        assertEquals(5, longal.get(201));
        longal.fill(50, 100, 99);
        assertEquals(202, longal.size());
        for (int i = 50; i < 100; i++)
            assertEquals(99, longal.get(i));
        longal.fill(199, 250, 99);
        assertEquals(250, longal.size());
        for (int i = 199; i < 250; i++)
            assertEquals(99, longal.get(i));
        longal.fill(500, 650, 99);
        assertEquals(650, longal.size());
        for (int i = 500; i < 650; i++)
            assertEquals(99, longal.get(i));
        longal.set(450, 42);
        assertEquals(42, longal.get(450));
        for (int i = 0; i < 30; i++)
            longal.push(42 + i);
        assertEquals(680, longal.size());
        for (int i = 0; i < 30; i++)
            assertEquals(42 + i, longal.get(650 + i));
        
        Assert.assertTrue(longal.contains(99));
        Assert.assertTrue(longal.contains(42));
        Assert.assertFalse(longal.contains(73));
        
        for (int i = 0; i < 30; i++)
            assertEquals((i + 3) * 10, longal.shift());
            
        for (int i = 0; i < 479; i++)
            longal.pop();
        assertEquals(171, longal.size());
        assertEquals(1, longal.get(167));
        for (int i = 0; i < 161; i++)
            longal.pop();
        assertEquals(10, longal.size());
        longal.clear();
        assertEquals(0, longal.size());
    }
}
