package org.jcodec.samples.mux;

import static org.jcodec.common.io.IOUtils.closeQuietly;
import static org.jcodec.common.io.IOUtils.readFileToByteArray;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import org.jcodec.common.DemuxerTrack;
import org.jcodec.common.io.FileChannelWrapper;
import org.jcodec.containers.mkv.MKVParser;
import org.jcodec.containers.mkv.demuxer.MKVDemuxer;
import org.jcodec.containers.mkv.demuxer.MKVDemuxer.AudioTrack;
import org.jcodec.containers.mkv.boxes.EbmlMaster;


public class MKVDemux {

	public static void main(String[] args) throws FileNotFoundException, IOException {
        if (args.length < 1) {
            System.out.println("Syntax: <in.mkv>\n" + "\tWhere:\n"
                    + "\t<in.mkv>\tThe file to decode");
            return;
        }

        FileInputStream demInputStream = new FileInputStream(args[0]);
        MKVDemuxer demuxer = new MKVDemuxer(new FileChannelWrapper(demInputStream.getChannel()));

    	System.out.println("Audio tracks: " + demuxer.getAudioTracks().size());
        for (DemuxerTrack elem : demuxer.getAudioTracks()) {
        	AudioTrack elemAudio = (AudioTrack)elem;
        	if (elemAudio == null) {
        		System.out.println("    - null");
        	} else {
        		System.out.println("    - " + elemAudio.getLanguage() + "  " + elemAudio.getMeta().getCodec() + "  " + elem.getMeta().getAudioCodecMeta().getChannelCount() + "  " + elem.getMeta().getAudioCodecMeta().getSampleRate());
        	}
        }
    	System.out.println("Video tracks: " + demuxer.getVideoTracks().size());
        for (DemuxerTrack elem : demuxer.getVideoTracks()) {
        	System.out.println("    - " + elem.getMeta().getType() + "  " + elem.getMeta().getCodec() + "  " + elem.getMeta().getVideoCodecMeta().getSize());
        }
    	System.out.println("Video subtitle tracks: " + demuxer.getSubtitleTracks().size());
        for (DemuxerTrack elem : demuxer.getSubtitleTracks()) {
        	System.out.println("    - " + elem.getMeta().getType() + "  " + elem.getMeta().getCodec());
        }
        closeQuietly(demInputStream);
        
	}

}
