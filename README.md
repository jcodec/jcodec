jcodec - a pure java implementation of video/audio codecs.

# About 

JCodec is a library implementing a set of popular video and audio codecs. Currently JCodec supports:

* Video codecs
    * H.264 main profile decoder;
    * H.264 baseline profile encoder;
    * VP8 decoder (I frames only);
    * VP8 encoder (I frames only);
    * MPEG 1/2 decoder ( I/P/B frames, interlace );
    * Apple ProRes decoder/encoder;
    * JPEG decoder;
    * PNG decoder/encoder.
    * DivX/Xvid

* Audio codecs
    * SMPTE 302M decoder;
    * AAC decoder ([JAAD](http://jaadec.sourceforge.net/)) 
    * RAW PCM.

* Formats:
    * MP4 ( MOV ) demuxer / muxer;
    * MKV ( Matroska ) demuxer / muxer;
    * MPEG PS demuxer;
    * MPEG TS demuxer;
    * WAV demuxer/muxer;
    * MPEG Audio (MP3) demuxer;
    * ADTS demuxer.
    * DPX parser

JCodec is free software distributed under FreeBSD License.

# Future development

 Those are just some of the things JCodec dev team is planning to work on:

* Video
    * Improve H.264 encoder: CABAC, rate control;
    * Performance optimize H.264 decoder.
* Audio
    * MP3 decoder;
    * AAC encoder.

# Getting started

JCodec can be used in both standard Java and Android. It contains platform-agnostic java classes. To use the latest version of JCodec add the maven dependency as below:

```xml
<dependency>
    <groupId>org.jcodec</groupId>
    <artifactId>jcodec</artifactId>
    <version>0.2.5</version>
</dependency>
```

OR gradle dependency as below:

```gradle
dependencies {
    ...
    implementation 'org.jcodec:jcodec:0.2.5'
    ...
}
```

Additionally if you want to use JCodec with AWT images (BufferedImage) add this maven dependency:

```xml
<dependency>
    <groupId>org.jcodec</groupId>
    <artifactId>jcodec</artifactId>
    <version>0.2.5</version>
</dependency>
<dependency>
    <groupId>org.jcodec</groupId>
    <artifactId>jcodec-javase</artifactId>
    <version>0.2.5</version>
</dependency>
```

OR if you want to use JCodec with Android images (Bitmap) add this gradle dependency:

```gradle
android {
    configurations.all {
        resolutionStrategy.force 'com.google.code.findbugs:jsr305:3.0.2'
    }
}
dependencies {
    ...
    implementation 'org.jcodec:jcodec:0.2.5'
    implementation 'org.jcodec:jcodec-android:0.2.5'
    ...
}
```

For the latest and greatest (the 0.2.6-SNAPSHOT) clone this Git project and build locally like so:
```
git clone https://github.com/jcodec/jcodec.git
cd jcodec
mvn clean install
(cd javase; mvn clean install)
(cd android; mvn clean install)
```

If you JUST need the jars, download them from here:
* [JCodec 0.2.5 JAR](http://central.maven.org/maven2/org/jcodec/jcodec/0.2.5/jcodec-0.2.5.jar), [GPG Sign](http://central.maven.org/maven2/org/jcodec/jcodec/0.2.5/jcodec-0.2.5.jar.asc), [POM](http://central.maven.org/maven2/org/jcodec/jcodec/0.2.5/jcodec-0.2.5.pom)
* [JCodec JavaSE 0.2.5 JAR](http://central.maven.org/maven2/org/jcodec/jcodec-javase/0.2.5/jcodec-javase-0.2.5.jar), [GPG Sign](http://central.maven.org/maven2/org/jcodec/jcodec-javase/0.2.5/jcodec-javase-0.2.5.jar.asc), [POM](http://central.maven.org/maven2/org/jcodec/jcodec-javase/0.2.5/jcodec-javase-0.2.5.pom)

There is virtually no documentation right now but the plan is to catch up on this so stay tuned. stackoverflow.com contains quite a bit information at this point.

Also check the 'samples' subfolder. It's a maven project, and it contains some code samples for the popular use-cases:
* Encoding using high-level API -- [SequenceEncoderDemo.java](https://github.com/jcodec/jcodec/blob/master/samples/main/java/org/jcodec/samples/gen/SequenceEncoderDemo.java);
* Encoding/decoding using low-level API -- [TranscodeMain.java](https://github.com/jcodec/jcodec/blob/master/samples/main/java/org/jcodec/samples/transcode/TranscodeMain.java);

# Performance / quality considerations
Because JCodec is a pure Java implementation please adjust your performance expectations accordingly. We usually make the best effort to write efficient code but despite this the decoding will typically be an order of magnitude slower than the native implementations (such as FFMpeg). We are currently looking into implementing performance-critical parts in OpenCL (or RenderScript for Android) but the ETA is unknown.

Expect the encoded quality/bitrate for h.264 (AVC) to be so much worse compared to the well known native encoders (such as x264). This is because very little work has been put so far into developing the encoder and also because encoders usually trade speed for quality, speed is something we don't have in Java, hence the quality. Again we may potentially fix that in the future by introducing OpenCL (RenderScript) code but at this point it's an unknown.

That said the decode quality should be at the industry level. This is because the decoding process is usually specified by the standard and the correct decoder implementations are expected to produce bit-exact outputs.

# Sample code


## Getting a single frame from a movie ( supports only AVC, H.264 in MP4, ISO BMF, Quicktime container ):

```java
int frameNumber = 42;
Picture picture = FrameGrab.getFrameFromFile(new File("video.mp4"), frameNumber);

//for JDK (jcodec-javase)
BufferedImage bufferedImage = AWTUtil.toBufferedImage(picture);
ImageIO.write(bufferedImage, "png", new File("frame42.png"));

//for Android (jcodec-android)
Bitmap bitmap = AndroidUtil.toBitmap(picture);
bitmap.compress(Bitmap.CompressFormat.PNG, 100, new FileOutputStream("frame42.png")); 

```

## Get all frames from a video file

```
File file = new File("video.mp4");
FrameGrab grab = FrameGrab.createFrameGrab(NIOUtils.readableChannel(file));
Picture picture;
while (null != (picture = grab.getNativeFrame())) {
    System.out.println(picture.getWidth() + "x" + picture.getHeight() + " " + picture.getColor());
}
```

## Getting a sequence of frames from a movie ( supports only AVC, H.264 in MP4, ISO BMF, Quicktime container ):

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

    //for Android (jcodec-android)
    Bitmap bitmap = AndroidUtil.toBitmap(picture);
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, new FileOutputStream("frame"+i+".png")); 
}
```

Making a video with a set of images from memory:
```java
SeekableByteChannel out = null;
try {
    out = NIOUtils.writableFileChannel("/tmp/output.mp4");
    // for Android use: AndroidSequenceEncoder
    AWTSequenceEncoder encoder = new AWTSequenceEncoder(out, Rational.R(25, 1));
    for (...) {
        // Generate the image, for Android use Bitmap
        BufferedImage image = ...;
        // Encode the image
        encoder.encodeImage(image);
    }
    // Finalize the encoding, i.e. clear the buffers, write the header, etc.
    encoder.finish();
} finally {
    NIOUtils.closeQuietly(out);
}
```

## H264 video from png images
```
File output = new File("test.mp4");
SequenceEncoder enc = SequenceEncoder.createWithFps(NIOUtils.writableChannel(output), new Rational(1, 1));
String[] files = {"frame0.png", "frame1.png", "frame2.png"};
for (String file : files) {
   enc.encodeNativeFrame(AWTUtil.decodePNG(new File(file), ColorSpace.RGB));
}
enc.finish();
```

## Read Tape Timecode from MXF file

```java
TapeTimecode timecode = MXFDemuxer.readTapeTimecode(new File("myfile.mxf"));
```

## Parse DPX metadata

```java
DPXMetadata dpx = DPXReader.readFile(firstDpx).parseMetadata();
System.out.println(dpx.getTimecodeString());
```

## Parse Apple GPS metadata from MOV or MP4 file
```java
MovieBox moov = MP4Util.parseMovie(new File("gps1.mp4"));
UdtaBox udta = NodeBox.findFirst(moov, UdtaBox.class, "udta");
String latlng = udta.latlng();
assertEquals("-35.2840+149.1215/", latlng);
```

OR

```java
MovieBox moov = MP4Util.parseMovie(new File("gps2.MOV"));
MetaBox meta = NodeBox.findFirst(moov, MetaBox.class, "meta");
String latlng1 = meta.getKeyedMeta().get("com.apple.quicktime.location.ISO6709").getString();
assertEquals("-35.2844+149.1214+573.764/", latlng1);
String latlng2 = meta.getItunesMeta().get(1).getString();
assertEquals("-35.2844+149.1214+573.764/", latlng2);

```

## Extract subtitles from MKV file
```java
MKVDemuxer demuxer = new MKVDemuxer(new AutoFileChannelWrapper(new File("subs.mkv")));
DemuxerTrack track = demuxer.getSubtitleTracks().get(0);
Packet packet;
while (null != (packet = track.nextFrame())) {
    String text = takeString(packet.getData());
    System.out.println("time: " + packet.pts + " text: " + text);
}
```

## MP4/M4A/MOV metadata versions

Some editors (e.g. Final Cut Pro 7) employ a clever hack to support multiple versions of metadata for mp4 files.
The trick is to append a new version of `moov` atom to end of file and set previous version atom name to `free`.
Jcodec supports this hack via `org.jcodec.movtool.MoovVersions` 

See `MoovVersionsTest.java` for complete usage example.

To list available versions use

```java
MoovVersions.listMoovVersionAtoms(new File("my.mp4"))
```

To add a version

```java
moov = MP4Util.parseMovie(new File("my.mp4"))
//add your modifications to moov here
MoovVersions.addVersion(new File("my.mp4"), moov)
```

To rollback (undo) to previous version

```java
MoovVersions.undo(new File("my.mp4"))
```

To rollback to specific  version

```java
versions = MoovVersions.listMoovVersionAtoms(new File("my.mp4"))
//pick your version
version = versions.get(Math.random()*versions.size())
MoovVersions.rollback(new File("my.mp4"), version)
```

# Contact

Feel free to communicate any questions or concerns to us. Dev team email: jcodecproject@gmail.com
