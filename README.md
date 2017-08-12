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
    * Native optimizations for decoders and encoders;
* Audio
    * AAC encoder;

# Getting started

Build from the source and include both JARs (jcodec.jar and jcodec-javase.jar) in your projects. Alternatively, obsolete
versions can be included automatically with maven. For this just add below snippet to your pom.xml-

For JDK
```xml
<dependency>
    <groupId>org.jcodec</groupId>
    <artifactId>jcodec-javase</artifactId>
    <version>0.2.0</version>
</dependency>
```

For Android and JDK 
```xml
<dependency>
    <groupId>org.jcodec</groupId>
    <artifactId>jcodec</artifactId>
    <version>0.2.0</version>
</dependency>
```
OR download it from here (you will need both jars):
* [JCodec 0.2.0 JAR](http://central.maven.org/maven2/org/jcodec/jcodec/0.2.0/jcodec-0.2.0.jar), [GPG Sign](http://central.maven.org/maven2/org/jcodec/jcodec/0.2.0/jcodec-0.2.0.jar.asc), [POM](http://central.maven.org/maven2/org/jcodec/jcodec/0.2.0/jcodec-0.2.0.pom)
* [JCodec JavaSE 0.2.0 JAR](http://central.maven.org/maven2/org/jcodec/jcodec-javase/0.2.0/jcodec-javase-0.2.0.jar), [GPG Sign](http://central.maven.org/maven2/org/jcodec/jcodec-javase/0.2.0/jcodec-javase-0.2.0.jar.asc), [POM](http://central.maven.org/maven2/org/jcodec/jcodec-javase/0.2.0/jcodec-javase-0.2.0.pom)

There is virtually no documentation right now but the plan is to catch up on this so stay tuned. stackoverflow.com contains quite a bit information at this point.
Also check the 'samples' subfolder. It's a maven project, and it contains some code samples for the popular use-cases:
* Encoding using high-level API -- [SequenceEncoderDemo.java](https://github.com/jcodec/jcodec/blob/master/samples/main/java/org/jcodec/samples/gen/SequenceEncoderDemo.java);
* Encoding/decoding using low-level API -- [TranscodeMain.java](https://github.com/jcodec/jcodec/blob/master/samples/main/java/org/jcodec/samples/transcode/TranscodeMain.java);

# Sample code


Getting a single frame from a movie ( supports only AVC, H.264 in MP4, ISO BMF, Quicktime container ):
```java
int frameNumber = 42;
Picture picture = FrameGrab.getFrameFromFile(new File("video.mp4"), frameNumber);

//for JDK (jcodec-javase)
BufferedImage bufferedImage = AWTUtil.toBufferedImage(picture);
ImageIO.write(bufferedImage, "png", new File("frame42.png"));
```

Get all frames from a video file
```
File file = new File("video.mp4");
FrameGrab grab = FrameGrab.createFrameGrab(NIOUtils.readableChannel(file));
Picture picture;
while (null != (picture = grab.getNativeFrame())) {
    System.out.println(picture.getWidth() + "x" + picture.getHeight() + " " + picture.getColor());
}
```

Getting a sequence of frames from a movie ( supports only AVC, H.264 in MP4, ISO BMF, Quicktime container ):
```java
double startSec = 51.632;
int frameCount = 10;
File file = new File("video.mp4");

FrameGrab grab = FrameGrab.createFrameGrab(NIOUtils.readableChannel(file));
grab.seekToSecondPrecise(startSec);

for (int i=0;i<frameCount;i++) {
    Picture picture = grab.getNativeFrame();
    System.out.println(picture.getWidth() + "x" + picture.getHeight() + " " + picture.getColor());
    //for JDK (jcodec-javase)
    BufferedImage bufferedImage = AWTUtil.toBufferedImage(picture);
    ImageIO.write(bufferedImage, "png", new File("frame"+i+".png"));
}
```

# Contact

Feel free to communicate any questions or concerns to us. Dev team email: jcodecproject@gmail.com
