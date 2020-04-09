package org.jcodec.api.transcode

import org.jcodec.common.model.AudioBuffer
import org.jcodec.common.model.Packet

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
class AudioFrameWithPacket(val audio: AudioBuffer?, val packet: Packet)