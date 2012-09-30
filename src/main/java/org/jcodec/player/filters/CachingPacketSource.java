package org.jcodec.player.filters;

import org.jcodec.common.model.Rational;

public interface CachingPacketSource extends PacketSource {
    boolean canPlayThrough(Rational secs);
}
