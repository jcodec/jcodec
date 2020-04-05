package org.jcodec.containers.mp4

import org.jcodec.api.JCodecException
import org.jcodec.common.XMLMapper
import org.jcodec.common.XMLMapper.TypeHandler
import org.jcodec.common.model.Rational
import java.io.IOException
import java.net.URL
import java.util.*
import javax.xml.stream.XMLStreamException

object MPDModel {
    @Throws(IOException::class, JCodecException::class)
    fun parse(url: URL): MPD {
        return try {
            XMLMapper.map(url.openStream(), MPD::class.java, TypeHandler1())
        } catch (e: ReflectiveOperationException) {
            throw JCodecException(e)
        } catch (e: XMLStreamException) {
            throw JCodecException(e)
        } catch (e: Exception) {
            throw JCodecException(e)
        }
    }

    class Time(var sec: Double) {

        companion object {
            fun parseTime(arg: String): Time? {
                val charArray = arg.toCharArray()
                if (charArray.size < 3 || charArray[0] != 'P' || charArray[1] != 'T') return null
                var tokenStart = 2
                var sec = 0.0
                for (i in 2 until charArray.size) {
                    if (charArray[i] == 'S' || charArray[i] == 'M' || charArray[i] == 'H' && tokenStart != i) {
                        val token = String(charArray, tokenStart, i - tokenStart)
                        val parseDouble = token.toDouble()
                        tokenStart = i + 1
                        when (charArray[i]) {
                            'S' -> sec += parseDouble
                            'M' -> sec += parseDouble * 60
                            'H' -> sec += parseDouble * 3600
                        }
                    }
                }
                return Time(sec)
            }
        }

    }

    class TypeHandler1 : TypeHandler {
        override fun parse(value: String, clz: Class<*>): Any? {
            return if (clz == Time::class.java) {
                Time.parseTime(value)!!
            } else null
        }

        override fun supports(clz: Class<*>): Boolean {
            return clz == Time::class.java
        }
    }

    class MPD {
        var mediaPresentationDuration: Time? = null
        var periods: MutableList<Period> = LinkedList()
        fun addPeriod(arg: Period) {
            periods.add(arg)
        }
    }

    class Period {
        var start: Time? = null
        var duration: Time? = null
        var adaptationSets: MutableList<AdaptationSet> = LinkedList()
        fun addAdaptationSet(arg: AdaptationSet) {
            adaptationSets.add(arg)
        }
    }

    class AdaptationSet {
        var segmentAlignment = false
        var maxWidth = 0
        var maxHeight = 0
        var maxFrameRate: Rational? = null
        var par: Rational? = null
        var segmentTemplate: SegmentTemplate? = null
        var representations: MutableList<Representation> = LinkedList()
        fun addRepresentation(arg: Representation) {
            representations.add(arg)
        }
    }

    class SegmentTemplate {
        var timescale = 0
        var duration = 0
        var media: String? = null
        var startNumber = 0
        var initialization: String? = null
    }

    class Representation {
        var id: String? = null
        var mimeType: String? = null
        var codecs: String? = null
        var width = 0
        var height = 0
        var frameRate: Rational? = null
        var sar: Rational? = null
        var startWithSAP = 0
        var bandwidth = 0
        var audioSamplingRate = 0
        var baseURL: String? = null
        var segmentTemplate: SegmentTemplate? = null
        var segmentBase: SegmentBase? = null
        var segmentList: SegmentList? = null
    }

    class SegmentBase {
        var indexRange // "0-834"
                : String? = null
    }

    class SegmentList {
        var duration = 0
        var initialization: Initialization? = null
        var segmentUrls: MutableList<SegmentURL> = LinkedList()
        fun addSegmentURL(arg: SegmentURL) {
            segmentUrls.add(arg)
        }
    }

    class Initialization {
        var sourceURL: String? = null
    }

    class SegmentURL {
        var media: String? = null
    }
}