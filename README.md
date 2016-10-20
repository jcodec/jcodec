jcodec - a pure java implementation of video/audio codecs.

# About 

JCodec is a library implementing a set of popular video and audio codecs. Currently JCodec supports:

* Video
    * H.264 Main profile decoder ( CAVLC/CABAC, I/P/B frames );
    * H.264 Baseline profile encoder ( CAVLC, I-frames only, P-frames as of version 0.2 );
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

JCodec is free software distributed under FreeBSD License.

# Future development

 Those are just some of the things JCodec dev team is planning to work on:

* Video
    * Improve H.264 encoder: CABAC, rate control;
    * Performance optimize H.264 decoder;
    * Implement H.264 encoder on RenderScript;
    * Native optiomizations for decoders and encoders;
* Audio
    * AAC encoder;

# Getting started

You can get JCodec automatically with maven. For this just add below snippet to your pom.xml .

```xml
<dependency>
    <groupId>org.jcodec</groupId>
    <artifactId>jcodec-javase</artifactId>
    <version>0.1.9</version>
</dependency>
```
OR download it from here (you will need both jars):
* [JCodec 0.1.9 JAR](http://jcodec.org/downloads/jcodec-0.1.9.jar), [GPG Sign](http://jcodec.org/downloads/jcodec-0.1.9.jar.asc), [POM](http://jcodec.org/downloads/jcodec-0.1.9.pom)
* [JCodec JavaSE 0.1.9 JAR](http://jcodec.org/downloads/jcodec-javase-0.1.9.jar), [GPG Sign](http://jcodec.org/downloads/jcodec-javase-0.1.9.jar.asc), [POM](http://jcodec.org/downloads/jcodec-javase-0.1.9.pom)

There is virtually no documentation right now but the plan is to catch up on this so stay tuned. stackoverflow.com contains quite a bit information at this point.
Also check the 'samples' subfolder. It's a maven project, and it contains some code samples for the popular use-cases:
* Encoding using high-level API -- [SequenceEncoderDemo.java](https://github.com/jcodec/jcodec/blob/master/samples/main/java/org/jcodec/samples/gen/SequenceEncoderDemo.java);
* Encoding/decoding using low-level API -- [TranscodeMain.java](https://github.com/jcodec/jcodec/blob/master/samples/main/java/org/jcodec/samples/transcode/TranscodeMain.java);

# Sample code

Getting a single frame from a movie ( supports only AVC, H.264 in MP4, ISO BMF, Quicktime container ):
```java
    int frameNumber = 150;
    BufferedImage frame = FrameGrab.getFrame(new File("filename.mp4"), frameNumber);
    ImageIO.write(frame, "png", new File("frame_150.png"));
```

Getting a sequence of frames from a movie ( supports only AVC, H.264 in MP4, ISO BMF, Quicktime container ):
```java
    double startSec = 51.632;
    FileChannelWrapper ch = null;
    try {
        ch = NIOUtils.readableFileChannel(new File("filename.mp4"));
        FrameGrab fg = new FrameGrab(ch);
        grab.seek(startSec);
        for (int i = 0; i < 100; i++) {
            ImageIO.write(grab.getFrame(), "png",
                new File(System.getProperty("user.home"), String.format("Desktop/frame_%08d.png", i)));
        }
    } finally {
        NIOUtils.closeQuietly(ch);
    }
```

# Contact

Feel free to communicate any questions or concerns to us. Dev team email: jcodecproject@gmail.com
