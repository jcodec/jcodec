package org.jcodec.codecs.vp8;

import java.nio.ByteBuffer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class BooleanArithmeticDecoder {
        int bit_count; /* # of bits shifted out of value, at most 7 */
        ByteBuffer input;
        int offset; /* pointer to next compressed data byte */
        int range; /* always identical to encoder's range */
        int value; /* contains at least 24 significant bits */
        long callCounter=0;
        private String debugName;

        public BooleanArithmeticDecoder(ByteBuffer input, int offset) {
            this.input = input;
            this.offset = offset;
            initBoolDecoder();
        }

        void initBoolDecoder() {

            value = 0; /* value = first 16 input bits */

            // data.position(offset);
            value = (input.get() & 0xFF) << 8; // readUnsignedByte() << 8;
            // value = (data[offset]) << 8;
            offset++;

            range = 255; /* initial range is full */
            bit_count = 0; /* have not yet shifted out any bits */
        }

        public int decodeBit()  {
            return decodeBool(128);
        }

        public int decodeBool(int probability) {
            int bit = 0;
            int range = this.range;
            int value = this.value;
            int split = 1 + (((range - 1) * probability) >> 8);
            int bigsplit = (split << 8);

            this.callCounter++;
//            System.out.println();
//            System.out.println("this.range: " + this.range + " binary: " + Integer.toBinaryString(this.range));
//            System.out.println("split: " + split + " binary: " + Integer.toBinaryString(split));
//            System.out.println("SPLIT: " + bigsplit + " binary: " + Integer.toBinaryString(bigsplit));
//            System.out.println("value: " + value + " binary: " + Integer.toBinaryString(value));
            range = split;

            if (value >= bigsplit) {
                range = this.range - range;
                value = value - bigsplit;
                bit = 1;
            }

            int count = this.bit_count;
            int shift = leadingZeroCountInByte((byte)range);
            range <<= shift;
            value <<= shift;
            count -= shift;

            if (count <= 0) {
                value |= (input.get() & 0xFF) << (-count);
//                System.out.println("read value: " + value + " binary: " + Integer.toBinaryString(value));
                offset++;
                count += 8;
            }

            this.bit_count = count;
            this.value = value;
            this.range = range;
            return bit;
        }

        /*
         * Convenience function reads a "literal", that is, a "num_bits" wide unsigned value whose bits come high- to low-order, with each bit encoded at probability 128 (i.e., 1/2).
         */
        public int decodeInt(int sizeInBits) {
            int v = 0;
            while (sizeInBits-- > 0)
                v = (v << 1) | decodeBool(128);
            return v;
        }

        /* root: "0", "1" subtrees */
        /* "00" = 0th value, "01" = 1st value */
        /* "10" = 2nd value, "11" = 3rd value */
        /**
         *  
         * General formula in VP8 trees.
         * <ul>
         *  <li> if tree element is a positive number it is treated as index of the child elements <pre>tree[i] > 0</pre>
         *       <ul>
         *       <li> left child is assumed to have index <pre>i</pre> and value <pre>tree[i]</pre> </li>
         *       <li> right child is assumed to have index <pre>i+1</pre> and value <pre>tree[i+1]</pre></li>
         *       </ul>
         *  </li>
         *  <li> a negative tree value means a leaf node was reached and it's negated value should be returned <pre>-tree[i]</pre></li>
         * </ul>
         * 
         * Here's a real example of a tree coded according to this formula in VP8 spec.
         * <pre>
         * const tree_index mb_segment_tree [2 * (4-1)] =
         * // +-------+---+
         * // |       |   |
         *  { 2,  4, -0, -1, -2, -3 };
         * //     |           |   |
         * //     +-----------+---+
         * </pre>
         * 
         * If presented in hierarchical form this tree would look like:
         * <pre>
         * +---------------+
         * |      root     |
         * |     /    \    |
         * |    2      4   |
         * |   / \    / \  |
         * | -0  -1 -2  -3 |
         * +---------------+
         * <pre>
         * 
         * On the other hand probabilities are coded only for non-leaf nodes. 
         * Thus tree array has twice as many nodes as probabilities array
         * Consider (3>>1) == 1 == (2>>1), and (0>>1) == 0 == (1>>1)
         * Thus single probability element refers to single parent element in tree.
         * if (using that probability) a '0' is coded, algorithm goes to the left 
         * branch, correspondingly if '1' is coded, algorithm goes to
         * the right branch (see tree structure above).
         * 
         * The process is repeated until a negative tree element is found.
         *  
         */
        public int readTree(int tree[],int probability[]) {
            int i = 0; 

            /*
             * 1. pick corresponding probability probability[i >> 1]
             * 2. pick left or right branch from coded info decodeBool(probability)
             * 3. tree[i+decodedBool] get corresponding (left of right) value
             * 4. repeat until tree[i+decodedBool] is positive 
             */
            while ((i = tree[i + decodeBool(probability[i >> 1])]) > 0) {
            }
            return -i; /* negate the return value */

        }

        public int readTreeSkip(int t[], /* tree specification */
                int p[], /* corresponding interior node probabilities */
                int skip_branches) {
            int i = skip_branches * 2; /* begin at root */

            /* Descend tree until leaf is reached */
            while ((i = t[i + decodeBool(p[i >> 1])]) > 0) {
            }
            return -i; /* return value is negation of nonpositive index */

        }

        public void seek() {
            input.position(offset);
        }

        public String toString() {
            return "bc: " + value;
        }
        
        public static int getBitInBytes(byte[] bs, int i) {
            int byteIndex = i >> 3;
            int bitIndex = i & 0x07;
            return (bs[byteIndex] >> (0x07 - bitIndex)) & 0x01;
        }
        
        public static int getBitsInBytes(byte[] bytes, int idx, int len){
            int val = 0;
            for(int i=0;i<len;i++){
                val = (val << 1) | getBitInBytes(bytes, idx+i);
            }
            return val;
        }

        public static int leadingZeroCountInByte(byte b) {
            int i = b&0xFF;
            if (i>=128 || i == 0)
                return 0;
            
            return Integer.numberOfLeadingZeros(b)-24;
            /*
             * if-less alternative:
             * http://aggregate.ee.engr.uky.edu/MAGIC/#Leading Zero Count 
             */
        }
        
    }