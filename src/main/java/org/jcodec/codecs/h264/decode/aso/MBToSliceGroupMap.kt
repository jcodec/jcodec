package org.jcodec.codecs.h264.decode.aso

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * Contains a mapping of macroblocks to slice groups. Groups is an array of
 * group slice group indices having a dimension picWidthInMbs x picHeightInMbs
 *
 * @author The JCodec project
 */
class MBToSliceGroupMap(val groups: IntArray, val indices: IntArray, val inverse: Array<IntArray?>)