package org.jcodec.containers.mp4.boxes

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * A box storing a list of synch samples
 *
 * @author The JCodec project
 */
class PartialSyncSamplesBox(header: Header) : SyncSamplesBox(header) {
    companion object {
        const val STPS = "stps"
    }
}