package org.jcodec.movtool

import org.jcodec.common.Format
import org.jcodec.common.JCodecUtil
import org.jcodec.containers.mp4.MP4Util.parseFullMovie
import org.jcodec.containers.mp4.boxes.*
import org.jcodec.containers.mp4.boxes.MetaBox.Companion.createMetaBox
import org.jcodec.containers.mp4.boxes.MetaBox.Companion.fourcc
import org.jcodec.containers.mp4.boxes.NodeBox.Companion.findFirst
import org.jcodec.containers.mp4.boxes.NodeBox.Companion.findFirstPath
import org.jcodec.containers.mp4.boxes.UdtaBox.Companion.createUdtaBox
import org.jcodec.containers.mp4.boxes.UdtaMetaBox.Companion.createUdtaMetaBox
import java.io.File
import java.io.IOException
import java.util.*

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
class MetadataEditor(val source: File, val movieEditor: MovieEditor) {
    class MovieEditor(val keyedMeta: MutableMap<String, MetaValue>?, val itunesMeta: MutableMap<Int, MetaValue>?,
                      val udata: Map<Int, MetaValue>) {
        fun apply(movie: MovieBox) {
            var meta1 = findFirst(movie, fourcc()) as MetaBox?
            var meta2 = findFirstPath(movie, arrayOf("udta", fourcc())) as MetaBox?
            if (keyedMeta != null && keyedMeta.size > 0) {
                if (meta1 == null) {
                    meta1 = createMetaBox()
                    movie.add(meta1)
                }
                meta1.keyedMeta = keyedMeta.map { (k, v) -> k!! to v!! }.toMap()
            }
            if (itunesMeta != null && itunesMeta.size > 0) {
                var udta = findFirst(movie, "udta") as UdtaBox?
                if (meta2 == null) {
                    meta2 = createUdtaMetaBox()
                    if (udta == null) {
                        udta = createUdtaBox()
                        movie.add(udta)
                    }
                    udta.add(meta2)
                }
                meta2.itunesMeta = itunesMeta.orEmpty().map { (k, v) -> k!! to v!! }.toMap()
                udta!!.metadata = udata
            }
        }

        companion object {
            fun createFromMovie(moov: MovieBox?): MovieEditor {
                val keyedMeta = findFirst(moov, fourcc()) as MetaBox?
                val itunesMeta = findFirstPath(moov, arrayOf("udta", fourcc())) as MetaBox?
                val udtaBox = findFirst(moov, "udta") as UdtaBox?
                val keyedMeta1: MutableMap<String, MetaValue>? = keyedMeta?.keyedMeta?.toMutableMap() ?: HashMap<String, MetaValue>()
                val itunesMeta1: MutableMap<Int, MetaValue>? = itunesMeta?.itunesMeta?.toMutableMap() ?: HashMap<Int, MetaValue>()
                val udata = udtaBox?.metadata ?: HashMap()
                return MovieEditor(keyedMeta1, itunesMeta1, udata)
            }
        }

    }

    @Throws(IOException::class)
    fun save(fast: Boolean) {
        // In Javascript you cannot access a field from the outer type.
        val self = this
        val edit: MP4Edit = object : MP4Edit {
            override fun applyToFragment(mov: MovieBox?, fragmentBox: Array<MovieFragmentBox?>?) {}
            override fun apply(movie: MovieBox) {
                movieEditor.apply(movie)
            }
        }
        if (fast) {
            RelocateMP4Editor().modifyOrRelocate(source, edit)
        } else {
            ReplaceMP4Editor().modifyOrReplace(source, edit)
        }
    }

    val itunesMeta: MutableMap<Int, MetaValue>?
        get() = movieEditor.itunesMeta

    val keyedMeta: MutableMap<String, MetaValue>?
        get() = movieEditor.keyedMeta

    val udata: Map<Int, MetaValue>
        get() = movieEditor.udata

    companion object {
        @kotlin.jvm.JvmStatic
        @Throws(IOException::class)
        fun createFrom(f: File): MetadataEditor {
            val format = JCodecUtil.detectFormat(f)
            require(format == Format.MOV) { "Unsupported format: $format" }
            val movie = parseFullMovie(f)
            return MetadataEditor(f, MovieEditor.createFromMovie(movie!!.moov))
        }
    }

}