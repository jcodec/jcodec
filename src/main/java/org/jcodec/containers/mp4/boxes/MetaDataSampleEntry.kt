package org.jcodec.containers.mp4.boxes

open class MetaDataSampleEntry(header: Header) : SampleEntry(header) {
    override var drefInd: Short = 0
}