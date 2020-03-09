package org.jcodec.codecs.aac;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.jcodec.common.IntArrayList;
import org.jcodec.common.LongArrayList;
import org.jcodec.common.io.BitReader;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.logging.Logger;
import org.jcodec.common.tools.MathUtil;
import org.jcodec.containers.mp4.MP4Util;
import org.jcodec.containers.mp4.MP4Util.Atom;
import org.jcodec.containers.mp4.boxes.ChunkOffsets64Box;
import org.jcodec.containers.mp4.boxes.MovieBox;
import org.jcodec.containers.mp4.boxes.NodeBox;
import org.jcodec.containers.mp4.boxes.SampleSizesBox;
import org.jcodec.containers.mp4.boxes.SampleToChunkBox;
import org.jcodec.containers.mp4.boxes.TextMetaDataSampleEntry;
import org.jcodec.containers.mp4.boxes.TimeToSampleBox;
import org.jcodec.containers.mp4.boxes.TimeToSampleBox.TimeToSampleEntry;
import org.jcodec.containers.mp4.boxes.TrakBox;

import net.sourceforge.jaad.aac.AACDecoderConfig;
import net.sourceforge.jaad.aac.AACException;
import net.sourceforge.jaad.aac.ChannelConfiguration;
import net.sourceforge.jaad.aac.SampleFrequency;
import net.sourceforge.jaad.aac.syntax.CCE;
import net.sourceforge.jaad.aac.syntax.CPE;
import net.sourceforge.jaad.aac.syntax.DSE;
import net.sourceforge.jaad.aac.syntax.Element;
import net.sourceforge.jaad.aac.syntax.FIL;
import net.sourceforge.jaad.aac.syntax.PCE;
import net.sourceforge.jaad.aac.syntax.SCE_LFE;
import net.sourceforge.jaad.aac.syntax.SyntaxConstants;

import static net.sourceforge.jaad.aac.syntax.SyntaxConstants.*;

/**
 * This will attempt to parse individual AAC frames out of a corrupt M4A file
 * and recreate the index. The movie header template needs to be provided with
 * the AAC specific atoms.
 * 
 * @author Stanislav Vitvitskiy
 *
 */
public class M4AFix {
    public static void main(String[] args) throws IOException {
        File file = new File(args[0]);
        MovieBox moov = MP4Util.parseMovie(new File(args[1]));
        File file1 = new File(args[2]);
        NIOUtils.copyFile(file, file1);
        file = file1;

        SeekableByteChannel ch = null;
        try {
            ch = NIOUtils.rwChannel(file);
            fixM4A(moov, ch);
        } finally {
            NIOUtils.closeQuietly(ch);
        }

    }

    public static void fixM4A(MovieBox moov, SeekableByteChannel ch) throws IOException {
        List<Atom> rootAtoms = MP4Util.getRootAtoms(ch);
        if (MP4Util.getMoov(rootAtoms) != null)
            return;
        Atom mdat = MP4Util.getMdat(rootAtoms);
        int size = processMdat(ch, mdat, moov);
        MP4Util.writeMovie(ch, moov);
        ch.setPosition(mdat.getOffset());
        ch.write((ByteBuffer) ByteBuffer.allocate(4).putInt(size + 8).flip());
    }

    public static class Track {
        LongArrayList offsets = LongArrayList.createLongArrayList();
        IntArrayList sizes = IntArrayList.createIntArrayList();
        List<SampleToChunkBox.SampleToChunkEntry> chunkSizes = new ArrayList<SampleToChunkBox.SampleToChunkEntry>();
        long newChunkOff;
        int chunkSize = 0, prevChunkSize = 0, chunkNo = 0, firstChunk = 0;
        int frameCount = 0;

        void addFrame(int sz, long offset, boolean newChunk) {
            if (newChunk) {
                newChunkOff = offset;
                offsets.add(newChunkOff);
                newChunk = false;
                if (chunkSize != prevChunkSize) {
                    chunkSizes.add(new SampleToChunkBox.SampleToChunkEntry(firstChunk + 1, chunkSize, 1));
                    firstChunk = chunkNo;
                }
                prevChunkSize = chunkSize;
                chunkSize = 0;
                chunkNo++;
            }

            ++frameCount;
            sizes.add(sz);
            chunkSize++;
        }

        TrakBox finish(TrakBox trak, MovieBox moov) {
            if (chunkSize != prevChunkSize)
                chunkSizes.add(new SampleToChunkBox.SampleToChunkEntry(firstChunk + 1, chunkSize, 1));

            ChunkOffsets64Box co64 = ChunkOffsets64Box.createChunkOffsets64Box(offsets.toArray());
            SampleSizesBox stsz = SampleSizesBox.createSampleSizesBox2(sizes.toArray());
            SampleToChunkBox stsc = SampleToChunkBox
                    .createSampleToChunkBox(chunkSizes.toArray(new SampleToChunkBox.SampleToChunkEntry[0]));
            TimeToSampleEntry[] tts = new TimeToSampleEntry[] { new TimeToSampleEntry(frameCount, 1024) };
            TimeToSampleBox stts = TimeToSampleBox.createTimeToSampleBox(tts);
            NodeBox stbl = trak.getStbl();
            stbl.replaceBox(co64);
            stbl.replaceBox(stsz);
            stbl.replaceBox(stts);
            stbl.replaceBox(stsc);
            stbl.removeChildren(new String[] { "stco" });
            int duration = (moov.getTimescale() * frameCount * 1024) / 48000;
            int mediaDuration = (trak.getTimescale() * frameCount * 1024) / 48000;
            trak.setDuration(duration);
            trak.setMediaDuration(mediaDuration);
            return trak;
        }
    }

    private static int processMdat(SeekableByteChannel ch, Atom atom, MovieBox moov) throws IOException {
        ByteBuffer buf = null;
        ch.setPosition(atom.getOffset() + atom.getHeader().headerSize());
        long offset = ch.position();

        buf = NIOUtils.fetchFromChannel(ch, (int) atom.getHeader().getBodySize());
        int size = buf.remaining();
        long start = System.currentTimeMillis();
        Track audio = new Track();
        Track tags = new Track();
        Track words = new Track();

        boolean newChunk = true;
        while (buf.hasRemaining()) {
            int sz = parseFrame(buf);

            if (sz > 0)
                audio.addFrame(sz, offset, newChunk);
            else if (sz == -18 || sz == -19)
                tags.addFrame(-sz, offset, true);
            else
                words.addFrame(-sz, offset, true);

            if (sz > 0) {
                offset += sz;
            } else {
                // Inverted to indicate non-aac frame
                offset += -sz;
                newChunk = true;
            }

        }
        TrakBox audioTrack = moov.getAudioTracks().get(0);
        List<TrakBox> metaTracks = moov.getMetaTracks();
        TrakBox tagsTrack = null;
        TrakBox wordsTrack = null;
        for (TrakBox trakBox : metaTracks) {
            String mime = ((TextMetaDataSampleEntry) trakBox.getSampleEntries()[0]).getMimeFormat();
            if ("application/transcription_1".equals(mime))
                wordsTrack = trakBox;
            else if ("application/audio_tags_1".equals(mime))
                tagsTrack = trakBox;
        }
        moov.removeChildren(new String[] { "trak" });
        moov.add(audio.finish(audioTrack, moov));
        moov.add(tags.finish(tagsTrack, moov));
        moov.add(words.finish(wordsTrack, moov));

        moov.setDuration(MathUtil.max3L(audioTrack.getDuration(), tagsTrack.getDuration(), wordsTrack.getDuration()));

        Logger.info("Time: " + (System.currentTimeMillis() - start));
        return size;
    }

    private static Element decodeSCE_LFE(BitReader _in, AACDecoderConfig conf) throws AACException {
        SCE_LFE el = new SCE_LFE(SyntaxConstants.WINDOW_LEN_LONG);
        el.decode(_in, conf);
        return el;
    }

    private static Element decodeCPE(BitReader _in, AACDecoderConfig conf) throws AACException {
        CPE el = new CPE(SyntaxConstants.WINDOW_LEN_LONG);
        el.decode(_in, conf);
        return el;
    }

    private static Element decodeCCE(BitReader _in, AACDecoderConfig conf) throws AACException {
        CCE el = new CCE(SyntaxConstants.WINDOW_LEN_LONG);
        el.decode(_in, conf);
        return el;
    }

    private static Element decodePCE(BitReader _in, AACDecoderConfig conf) throws AACException {
        PCE pce = new PCE();
        pce.decode(_in);
        conf.setProfile(pce.getProfile());
        conf.setSampleFrequency(pce.getSampleFrequency());
        conf.setChannelConfiguration(ChannelConfiguration.forInt(pce.getChannelCount()));
        return pce;
    }

    private static Element decodeFIL(BitReader _in, Element prev, AACDecoderConfig conf) throws AACException {
        FIL el = new FIL(conf.isSBRDownSampled());
        el.decode(_in, prev, conf.getSampleFrequency(), conf.isSBREnabled(), conf.isSmallFrameUsed());
        return el;
    }

    static int parseFrame(ByteBuffer buf) throws AACException {
        // Do I look like a proto?
        int len = buf.duplicate().getInt();
        if ((len >> 16) == 0) {
            NIOUtils.skip(buf, len + 4);
            return -(len + 4);
        } else {
            BitReader _in = BitReader.createBitReader(buf.duplicate());
            AACDecoderConfig conf = new AACDecoderConfig();
            conf.setSampleFrequency(SampleFrequency.SAMPLE_FREQUENCY_48000);
            int type;
            Element prev = null;
            while ((type = _in.readNBit(3)) != ELEMENT_END) {
                switch (type) {
                case ELEMENT_SCE:
                case ELEMENT_LFE:
                    prev = decodeSCE_LFE(_in, conf);
                    break;
                case ELEMENT_CPE:
                    prev = decodeCPE(_in, conf);
                    break;
                case ELEMENT_CCE:
                    decodeCCE(_in, conf);
                    prev = null;
                    break;
                case ELEMENT_DSE:
                    decodeSCE_LFE(_in, conf);
                    prev = null;
                    break;
                case ELEMENT_PCE:
                    decodePCE(_in, conf);
                    prev = null;
                    break;
                case ELEMENT_FIL:
                    decodeFIL(_in, prev, conf);
                    prev = null;
                    break;
                }
            }
            _in.align();
            buf.position(buf.position() + _in.position() / 8);
            return _in.position() / 8;
        }
    }
}
