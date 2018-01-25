package org.jcodec.common;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * H.264 encoder. Save/restore interface. Encoding process usually updates the
 * context of decoding inplace so that the next coding unit has the context
 * ready. However rate control algorithm may require that the current macroblock
 * is redone with different choice of encoding parameters, in this case all the
 * contexts need to be reset to their previous states.
 * 
 * This interface is implemented by the components that support the function of
 * clean restoring to the state at the time 'save' is called.
 * 
 * @author Jay Codec
 * 
 */
public interface SaveRestore {

    void save();

    void restore();

}
