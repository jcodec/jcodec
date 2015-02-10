package org.jcodec.containers.flv;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.Map;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * FLV metadata
 * 
 * @author Stan Vitvitskyy
 * 
 */
public class FLVMetadata {

    private double duration;
    private double width;
    private double height;
    private double framerate;

    private String audiocodecid;
    private double videokeyframe_frequency;
    private String videodevice;
    private double avclevel;
    private double audiosamplerate;
    private double audiochannels;
    private String presetname;
    private double videodatarate;
    private double audioinputvolume;
    private Date creationdate;
    private String videocodecid;
    private double avcprofile;
    private String audiodevice;
    private double audiodatarate;

    public FLVMetadata(Map<String, Object> md) {
        Field[] declaredFields = this.getClass().getDeclaredFields();
        for (Field field : declaredFields) {
            Object object = md.get(field.getName());
            try {
                if (object instanceof Double) {
                    field.setDouble(this, (Double) object);
                } else if (object instanceof Boolean) {
                    field.setBoolean(this, (Boolean) object);
                } else {
                    field.set(this, object);
                }
            } catch (IllegalArgumentException | IllegalAccessException e) {
            }
        }
    }

    public double getDuration() {
        return duration;
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

    public double getFramerate() {
        return framerate;
    }

    public String getAudiocodecid() {
        return audiocodecid;
    }

    public double getVideokeyframe_frequency() {
        return videokeyframe_frequency;
    }

    public String getVideodevice() {
        return videodevice;
    }

    public double getAvclevel() {
        return avclevel;
    }

    public double getAudiosamplerate() {
        return audiosamplerate;
    }

    public double getAudiochannels() {
        return audiochannels;
    }

    public String getPresetname() {
        return presetname;
    }

    public double getVideodatarate() {
        return videodatarate;
    }

    public double getAudioinputvolume() {
        return audioinputvolume;
    }

    public Date getCreationdate() {
        return creationdate;
    }

    public String getVideocodecid() {
        return videocodecid;
    }

    public double getAvcprofile() {
        return avcprofile;
    }

    public String getAudiodevice() {
        return audiodevice;
    }

    public double getAudiodatarate() {
        return audiodatarate;
    }
}
