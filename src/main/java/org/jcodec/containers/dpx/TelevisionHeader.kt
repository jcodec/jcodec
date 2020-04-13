package org.jcodec.containers.dpx

class TelevisionHeader {
    var timecode = 0
    var userBits = 0
    var interlace // Interlace (0 = noninterlaced; 1 = 2:1 interlace)
            : Byte = 0
    var filedNumber: Byte = 0
    var videoSignalStarted: Byte = 0
    var zero: Byte = 0
    var horSamplingRateHz = 0
    var vertSampleRateHz = 0
    var frameRate = 0
    var timeOffset = 0
    var gamma = 0
    var blackLevel = 0
    var blackGain = 0
    var breakpoint = 0
    var referenceWhiteLevel = 0
    var integrationTime = 0 //        public byte[] reserved;
}