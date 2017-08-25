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

Core Jcodec
```xml
<dependency>
    <groupId>org.jcodec</groupId>
    <artifactId>jcodec</artifactId>
    <version>0.2.0</version>
</dependency>
```

For JDK
```xml
<dependency>
    <groupId>org.jcodec</groupId>
    <artifactId>jcodec</artifactId>
    <version>0.2.0</version>
</dependency>
<dependency>
    <groupId>org.jcodec</groupId>
    <artifactId>jcodec-javase</artifactId>
    <version>0.2.0</version>
</dependency>
```

For Android
```xml
android {
        configurations.all {
        resolutionStrategy.force 'com.google.code.findbugs:jsr305:3.0.2'
    }
}

dependencies {
    compile 'org.jcodec:jcodec:0.2.0'
    compile 'org.jcodec:jcodec-android:0.2.0'
}
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

//for Android (jcodec-android)
Bitmap bitmap = AndroidUtil.toBitmap(picture);
bitmap.compress(Bitmap.CompressFormat.PNG, 100, new FileOutputStream("frame42.png")); 

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

## Re-exporting video/audio without transcoding into a different format
```java
Source source = SourceImpl.create(input.getAbsolutePath());
Sink sink = SinkImpl.createWithFile(output.getAbsolutePath(), Format.MOV,
    null, null);
TranscoderBuilder builder = Transcoder.newTranscoder(source, sink);
builder.setAudioCopy();
builder.setVideoCopy();
builder.create().transcode();
```

## Transcoding video/audio into a different format
```java
Source source = SourceImpl.create(input.getAbsolutePath());
Sink sink = SinkImpl.createWithFile(output.getAbsolutePath(), Format.MOV,
    Codec.PRORES, Codec.PCM);
TranscoderBuilder builder = Transcoder.newTranscoder(source, sink);
builder.create().transcode();
```

## Adding a custom visual effect to the video
```java
Source source = SourceImpl.create(input.getAbsolutePath());
Sink sink = SinkImpl.createMatchingToFile(output.getAbsolutePath(), source);

TranscoderBuilder builder = Transcoder.newTranscoder(source, sink);
builder.addVideoFilter(new Filter {
    @Override
    public ColorSpace getInputColor() {
        return ColorSpace.ANY_PLANAR;
    }
    @Override
    public ColorSpace getOutputColor() {
        return ColorSpace.SAME;
    }
    @Override
    public LoanerPicture filter(Picture picture, PixelStore store) {
        LoanerPicture out = store.getPicture(picture.getWidth(), picture.getHeight(),
            picture.getColor());
        // Do something with the pixels from picture and store them into the out.picture
        // output picture.
        return out;
    }
});
builder.setAudioCopy();

builder.create().transcode();
```

# Contact

Feel free to communicate any questions or concerns to us. Dev team email: jcodecproject@gmail.com
