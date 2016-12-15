package org.jcodec.samples.transcode;

import static org.jcodec.common.model.Rational.HALF;
import static org.jcodec.common.model.Unit.SEC;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Set;

import org.jcodec.codecs.prores.ProresEncoder;
import org.jcodec.common.Codec;
import org.jcodec.common.DemuxerTrackMeta;
import org.jcodec.common.Format;
import org.jcodec.common.VideoEncoder;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Picture8Bit;
import org.jcodec.common.model.Size;
import org.jcodec.common.tools.MainUtils.Cmd;
import org.jcodec.containers.mp4.Brand;
import org.jcodec.containers.mp4.MP4Packet;
import org.jcodec.containers.mp4.muxer.FramesMP4MuxerTrack;
import org.jcodec.containers.mp4.muxer.MP4Muxer;

/**
 * A profile to convert an image sequence to prores.
 * 
 * @author Stanislav Vitvitskiy
 *
 */
class Png2prores extends FromImgProfile {
    private static final String DEFAULT_PROFILE = "apch";
    private static final String FLAG_FOURCC = "fourcc";
    private static final String FLAG_INTERLACED = "interlaced";
    private MP4Muxer muxer;
    private ProresEncoder.Profile proresProfile;
    private boolean interlaced;

    @Override
    public void transcode(Cmd cmd, Profile profile) throws IOException {
        String fourccName = cmd.getStringFlagD(FLAG_FOURCC, DEFAULT_PROFILE);
        proresProfile = getProfile(fourccName);
        if (proresProfile == null) {
            System.out.println("Unsupported fourcc: " + fourccName);
            return;
        }
        interlaced = cmd.getBooleanFlagD(FLAG_INTERLACED, false);
        super.transcode(cmd, profile);
    }

    @Override
    protected void populateAdditionalFlags(Map<String, String> flags) {
        flags.put(FLAG_FOURCC, "Prores profile fourcc.");
        flags.put(FLAG_INTERLACED, "Should use interlaced encoding?");
    }

    private static ProresEncoder.Profile getProfile(String fourcc) {
        for (ProresEncoder.Profile profile2 : ProresEncoder.Profile.values()) {
            if (fourcc.equals(profile2.fourcc))
                return profile2;
        }
        return null;
    }

    @Override
    public Set<Format> outputFormat() {
        return TranscodeMain.formats(Format.MOV);
    }

    @Override
    public Set<Codec> outputVideoCodec() {
        return TranscodeMain.codecs(Codec.PRORES);
    }

    @Override
    protected VideoEncoder getEncoder() {
        return new ProresEncoder(proresProfile, interlaced);
    }
    
    static final String APPLE_PRO_RES_422 = "Apple ProRes 422";
    
    @Override
    protected FramesMP4MuxerTrack getMuxerTrack(SeekableByteChannel sink, DemuxerTrackMeta inTrackMeta, Picture8Bit yuv,
            Packet firstPacket) throws IOException {
        muxer = MP4Muxer.createMP4Muxer(sink, Brand.MOV);
        FramesMP4MuxerTrack videoTrack = muxer.addVideoTrack(proresProfile.fourcc, new Size(yuv.getWidth(), yuv.getHeight()),
                APPLE_PRO_RES_422, (int)firstPacket.getTimescale());
        videoTrack.setTgtChunkDuration(HALF, SEC);
        return videoTrack;
    }

    @Override
    protected void finalizeMuxer() throws IOException {
        muxer.writeHeader();
    }

    @Override
    protected MP4Packet encodeFrame(VideoEncoder encoder, Picture8Bit yuv, Packet inPacket, ByteBuffer buf) {
        ByteBuffer encoded = encoder.encodeFrame8Bit(yuv, buf);
        return MP4Packet.createMP4Packet(encoded, inPacket.getPts(), inPacket.getTimescale(), inPacket.getDuration(),
                inPacket.getFrameNo(), true, null, (int) inPacket.getFrameNo(), inPacket.getPts(), 0);
    }
}