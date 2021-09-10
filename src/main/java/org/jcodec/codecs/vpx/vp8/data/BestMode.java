package org.jcodec.codecs.vpx.vp8.data;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License.
 * 
 * The class is a direct java port of libvpx's
 * (https://github.com/webmproject/libvpx) relevant VP8 code with significant
 * java oriented refactoring.
 * 
 * @author The JCodec project
 * 
 */
public class BestMode {
    public long yrd = Long.MAX_VALUE;
    public long rd = Long.MAX_VALUE;
    public long intra_rd = Long.MAX_VALUE;
    public MBModeInfo mbmode = new MBModeInfo();
    public BModeInfo[] bmodes = new BModeInfo[16];
    public Partition_Info partition = new Partition_Info();

    public BestMode() {
        for (int i = 0; i < bmodes.length; i++) {
            bmodes[i] = new BModeInfo();
        }
    }
}
