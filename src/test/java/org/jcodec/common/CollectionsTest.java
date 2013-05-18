package org.jcodec.common;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

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
        ByteArrayList intal = new ByteArrayList();
        byte[] arr = new byte[200];
        for (int i = 0; i < 200; i++) {
            intal.add((byte)i);
            arr[i] = (byte)i;
        }
        assertEquals(20, intal.get(20));
        assertEquals(40, intal.get(40));
        assertEquals(151, intal.get(151) & 0xff);
        assertEquals(200, intal.size());
        assertArrayEquals(arr, intal.toArray());
        byte[] other = new byte[] { 1, 2, 3, 4, 5 };
        intal.addAll(other);
        assertEquals(205, intal.size());
        assertEquals(5, intal.get(204));
        intal.fill(10, 100, (byte)99);
        assertEquals(99, intal.get(50));
    }

    @Test
    public void testIntArrayList() {
        IntArrayList intal = new IntArrayList();
        int[] arr = new int[200];
        for (int i = 0; i < 200; i++) {
            intal.add(i * 10);
            arr[i] = i * 10;
        }
        assertEquals(200, intal.get(20));
        assertEquals(400, intal.get(40));
        assertEquals(1510, intal.get(151));
        assertEquals(200, intal.size());
        assertArrayEquals(arr, intal.toArray());
        int[] other = new int[] { 1, 2, 3, 4, 5 };
        intal.addAll(other);
        assertEquals(205, intal.size());
        assertEquals(5, intal.get(204));
        intal.fill(10, 100, 99);
        assertEquals(99, intal.get(50));
    }
    
    @Test
    public void testLongArrayList() {
        LongArrayList intal = new LongArrayList();
        long[] arr = new long[200];
        for (int i = 0; i < 200; i++) {
            intal.add(i * 10);
            arr[i] = i * 10;
        }
        assertEquals(200, intal.get(20));
        assertEquals(400, intal.get(40));
        assertEquals(1510, intal.get(151));
        assertEquals(200, intal.size());
        assertArrayEquals(arr, intal.toArray());
        long[] other = new long[] { 1, 2, 3, 4, 5 };
        intal.addAll(other);
        assertEquals(205, intal.size());
        assertEquals(5, intal.get(204));
        intal.fill(10, 100, 99);
        assertEquals(99, intal.get(50));
    }
}
