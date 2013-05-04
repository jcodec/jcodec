jcodec - a pure java implementation of video/audio codecs.

# About 

JCodec is a library implementing a set of popular video and audio codecs. Currently JCodec supports:

* Video
    * H.264 Main profile decoder ( CAVLC/CABAC, I/P/B frames );
    * H.264 Baseline profile encoder ( CAVLC, I-frame only );
    * MPEG 1/2 decoder ( I/P/B frames, interlace );
    * Apple ProRes decoder;
    * Apple ProRes encoder;
    * JPEG decoder;

* Audio
    * SMPTE 302M decoder;

* Wrappers ( muxers, demuxers, formats ):
    * MP4 ( ISO BMF, Apple QuickTime ) de-muxer;
    * MP4 ( ISO BMF, Apple QuickTime ) muxer;
    * MKV ( Matroska ) de-muxer;
    * MKV ( Matroska ) muxer;
    * MPEG PS ( Program Stream ) demuxer;
    * MPEG TS ( Transport Stream ) demuxer;

JCodec also features a pure java video player that works as a web-browser applet. The player uses a specially developed streaming technology 'JCodec Streaming'.

JCodec is free software distributed under FreeBSD License.

# Future development

 Those are just some of the things JCodec dev team is planning to work on during the next year:

* Video
    * Improve H.264 encoder: add P frames, CABAC, rate control;
    * Performance optimize H.264 decoder;
    * J2K decoder;
    * Support high profiles in H.264;
* Audio
    * MP3 decoder;
    * MP3 encoder;
    * AAC encoder;
* Wrappers
    * MXF demuxer;
* Player
    * H.264 support, GOP support, stability, remove signing, JAAD integration;

# Getting started

You can get JCodec automatically with maven. For this just add below snippet to your pom.xml .

```xml
<dependency>
    <groupId>org.jcodec</groupId>
    <artifactId>jcodec</artifactId>
    <version>0.1.0</version>
</dependency>
```
OR download it from here:
* [JCodec 0.1.0 JAR](http://jcodec.org/downloads/jcodec-0.1.0.jar), [GPG Sign](http://jcodec.org/downloads/jcodec-0.1.0.jar.asc), [POM](http://jcodec.org/downloads/jcodec-0.1.0.pom)
* [JCodec 0.1.0 JAR with deps](http://jcodec.org/downloads/jcodec-0.1.0-uberjar.jar)

There is virtually no documentation right now but the plan is to catch up on this so stay tuned.

# Contact

Feel free to communicate any questions or concerns to us. Dev team email: jcodecproject@gmail.com