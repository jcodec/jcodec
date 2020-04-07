package org.jcodec.containers.dpx

import org.jcodec.common.StringUtils
import org.jcodec.common.io.IOUtils
import org.jcodec.common.io.NIOUtils
import org.jcodec.common.io.SeekableByteChannel
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.ReadableByteChannel
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class DPXReader(ch: SeekableByteChannel) {
    private val readBuf: ByteBuffer
    private val magic: Int
    private var eof = false
    fun parseMetadata(): DPXMetadata {
        // File information header
        val dpx = DPXMetadata()
        dpx.file = readFileInfo(readBuf)
        dpx.file!!.magic = magic

        // Image information header
        readBuf.position(IMAGEINFO_OFFSET)
        dpx.image = readImageInfoHeader(readBuf)

        //  Image source information header
        readBuf.position(IMAGESOURCE_OFFSET)
        dpx.imageSource = readImageSourceHeader(readBuf)

        // Motion-picture film information header
        readBuf.position(FILM_OFFSET)
        dpx.film = readFilmInformationHeader(readBuf)

        // Television information header
        readBuf.position(TVINFO_OFFSET)
        dpx.television = readTelevisionInfoHeader(readBuf)
        dpx.userId = readNullTermString(readBuf, 32)
        return dpx
    }

    @Throws(IOException::class)
    private fun initialRead(ch: ReadableByteChannel) {
        readBuf.clear()
        if (ch.read(readBuf) == -1) eof = true
        readBuf.flip()
    }

    companion object {
        private const val READ_BUFFER_SIZE = 2048 + 1024
        const val IMAGEINFO_OFFSET = 768
        const val IMAGESOURCE_OFFSET = 1408
        const val FILM_OFFSET = 1664
        const val TVINFO_OFFSET = 1920
        const val SDPX = 0x53445058
        private fun readFileInfo(bb: ByteBuffer): FileHeader {
            val h = FileHeader()
            h.imageOffset = bb.int
            h.version = readNullTermString(bb, 8)
            h.filesize = bb.int
            h.ditto = bb.int
            h.genericHeaderLength = bb.int
            h.industryHeaderLength = bb.int
            h.userHeaderLength = bb.int
            h.filename = readNullTermString(bb, 100)
            h.created = tryParseISO8601Date(readNullTermString(bb, 24))
            h.creator = readNullTermString(bb, 100)
            h.projectName = readNullTermString(bb, 200)
            h.copyright = readNullTermString(bb, 200)
            h.encKey = bb.int
            return h
        }

        @JvmStatic
        fun tryParseISO8601Date(dateString: String): Date? {
            /*
        S268M

        3.4 creation date/time:
        Defined as yyyy:mm:dd:hh:mm:ssLTZ, formatted according to ISO 8601.
        “LTZ” means “Local Time Zone;” format is:
        LTZ = Z (time zone = UTC), or
        LTZ = +/–hh, or
        LTZ = +/–hhmm (local time is offset from UTC)

        Few people knew what "LTZ" meant, or how it was to be encoded.
        This has been corrected by citing ISO 8601 practice.
         */
            var dateString = dateString
            if (StringUtils.isEmpty(dateString)) {
                return null
            }
            val noTZ = "yyyy:MM:dd:HH:mm:ss"
            if (dateString.length == noTZ.length) {
                return date(dateString, noTZ)
            } else if (dateString.length == noTZ.length + 4) {
                // ':+/–hh'
                dateString = dateString + "00"
            }

            // ':+/–hhmm'
            return date(dateString, "yyyy:MM:dd:HH:mm:ss:Z")
        }

        private fun date(dateString: String, dateFormat: String): Date? {
            val format = SimpleDateFormat(dateFormat, Locale.US)
            return try {
                format.parse(dateString)
            } catch (e: ParseException) {
                null
            }
        }

        private fun readNullTermString(bb: ByteBuffer, length: Int): String {
            val b = ByteBuffer.allocate(length)
            bb[b.array(), 0, length]
            return NIOUtils.readNullTermString(b)
        }

        @Throws(IOException::class)
        fun readFile(file: File?): DPXReader {
            val _in: SeekableByteChannel = NIOUtils.readableChannel(file)
            return try {
                DPXReader(_in)
            } finally {
                IOUtils.closeQuietly(_in)
            }
        }

        private fun readTelevisionInfoHeader(r: ByteBuffer): TelevisionHeader {
            val h = TelevisionHeader()
            h.timecode = r.int
            h.userBits = r.int
            h.interlace = r.get()
            h.filedNumber = r.get()
            h.videoSignalStarted = r.get()
            h.zero = r.get()
            h.horSamplingRateHz = r.int
            h.vertSampleRateHz = r.int
            h.frameRate = r.int
            h.timeOffset = r.int
            h.gamma = r.int
            h.blackLevel = r.int
            h.blackGain = r.int
            h.breakpoint = r.int
            h.referenceWhiteLevel = r.int
            h.integrationTime = r.int
            //        h.reserved = readBB(r, 76).array();
            return h
        }

        private fun readFilmInformationHeader(r: ByteBuffer): FilmHeader {
            val h = FilmHeader()
            h.idCode = readNullTermString(r, 2)
            h.type = readNullTermString(r, 2)
            h.offset = readNullTermString(r, 2)
            h.prefix = readNullTermString(r, 6) // Prefix (6 digits from film edge code)
            h.count = readNullTermString(r, 4) // Count (4 digits from film edge code)
            h.format = readNullTermString(r, 32)
            //        h.reserved = readBB(r, 104).array(); // Reserved for future use
            return h
        }

        private fun readImageSourceHeader(r: ByteBuffer): ImageSourceHeader {
            val h = ImageSourceHeader()
            h.xOffset = r.int
            h.yOffset = r.int
            h.xCenter = r.float
            h.yCenter = r.float
            h.xOriginal = r.int
            h.yOriginal = r.int
            h.sourceImageFilename = readNullTermString(r, 100)
            h.sourceImageDate = tryParseISO8601Date(readNullTermString(r, 24))
            h.deviceName = readNullTermString(r, 32)
            h.deviceSerial = readNullTermString(r, 32)
            h.borderValidity = shortArrayOf(r.short, r.short, r.short, r.short)
            h.aspectRatio = intArrayOf(r.int, r.int)
            return h
        }

        private fun readImageInfoHeader(r: ByteBuffer): ImageHeader {
            val h = ImageHeader()
            // offset = 768
            h.orientation = r.short
            h.numberOfImageElements = r.short
            h.pixelsPerLine = r.int
            h.linesPerImageElement = r.int
            h.imageElement1 = ImageElement()

            // offset = 780
            h.imageElement1.dataSign = r.int
            return h
        }
    }

    init {
        readBuf = ByteBuffer.allocate(READ_BUFFER_SIZE)
        initialRead(ch)
        magic = readBuf.int

        /*
        SMPTE 268M spec. Page 3. From Magic number definition:
        Programs reading DPX files should use the first four bytes to determine the byte order of the file.
        The first four bytes will be S, D, P, X if the byte order is most significant byte first,
        or X, P, D, S if the byte order is least significant byte first.
         */if (magic == SDPX) {
            readBuf.order(ByteOrder.BIG_ENDIAN)
        } else {
            readBuf.order(ByteOrder.LITTLE_ENDIAN)
        }
    }
}