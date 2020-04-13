package org.jcodec.containers.imgseq

import org.jcodec.common.*
import org.jcodec.common.io.NIOUtils
import org.jcodec.common.logging.Logger
import org.jcodec.common.model.Packet
import org.jcodec.common.model.Packet.FrameType
import java.io.File
import java.io.IOException
import java.util.*

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * A demuxer that reads image files out of a folder.
 *
 * Supports both sequences starting with 0 and 1 index.
 *
 * @author Stanislav Vitvitskyy
 */
class ImageSequenceDemuxer(private val namePattern: String, private val maxFrames: Int) : Demuxer, DemuxerTrack {
    private var frameNo = 0
    private var curFrame: Packet? = null
    private var codec: Codec? = null
    private var _maxAvailableFrame: Int = -1
    private var prevName: String? = null

    @Throws(IOException::class)
    override fun close() {
    }

    override fun getTracks(): List<DemuxerTrack> {
        val tracks = ArrayList<DemuxerTrack>()
        tracks.add(this)
        return tracks
    }

    override fun getVideoTracks(): List<DemuxerTrack> {
        return tracks
    }

    override fun getAudioTracks(): List<DemuxerTrack> {
        return ArrayList()
    }

    @Throws(IOException::class)
    override fun nextFrame(): Packet? {
        return try {
            curFrame
        } finally {
            curFrame = loadFrame()
        }
    }

    @Throws(IOException::class)
    private fun loadFrame(): Packet? {
        if (frameNo > maxFrames) {
            return null
        }
        var file: File? = null
        do {
            val name = String.format(namePattern, frameNo)
            // In case the name doesn't contain placeholders, to prevent infinitely
            // looping around the same file.
            if (name == prevName) {
                return null
            }
            prevName = name
            file = File(name)
            if (file.exists() || frameNo > 0) break
            frameNo++
        } while (frameNo < 2)
        if (file == null || !file.exists()) return null
        val ret = Packet(NIOUtils.fetchFromFile(file), frameNo.toLong(), VIDEO_FPS, 1, frameNo.toLong(), FrameType.KEY, null, frameNo)
        ++frameNo
        return ret
    }
    // movie
    /**
     * Finds maximum frame of a sequence by bisecting the range.
     *
     * Performs at max at max 48 Stat calls ( 2*log2(MAX_MAX) ).
     *
     * @return
     */
    fun getMaxAvailableFrame(): Int {
        if (_maxAvailableFrame == -1) {
            var firstPoint = 0
            var i = MAX_MAX
            while (i > 0) {
                if (File(String.format(namePattern, i)).exists()) {
                    firstPoint = i
                    break
                }
                i /= 2
            }
            var pos = firstPoint
            var interv = firstPoint / 2
            while (interv > 1) {
                if (File(String.format(namePattern, pos + interv)).exists()) {
                    pos += interv
                }
                interv /= 2
            }
            _maxAvailableFrame = pos
            Logger.info("Max frame found: $_maxAvailableFrame")
        }
        return Math.min(_maxAvailableFrame, maxFrames)
    }

    override fun getMeta(): DemuxerTrackMeta {
        val durationFrames = getMaxAvailableFrame()
        return DemuxerTrackMeta(TrackType.VIDEO, codec, ((durationFrames + 1) * VIDEO_FPS).toDouble(), null, durationFrames + 1,
                null, null, null)
    }

    companion object {
        private const val VIDEO_FPS = 25
        private const val MAX_MAX = 60 * 60 * 60 * 24 // Longest possible
    }

    init {
        curFrame = loadFrame()!!
        // codec = JCodecUtil.detectDecoder(curFrame.getData());
        val lowerCase = namePattern.toLowerCase()
        if (lowerCase.endsWith(".png")) {
            codec = Codec.PNG
        } else if (lowerCase.endsWith(".jpg") || lowerCase.endsWith(".jpeg")) {
            codec = Codec.JPEG
        }
    }
}