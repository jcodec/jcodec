package org.jcodec.codecs.vpx.vp9;

import java.util.ArrayList;
import java.util.List;

import org.jcodec.codecs.vpx.VPXBooleanDecoder;
import org.jcodec.common.ArrayUtil;
import org.junit.Assert;
import org.junit.Test;

public class CodedSuperBlockTest {

    @Test
    public void testReadPartitionMid() {
        MockVPXBooleanDecoder decoder = new MockVPXBooleanDecoder(new int[] { 158, 97, 94 }, new int[] { 1, 1, 1 });
        DecodingContext c = new DecodingContext();
        c.abovePartitionSizes = new int[] { 2, 2, 2, 2, 1, 1 };
        c.leftPartitionSizes = new int[] { 2, 2, 2, 2, 1, 1, 2, 2 };
        c.tileHeight = 36;
        c.tileWidth = 64;
        ArrayUtil.fill2D(c.partitionProbs,
                new int[] { 158, 97, 94, 93, 24, 99, 85, 119, 44, 62, 59, 67, 149, 53, 53, 94, 20, 48, 83, 53, 24, 52,
                        18, 18, 150, 40, 39, 78, 12, 26, 67, 33, 11, 24, 7, 5, 174, 35, 49, 68, 11, 27, 57, 15, 9, 12,
                        3, 3, },
                0);
        int miCol = 5;
        int miRow = 5;
        int blSz = 0;

        Assert.assertEquals(3, CodedSuperBlock.readPartition(miCol, miRow, blSz, decoder, c));
        Assert.assertTrue(decoder.isFullyRead());
    }

    @Test
    public void testReadPartitionBottom() {
        MockVPXBooleanDecoder decoder = new MockVPXBooleanDecoder(new int[] { 3 }, new int[] { 1 });
        DecodingContext c = new DecodingContext();
        c.abovePartitionSizes = new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 3, 3, 3, 3, 3, 3, 3, 3 };
        c.leftPartitionSizes = new int[] { 3, 3, 3, 3, 3, 3, 3, 3 };
        c.tileHeight = 36;
        c.tileWidth = 64;

        ArrayUtil.fill2D(c.partitionProbs,
                new int[] { 158, 97, 94, 93, 24, 99, 85, 119, 44, 62, 59, 67, 149, 53, 53, 94, 20, 48, 83, 53, 24, 52,
                        18, 18, 150, 40, 39, 78, 12, 26, 67, 33, 11, 24, 7, 5, 174, 35, 49, 68, 11, 27, 57, 15, 9, 12,
                        3, 3, },
                0);
        int miCol = 8;
        int miRow = 32;
        int blSz = 3;

        Assert.assertEquals(3, CodedSuperBlock.readPartition(miCol, miRow, blSz, decoder, c));
        Assert.assertTrue(decoder.isFullyRead());
    }

    @Test
    public void testReadPartitionRight() {
        MockVPXBooleanDecoder decoder = new MockVPXBooleanDecoder(new int[] { 18 }, new int[] { 1 });
        DecodingContext c = new DecodingContext();
        c.abovePartitionSizes = new int[] { 1, 1, 0, 0, 0, 1, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 3, 3, 3, 3, 0, 1, 0, 0 };
        c.leftPartitionSizes = new int[] { 0, 0, 1, 1, 0, 0, 0, 0 };
        c.tileHeight = 13;
        c.tileWidth = 23;

        ArrayUtil.fill2D(c.partitionProbs,
                new int[] { 158, 97, 94, 93, 24, 99, 85, 119, 44, 62, 59, 67, 149, 53, 53, 94, 20, 48, 83, 53, 24, 52,
                        18, 18, 150, 40, 39, 78, 12, 26, 67, 33, 11, 24, 7, 5, 174, 35, 49, 68, 11, 27, 57, 15, 9, 12,
                        3, 3 },
                0);
        int miCol = 22;
        int miRow = 2;
        int blSz = 1;

        Assert.assertEquals(3, CodedSuperBlock.readPartition(miCol, miRow, blSz, decoder, c));
        Assert.assertTrue(decoder.isFullyRead());
    }

    @Test
    public void testReadPartitionBottomRight() {
        MockVPXBooleanDecoder decoder = new MockVPXBooleanDecoder(new int[] {}, new int[] {});
        DecodingContext c = new DecodingContext();
        c.abovePartitionSizes = new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 1, 0 };
        c.leftPartitionSizes = new int[] { 0, 0, 0, 1, 0, 0, 0, 0 };
        c.tileHeight = 13;
        c.tileWidth = 23;

        ArrayUtil.fill2D(c.partitionProbs,
                new int[] { 158, 97, 94, 93, 24, 99, 85, 119, 44, 62, 59, 67, 149, 53, 53, 94, 20, 48, 83, 53, 24, 52,
                        18, 18, 150, 40, 39, 78, 12, 26, 67, 33, 11, 24, 7, 5, 174, 35, 49, 68, 11, 27, 57, 15, 9, 12,
                        3, 3 },
                0);
        int miCol = 22;
        int miRow = 12;
        int blSz = 1;

        Assert.assertEquals(3, CodedSuperBlock.readPartition(miCol, miRow, blSz, decoder, c));
        Assert.assertTrue(decoder.isFullyRead());
    }

    private static class CodedSuperBlockMock extends CodedSuperBlock {
        private int[] expectedMiRow;
        private int[] expectedMiCol;
        private int[] expectedBlSz;
        private int pos;

        public CodedSuperBlockMock(int[] expectedMiCol, int[] expectedMiRow, int[] expectedBlSz) {
            this.expectedMiCol = expectedMiCol;
            this.expectedMiRow = expectedMiRow;
            this.expectedBlSz = expectedBlSz;
        }

        @Override
        protected CodedBlock readBlock(int miCol, int miRow, int blSz, VPXBooleanDecoder decoder,
                DecodingContext c) {
            Assert.assertTrue(pos < expectedMiRow.length);
            Assert.assertEquals(expectedMiRow[pos], miRow);
            Assert.assertEquals(expectedMiCol[pos], miCol);
            Assert.assertEquals(expectedBlSz[pos], blSz);

            ++pos;
            return null;
        }
    }

    @Test
    public void testReadCorner() {
        MockVPXBooleanDecoder decoder = new MockVPXBooleanDecoder(
                new int[] { 12, 3, 3, 24, 7, 5, 52, 18, 18, 62, 59, 67, 62, 59, 67, 62, 59, 67, 62, 59, 67, 52, 18, 18,
                        62, 93, 24, 99, 158, 97, 62, 52, 18, 18, 62, 93, 158, 158, 97, 94, 52, 18, 18, 158, 97, 94, 85,
                        62, 59, 67, 85, 119, 44, 24, 7, 5, 52, 18, 18, 93, 24, 99, 85, 93, 24, 99, 85, 119, 44, 18, 93,
                        24, 99, 62, 59, 67, 52, 18, 18, 93, 24, 99, 62, 59, 67, 62, 93, 18, 62, 59, 67, 93, 7, 18, 85,
                        119, 44, 62, 59, 67, 18, 62, 59, 67, 62, 59, 67, 7, 18, 85, 158, 97, 94, 85, 119, 44 },
                new int[] { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1, 1, 0, 1, 1, 0, 1, 0, 0,
                        1, 1, 1, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0,
                        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 1, 1, 1, 1, 0, 1, 1, 1,
                        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1, 1, 1, 1, 1 });

        DecodingContext c = new DecodingContext();
        int miCol = 16;
        int miRow = 8;
        c.miTileStartCol = 0;
        c.tileHeight = 13;
        c.tileWidth = 23;

        c.abovePartitionSizes = new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0 };
        c.leftPartitionSizes = new int[] { 0, 0, 0, 1, 0, 0, 0, 0 };

        ArrayUtil.fill2D(c.partitionProbs,
                new int[] { 158, 97, 94, 93, 24, 99, 85, 119, 44, 62, 59, 67, 149, 53, 53, 94, 20, 48, 83, 53, 24, 52,
                        18, 18, 150, 40, 39, 78, 12, 26, 67, 33, 11, 24, 7, 5, 174, 35, 49, 68, 11, 27, 57, 15, 9, 12,
                        3, 3 },
                0);

        List<CodedBlock> result = new ArrayList<CodedBlock>();
        CodedSuperBlockMock mock = new CodedSuperBlockMock(
                new int[] { 16, 17, 16, 17, 18, 19, 18, 19, 16, 17, 16, 17, 18, 19, 18, 19, 20, 21, 20, 21, 22, 22, 20,
                        21, 20, 21, 22, 22, 16, 17, 18, 19, 20, 21, 22 },

                new int[] { 8, 8, 9, 9, 8, 8, 9, 9, 10, 10, 11, 11, 10, 10, 11, 11, 8, 8, 9, 9, 8, 9, 10, 10, 11, 11,
                        10, 11, 12, 12, 12, 12, 12, 12, 12 },

                new int[] { 0, 0, 0, 1, 3, 1, 2, 3, 3, 3, 3, 0, 0, 3, 0, 0, 0, 3, 0, 0, 0, 0, 0, 0, 3, 3, 0, 3, 0, 0, 0,
                        0, 3, 0, 0 });
        mock.readSubPartition(miCol, miRow, 3, decoder, c, result);
        Assert.assertEquals(35, result.size());
    }

    @Test
    public void testReadMiddle() {
        MockVPXBooleanDecoder decoder = new MockVPXBooleanDecoder(
                new int[] { 12, 3, 3, 24, 7, 5, 149, 53, 53, 158, 97, 85, 158, 97, 94, 85, 52, 94, 149, 24, 24, 7, 5,
                        149, 53, 53, 158, 158, 158, 158, 97, 94, 83, 53, 52, 149, 53, 53, 67, 33, 11, 83, 149, 53, 53,
                        158, 97, 85, 158, 158, 149, 94 },
                new int[] { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 1, 1, 1, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 0, 0, 0, 1, 1,
                        1, 1, 0, 0, 1, 1, 0, 1, 1, 1, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0 });

        DecodingContext c = new DecodingContext();
        int miCol = 32;
        int miRow = 8;
        c.miTileStartCol = 0;
        c.tileHeight = 36;
        c.tileWidth = 64;

        c.abovePartitionSizes = new int[] { 3, 3, 3, 3, 3, 3, 3, 3, 2, 2, 2, 2, 2, 2, 2, 2, 1, 0, 2, 2, 2, 2, 1, 1, 3,
                3, 3, 3, 1, 1, 1, 1, 2, 2, 0, 1, 1, 1, 2, 2 };
        c.leftPartitionSizes = new int[] { 2, 2, 2, 2, 2, 2, 1, 1 };

        ArrayUtil.fill2D(c.partitionProbs,
                new int[] { 158, 97, 94, 93, 24, 99, 85, 119, 44, 62, 59, 67, 149, 53, 53, 94, 20, 48, 83, 53, 24, 52,
                        18, 18, 150, 40, 39, 78, 12, 26, 67, 33, 11, 24, 7, 5, 174, 35, 49, 68, 11, 27, 57, 15, 9, 12,
                        3, 3 },
                0);

        List<CodedBlock> result = new ArrayList<CodedBlock>();
        CodedSuperBlockMock mock = new CodedSuperBlockMock(
                new int[] { 32, 33, 32, 33, 34, 32, 34, 36, 32, 33, 32, 33, 34, 34, 32, 34, 35, 36, 38, 39, 38, 39, 36,
                        38 },
                new int[] { 8, 8, 9, 9, 8, 10, 10, 8, 12, 12, 13, 13, 12, 13, 14, 14, 14, 12, 12, 12, 13, 13, 14, 14 },
                new int[] { 2, 3, 0, 3, 6, 6, 6, 9, 3, 3, 3, 0, 5, 5, 6, 4, 4, 6, 2, 3, 3, 3, 6, 6 });
        mock.readSubPartition(miCol, miRow, 3, decoder, c, result);
        Assert.assertEquals(24, result.size());
    }
}
