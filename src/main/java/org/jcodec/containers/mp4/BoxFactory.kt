package org.jcodec.containers.mp4

import org.jcodec.containers.mp4.boxes.*
import org.jcodec.containers.mp4.boxes.Box.LeafBox
import org.jcodec.platform.Platform

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * Default box factory
 *
 * @author The JCodec project
 */
class BoxFactory(private val boxes: Boxes) : IBoxFactory {
    override fun newBox(header: Header): Box {
        val claz = boxes.toClass(header.fourcc!!) ?: return LeafBox(header)
        val box = Platform.newInstance(claz, arrayOf<Any>(header))!!
        if (box is NodeBox) {
            when (box) {
                is SampleDescriptionBox -> box.setFactory(sample)
                is VideoSampleEntry -> box.setFactory(video)
                is AudioSampleEntry -> box.setFactory(audio)
                is TimecodeSampleEntry -> box.setFactory(timecode)
                is MetaDataSampleEntry -> box.setFactory(metadata)
                is DataRefBox -> box.setFactory(data)
                is WaveExtension -> box.setFactory(waveext)
                is KeysBox -> {
                    //keys box has its own box factory
                }
                else -> box.setFactory(this)
            }
        }
        return box
    }

    companion object {
        val default: IBoxFactory = BoxFactory(DefaultBoxes())
        private val audio: IBoxFactory = BoxFactory(AudioBoxes())
        private val data: IBoxFactory = BoxFactory(DataBoxes())
        private val sample: IBoxFactory = BoxFactory(SampleBoxes())
        private val timecode: IBoxFactory = BoxFactory(TimecodeBoxes())
        private val video: IBoxFactory = BoxFactory(VideoBoxes())
        private val waveext: IBoxFactory = BoxFactory(WaveExtBoxes())
        private val metadata: IBoxFactory = BoxFactory(MetaDataBoxes())
    }

}