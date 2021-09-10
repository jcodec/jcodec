package org.jcodec.codecs.vpx.vp8.data;

import java.util.EnumSet;

import org.jcodec.codecs.vpx.vp8.enums.InitFlags;

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
public class CodecPriv {
    public EnumSet<InitFlags> init_flags = EnumSet.noneOf(InitFlags.class);

    public EncData enc=new EncData();
    public CodecAlgPRiv priv;
}
