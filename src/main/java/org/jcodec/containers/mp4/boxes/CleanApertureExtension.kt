package org.jcodec.containers.mp4.boxes

import java.nio.ByteBuffer

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
class CleanApertureExtension(header: Header) : Box(header) {
    private var vertOffsetDenominator = 0
    private var vertOffsetNumerator = 0
    private var horizOffsetDenominator = 0
    private var horizOffsetNumerator = 0
    private var apertureHeightDenominator = 0
    private var apertureHeightNumerator = 0
    private var apertureWidthDenominator = 0
    private var apertureWidthNumerator = 0
    override fun parse(`is`: ByteBuffer) {
        apertureWidthNumerator = `is`.int
        apertureWidthDenominator = `is`.int
        apertureHeightNumerator = `is`.int
        apertureHeightDenominator = `is`.int
        horizOffsetNumerator = `is`.int
        horizOffsetDenominator = `is`.int
        vertOffsetNumerator = `is`.int
        vertOffsetDenominator = `is`.int
    }

    public override fun doWrite(out: ByteBuffer) {
        out.putInt(apertureWidthNumerator)
        out.putInt(apertureWidthDenominator)
        out.putInt(apertureHeightNumerator)
        out.putInt(apertureHeightDenominator)
        out.putInt(horizOffsetNumerator)
        out.putInt(horizOffsetDenominator)
        out.putInt(vertOffsetNumerator)
        out.putInt(vertOffsetDenominator)
    }

    override fun estimateSize(): Int {
        return 32 + 8
    }

    companion object {
        fun createCleanApertureExtension(apertureWidthN: Int, apertureWidthD: Int,
                                         apertureHeightN: Int, apertureHeightD: Int, horizOffN: Int, horizOffD: Int, vertOffN: Int, vertOffD: Int): CleanApertureExtension {
            val clap = CleanApertureExtension(Header(fourcc()))
            clap.apertureWidthNumerator = apertureWidthN
            clap.apertureWidthDenominator = apertureWidthD
            clap.apertureHeightNumerator = apertureHeightN
            clap.apertureHeightDenominator = apertureHeightD
            clap.horizOffsetNumerator = horizOffN
            clap.horizOffsetDenominator = horizOffD
            clap.vertOffsetNumerator = vertOffN
            clap.vertOffsetDenominator = vertOffD
            return clap
        }

        @JvmStatic
        fun fourcc(): String {
            return "clap"
        }
    }
}