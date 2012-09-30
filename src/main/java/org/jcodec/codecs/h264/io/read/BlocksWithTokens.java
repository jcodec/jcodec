package org.jcodec.codecs.h264.io.read;

import org.jcodec.codecs.h264.io.model.CoeffToken;
import org.jcodec.codecs.h264.io.model.ResidualBlock;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author Jay Codec
 * 
 */
class BlocksWithTokens {
    private ResidualBlock[] block;
    private CoeffToken[] token;

    public BlocksWithTokens(ResidualBlock[] block, CoeffToken[] token) {
        this.block = block;
        this.token = token;
    }

    public ResidualBlock[] getBlock() {
        return block;
    }

    public void setBlock(ResidualBlock[] block) {
        this.block = block;
    }

    public CoeffToken[] getToken() {
        return token;
    }

    public void setToken(CoeffToken[] token) {
        this.token = token;
    }

}
