package org.jcodec.containers.mxf

import org.jcodec.containers.mxf.model.*
import java.nio.ByteBuffer
import java.util.*

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 *
 * @author The JCodec project
 */
object MXFConst {
    @JvmField
    val HEADER_PARTITION_KLV = UL.newUL("06.0e.2b.34.02.05.01.01.0d.01.02.01.01.02")
    val INDEX_KLV = UL.newUL("06.0E.2B.34.02.53.01.01.0d.01.02.01.01.10.01.00")
    val GENERIC_DESCRIPTOR_KLV = UL.newUL("06.0E.2B.34.02.53.01.01.0d.01.01.01.01.01")
    @JvmField
    var klMetadata: MutableMap<UL, Class<out MXFMetadata>> = HashMap()

    class KLVFill(ul: UL?) : MXFMetadata(ul!!) {
        override fun readBuf(bb: ByteBuffer) {}
    }

    init {
        klMetadata[UL.newUL("06.0E.2B.34.02.53.01.01.0d.01.01.01.01.01.18.00")] = ContentStorage::class.java
        klMetadata[UL.newUL("06.0E.2B.34.02.53.01.01.0d.01.01.01.01.01.37.00")] = SourcePackage::class.java
        klMetadata[UL.newUL("06.0E.2B.34.02.53.01.01.0d.01.01.01.01.01.0F.00")] = Sequence::class.java
        klMetadata[UL.newUL("06.0E.2B.34.02.53.01.01.0D.01.01.01.01.01.2F.00")] = Preface::class.java
        klMetadata[UL.newUL("06.0e.2b.34.02.53.01.01.0d.01.01.01.01.01.30.00")] = Identification::class.java
        klMetadata[UL.newUL("06.0E.2B.34.02.53.01.01.0d.01.01.01.01.01.11.00")] = SourceClip::class.java
        klMetadata[UL.newUL("06.0e.2b.34.02.53.01.01.0d.01.01.01.01.01.23.00")] = EssenceContainerData::class.java
        klMetadata[UL.newUL("06.0E.2B.34.02.53.01.01.0d.01.01.01.01.01.3A.00")] = TimelineTrack::class.java
        klMetadata[UL.newUL("06.0E.2B.34.02.53.01.01.0d.01.01.01.01.01.3B.00")] = TimelineTrack::class.java
        klMetadata[UL.newUL("06.0E.2B.34.02.53.01.01.0d.01.01.01.01.01.36.00")] = MaterialPackage::class.java
        klMetadata[UL.newUL("06.0E.2B.34.02.53.01.01.0d.01.02.01.01.10.01.00")] = IndexSegment::class.java
        klMetadata[UL.newUL("06.0E.2B.34.02.53.01.01.0d.01.01.01.01.01.44.00")] = GenericDescriptor::class.java
        klMetadata[UL.newUL("06.0e.2b.34.02.53.01.01.0d.01.01.01.01.01.5b.00")] = GenericDataEssenceDescriptor::class.java
        klMetadata[UL.newUL("06.0e.2b.34.02.53.01.01.0d.01.01.01.01.01.5b.00")] = GenericDataEssenceDescriptor::class.java
        klMetadata[UL.newUL("06.0e.2b.34.02.53.01.01.0d.01.01.01.01.01.5c.00")] = GenericDataEssenceDescriptor::class.java
        klMetadata[UL.newUL("06.0e.2b.34.02.53.01.01.0d.01.01.01.01.01.43.00")] = GenericDataEssenceDescriptor::class.java
        klMetadata[UL.newUL("06.0E.2B.34.02.53.01.01.0d.01.01.01.01.01.42.00")] = GenericSoundEssenceDescriptor::class.java
        klMetadata[UL.newUL("06.0E.2B.34.02.53.01.01.0d.01.01.01.01.01.28.00")] = CDCIEssenceDescriptor::class.java
        klMetadata[UL.newUL("06.0E.2B.34.02.53.01.01.0d.01.01.01.01.01.29.00")] = RGBAEssenceDescriptor::class.java
        klMetadata[UL.newUL("06.0E.2B.34.02.53.01.01.0d.01.01.01.01.01.51.00")] = MPEG2VideoDescriptor::class.java
        klMetadata[UL.newUL("06.0E.2B.34.02.53.01.01.0d.01.01.01.01.01.48.00")] = WaveAudioDescriptor::class.java
        klMetadata[UL.newUL("06.0e.2b.34.02.53.01.01.0d.01.01.01.01.01.25.00")] = FileDescriptor::class.java
        klMetadata[UL.newUL("06.0e.2b.34.02.53.01.01.0d.01.01.01.01.01.27.00")] = GenericPictureEssenceDescriptor::class.java
        klMetadata[UL.newUL("06.0E.2B.34.02.53.01.01.0d.01.01.01.01.01.47.00")] = AES3PCMDescriptor::class.java
        klMetadata[UL.newUL("06.0E.2B.34.02.05.01.01.0d.01.02.01.01.05.01.00")] = MXFPartitionPack::class.java
        klMetadata[UL.newUL("06.0E.2B.34.02.05.01.01.0d.01.02.01.01.02.01.00")] = MXFPartitionPack::class.java
        klMetadata[UL.newUL("06.0E.2B.34.02.05.01.01.0d.01.02.01.01.02.02.00")] = MXFPartitionPack::class.java
        klMetadata[UL.newUL("06.0E.2B.34.02.05.01.01.0d.01.02.01.01.02.03.00")] = MXFPartitionPack::class.java
        klMetadata[UL.newUL("06.0E.2B.34.02.05.01.01.0d.01.02.01.01.02.04.00")] = MXFPartitionPack::class.java
        klMetadata[UL.newUL("06.0E.2B.34.02.05.01.01.0d.01.02.01.01.03.01.00")] = MXFPartitionPack::class.java
        klMetadata[UL.newUL("06.0E.2B.34.02.05.01.01.0d.01.02.01.01.03.02.00")] = MXFPartitionPack::class.java
        klMetadata[UL.newUL("06.0E.2B.34.02.05.01.01.0d.01.02.01.01.03.03.00")] = MXFPartitionPack::class.java
        klMetadata[UL.newUL("06.0E.2B.34.02.05.01.01.0d.01.02.01.01.03.04.00")] = MXFPartitionPack::class.java
        klMetadata[UL.newUL("06.0E.2B.34.02.05.01.01.0d.01.02.01.01.04.02.00")] = MXFPartitionPack::class.java
        klMetadata[UL.newUL("06.0E.2B.34.02.05.01.01.0d.01.02.01.01.04.04.00")] = MXFPartitionPack::class.java
        klMetadata[UL.newUL("06.0E.2B.34.02.53.01.01.0D.01.01.01.01.01.14.00")] = TimecodeComponent::class.java
        klMetadata[UL.newUL("06.0E.2B.34.01.01.01.02.03.01.02.10.01.00.00.00")] = KLVFill::class.java
        klMetadata[UL.newUL("06.0e.2b.34.02.53.01.01.0d.01.01.01.01.01.5a.00")] = J2KPictureDescriptor::class.java
    }
}