package org.jcodec.api.transcode

import org.jcodec.api.transcode.PixelStore.LoanerPicture
import org.jcodec.api.transcode.filters.ColorTransformFilter
import org.jcodec.common.AudioCodecMeta
import org.jcodec.common.IntArrayList
import org.jcodec.common.VideoCodecMeta
import org.jcodec.common.model.ColorSpace
import org.jcodec.common.model.Packet
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * Transcoder core.
 *
 * The simplest way to create a transcoder with default options:
 * Transcoder.newTranscoder(source, sink).create(); The source and the sink are
 * essential to the transcoder and must be provided.
 *
 * @author The JCodec project
 */
class Transcoder
/**
 * Use TranscoderBuilder (method newTranscoder below) to create a transcoder
 *
 * @param source
 * @param sink
 * @param videoCodecCopy
 * @param audioCodecCopy
 * @param extraFilters
 */ private constructor(private val sources: Array<Source>, private val sinks: Array<Sink>, private val videoMappings: Array<Mapping>, private val audioMappings: Array<Mapping>,
                        private val extraFilters: Array<MutableList<Filter>>, private val seekFrames: IntArray, private val maxFrames: IntArray) {

    private class Mapping(val source: Int, val copy: Boolean)

    private class Stream(private val sink: Sink, private val videoCopy: Boolean, private val audioCopy: Boolean, private val extraFilters: List<Filter>,
                         private val pixelStore: PixelStore) {
        private val videoQueue: LinkedList<VideoFrameWithPacket>
        private val audioQueue: LinkedList<AudioFrameWithPacket>
        private var filters: List<Filter>? = null
        private var videoCodecMeta: VideoCodecMeta? = null
        private var audioCodecMeta: AudioCodecMeta? = null
        private fun initColorTransform(sourceColor: ColorSpace, extraFilters: List<Filter>, sink: Sink): List<Filter> {
            var sourceColor = sourceColor
            val filters: MutableList<Filter> = ArrayList()
            for (filter in extraFilters) {
                val inputColor = filter.inputColor
                if (!sourceColor.matches(inputColor)) {
                    filters.add(ColorTransformFilter(inputColor))
                }
                filters.add(filter)
                if (filter.outputColor != ColorSpace.SAME) sourceColor = filter.outputColor
            }
            val inputColor = sink.inputColor
            if (inputColor != null && inputColor != sourceColor) filters.add(ColorTransformFilter(inputColor))
            return filters
        }

        @Throws(IOException::class)
        fun tryFlushQueues() {
            // Do we have enough audio
            if (videoQueue.size <= 0) return
            if (videoCopy && videoQueue.size < REORDER_LENGTH) return
            if (!hasLeadingAudio()) return
            var firstVideoFrame: VideoFrameWithPacket
            firstVideoFrame = videoQueue[0]
            // In case of video copy we need to reorder these frames back howe
            // they were in the stream. We use the original frame number for
            // this.
            if (videoCopy) {
                for (videoFrame in videoQueue) {
                    if (videoFrame.packet.getFrameNo() < firstVideoFrame.packet.getFrameNo()) firstVideoFrame = videoFrame
                }
            }

            // If we have .2s of leading audio, output it with the the current
            // video frame
            val aqSize = audioQueue.size
            for (af in 0 until aqSize) {
                val audioFrame = audioQueue[0]
                if (audioFrame.packet.ptsD >= firstVideoFrame.packet.ptsD + .2) break
                audioQueue.removeAt(0)
                if (audioCopy && sink is PacketSink) {
                    (sink as PacketSink).outputAudioPacket(audioFrame.packet, audioCodecMeta)
                } else {
                    sink.outputAudioFrame(audioFrame)
                }
            }
            videoQueue.remove(firstVideoFrame)
            if (videoCopy && sink is PacketSink) {
                (sink as PacketSink).outputVideoPacket(firstVideoFrame.packet, videoCodecMeta)
            } else {
                // Filtering the pixels
                val frame = filterFrame(firstVideoFrame)
                sink.outputVideoFrame(VideoFrameWithPacket(firstVideoFrame.packet, frame))
                pixelStore.putBack(frame)
            }
        }

        private fun filterFrame(firstVideoFrame: VideoFrameWithPacket): LoanerPicture {
            var frame = firstVideoFrame.frame
            for (filter in filters!!) {
                val old = frame!!
                frame = filter.filter(frame.picture, pixelStore)
                // Filters that don't change the original picture will
                // return null
                if (frame == null) {
                    frame = old
                } else {
                    pixelStore.putBack(old)
                }
            }
            return frame!!
        }

        @Throws(IOException::class)
        fun finalFlushQueues() {
            var lastVideoFrame: VideoFrameWithPacket? = null
            for (videoFrame in videoQueue) {
                if (lastVideoFrame == null || videoFrame.packet.ptsD >= lastVideoFrame.packet.ptsD) lastVideoFrame = videoFrame
            }
            if (lastVideoFrame != null) {
                for (audioFrame in audioQueue) {
                    // Don't output audio when there's no video any more
                    if (audioFrame.packet.ptsD > lastVideoFrame.packet.ptsD) break
                    if (audioCopy && sink is PacketSink) {
                        (sink as PacketSink).outputAudioPacket(audioFrame.packet, audioCodecMeta)
                    } else {
                        sink.outputAudioFrame(audioFrame)
                    }
                }
                for (videoFrame in videoQueue) {
                    if (videoFrame != null) {
                        if (videoCopy && sink is PacketSink) {
                            (sink as PacketSink).outputVideoPacket(videoFrame.packet, videoCodecMeta)
                        } else {
                            val frame = filterFrame(videoFrame)
                            sink.outputVideoFrame(VideoFrameWithPacket(videoFrame.packet, frame))
                            pixelStore.putBack(frame)
                        }
                    }
                }
            } else {
                for (audioFrame in audioQueue) {
                    if (audioCopy && sink is PacketSink) {
                        (sink as PacketSink).outputAudioPacket(audioFrame.packet, audioCodecMeta)
                    } else {
                        sink.outputAudioFrame(audioFrame)
                    }
                }
            }
        }

        fun addVideoPacket(videoFrame: VideoFrameWithPacket, meta: VideoCodecMeta?) {
            if (videoFrame.frame != null) pixelStore.retake(videoFrame.frame)
            videoQueue.add(videoFrame)
            videoCodecMeta = meta
            if (filters == null && videoFrame.frame != null) filters = initColorTransform(videoCodecMeta!!.color, extraFilters, sink)
        }

        fun addAudioPacket(videoFrame: AudioFrameWithPacket, meta: AudioCodecMeta?) {
            audioQueue.add(videoFrame)
            audioCodecMeta = meta
        }

        fun needsVideoFrame(): Boolean {
            if (videoQueue.size <= 0) return true
            return if (videoCopy && videoQueue.size < REORDER_LENGTH) true else false
        }

        fun hasLeadingAudio(): Boolean {
            val firstVideoFrame = videoQueue[0]
            for (audioFrame in audioQueue) {
                if (audioFrame.packet.ptsD >= firstVideoFrame.packet.ptsD + AUDIO_LEADING_TIME) {
                    return true
                }
            }
            return false
        }

        companion object {
            private const val AUDIO_LEADING_TIME = .2
            private const val REORDER_LENGTH = 5
        }

        init {
            videoQueue = LinkedList()
            audioQueue = LinkedList()
        }
    }

    @Throws(IOException::class)
    fun transcode() {
        val pixelStore: PixelStore = PixelStoreImpl()
        val videoStreams: Array<MutableList<Stream>> = Array(sources.size) { ArrayList<Stream>() }
        val audioStreams: Array<MutableList<Stream>> = Array(sources.size) { ArrayList<Stream>() }
        val decodeVideo = BooleanArray(sources.size)
        val decodeAudio = BooleanArray(sources.size)
        val finishedVideo = BooleanArray(sources.size)
        val finishedAudio = BooleanArray(sources.size)
        val allStreams = arrayOfNulls<Stream>(sinks.size)
        val videoFramesRead = IntArray(sources.size)
        for (i in sinks.indices) sinks[i].init(videoMappings[i].copy, audioMappings[i].copy)
        for (i in sources.indices) {
            sources[i].init(pixelStore)
            sources[i].seekFrames(seekFrames[i])
        }
        for (s in sinks.indices) {
            val stream = Stream(sinks[s], videoMappings[s].copy, audioMappings[s].copy, extraFilters[s],
                    pixelStore)
            allStreams[s] = stream
            if (sources[videoMappings[s].source].isVideo) {
                videoStreams[videoMappings[s].source].add(stream)
                if (!videoMappings[s].copy) decodeVideo[videoMappings[s].source] = true else {
                    val source = videoMappings[s].source
                    val codecPrivate = sources[source].videoCodecPrivate
                    sinks[s].setVideoCodecPrivate(codecPrivate)
                }
            } else {
                finishedVideo[videoMappings[s].source] = true
            }
            if (sources[audioMappings[s].source].isAudio) {
                audioStreams[audioMappings[s].source].add(stream)
                if (!audioMappings[s].copy) decodeAudio[audioMappings[s].source] = true else {
                    val source = audioMappings[s].source
                    val codecPrivate = sources[source].audioCodecPrivate
                    sinks[s].setAudioCodecPrivate(codecPrivate)
                }
            } else {
                finishedAudio[audioMappings[s].source] = true
            }
        }
        try {
            while (true) {
                // Read video and audio packet from each source and add it to
                // the appropriate queues
                for (s in sources.indices) {
                    val source = sources[s]

                    // See if we need to read a video frame, if out of the sinks
                    // still doesn't have enough audio don't read the next video
                    // frame just yet
                    var needsVideoFrame = !finishedVideo[s]
                    for (stream in videoStreams[s]) {
                        needsVideoFrame = needsVideoFrame and (stream.needsVideoFrame() || stream.hasLeadingAudio() || finishedAudio[s])
                    }
                    if (needsVideoFrame) {
                        // Read the next video frame and give it to all the
                        // streams that need it
                        var nextVideoFrame: VideoFrameWithPacket?
                        if (videoFramesRead[s] >= maxFrames[s]) {
                            nextVideoFrame = null
                            finishedVideo[s] = true
                        } else if (decodeVideo[s] || source !is PacketSource) {
                            nextVideoFrame = source.nextVideoFrame
                            if (nextVideoFrame == null) {
                                finishedVideo[s] = true
                            } else {
                                ++videoFramesRead[s]
                                printLegend(nextVideoFrame.packet.getFrameNo().toInt(), 0,
                                        nextVideoFrame.packet)
                            }
                        } else {
                            val packet = (source as PacketSource).inputVideoPacket()
                            if (packet == null) {
                                finishedVideo[s] = true
                            } else {
                                ++videoFramesRead[s]
                            }
                            nextVideoFrame = VideoFrameWithPacket(packet, null)
                        }

                        // The video source is empty, clear all video streams
                        // feeding from it, also locate and clean all these
                        // streams from audio feed
                        if (finishedVideo[s]) {
                            for (stream in videoStreams[s]) {
                                for (ss in audioStreams.indices) {
                                    audioStreams[ss].remove(stream)
                                }
                            }
                            videoStreams[s].clear()
                        }
                        if (nextVideoFrame != null) {
                            for (stream in videoStreams[s]) {
                                stream.addVideoPacket(nextVideoFrame, source.videoCodecMeta)
                            }
                            // De-reference the frame because it should be
                            // already in the queues by now, if nobody needs it
                            // it will just go away with this.
                            if (nextVideoFrame.frame != null) pixelStore.putBack(nextVideoFrame.frame!!)
                        }
                    }

                    // If no streams in need for this audio don't bother reading
                    if (!audioStreams[s].isEmpty()) {
                        // Read the next audio frame (or packet) and give it to all the streams that
                        // want it
                        var nextAudioFrame: AudioFrameWithPacket?
                        if (decodeAudio[s] || source !is PacketSource) {
                            nextAudioFrame = source.nextAudioFrame
                            if (nextAudioFrame == null) finishedAudio[s] = true
                        } else {
                            val packet = (source as PacketSource).inputAudioPacket()
                            if (packet == null) {
                                finishedAudio[s] = true
                                nextAudioFrame = null
                            } else {
                                nextAudioFrame = AudioFrameWithPacket(null, packet)
                            }
                        }
                        if (nextAudioFrame != null) {
                            for (stream in audioStreams[s]) {
                                stream.addAudioPacket(nextAudioFrame, source.audioCodecMeta)
                            }
                        }
                    } else {
                        finishedAudio[s] = true
                    }
                }

                // See if we can produce any output with the new frames just
                // read
                for (s in allStreams.indices) {
                    allStreams[s]!!.tryFlushQueues()
                }

                // Are we drained on all sources
                var allFinished = true
                for (s in sources.indices) {
                    allFinished = allFinished and (finishedVideo[s] and finishedAudio[s])
                }
                if (allFinished) break
            }
            // Finally flush everything that remains
            for (s in allStreams.indices) {
                allStreams[s]!!.finalFlushQueues()
            }
        } finally {
            for (i in sources.indices) sources[0].finish()
            for (i in sinks.indices) sinks[i].finish()
        }
    }

    private fun printLegend(frameNo: Int, maxFrames: Int, inVideoPacket: Packet) {
        if (frameNo % 100 == 0) print(String.format("[%6d]\r", frameNo))
    }

    class TranscoderBuilder {
        private val source: MutableList<Source>
        private val sink: MutableList<Sink>
        private val filters: MutableList<MutableList<Filter>>
        private val seekFrames: IntArrayList
        private val maxFrames: IntArrayList
        private val videoMappings: MutableList<Mapping>
        private val audioMappings: MutableList<Mapping>
        fun addFilter(sink: Int, filter: Filter): TranscoderBuilder {
            filters[sink].add(filter)
            return this
        }

        fun setSeekFrames(source: Int, seekFrames: Int): TranscoderBuilder {
            this.seekFrames[source] = seekFrames
            return this
        }

        fun setMaxFrames(source: Int, maxFrames: Int): TranscoderBuilder {
            this.maxFrames[source] = maxFrames
            return this
        }

        fun addSource(source: Source): TranscoderBuilder {
            this.source.add(source)
            seekFrames.add(0)
            maxFrames.add(Int.MAX_VALUE)
            return this
        }

        fun addSink(sink: Sink): TranscoderBuilder {
            this.sink.add(sink)
            videoMappings.add(Mapping(0, false))
            audioMappings.add(Mapping(0, false))
            filters.add(ArrayList())
            return this
        }

        fun setVideoMapping(src: Int, sink: Int, copy: Boolean): TranscoderBuilder {
            videoMappings[sink] = Mapping(src, copy)
            return this
        }

        fun setAudioMapping(src: Int, sink: Int, copy: Boolean): TranscoderBuilder {
            audioMappings[sink] = Mapping(src, copy)
            return this
        }

        fun create(): Transcoder {
            return Transcoder(source.toTypedArray(), sink.toTypedArray(),
                    videoMappings.toTypedArray(), audioMappings.toTypedArray(),
                    filters.toTypedArray(), seekFrames.toArray(), maxFrames.toArray())
        }

        init {
            source = ArrayList()
            sink = ArrayList()
            filters = ArrayList()
            seekFrames = IntArrayList(20)
            maxFrames = IntArrayList(20)
            videoMappings = ArrayList()
            audioMappings = ArrayList()
        }
    }

    companion object {
        const val REORDER_BUFFER_SIZE = 7

        @JvmStatic
        fun newTranscoder(): TranscoderBuilder {
            return TranscoderBuilder()
        }
    }

}