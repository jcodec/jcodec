package org.jcodec.codecs.mpeg4.es;

public interface IDescriptorFactory {
    Class<? extends Descriptor> byTag(int tag);

}