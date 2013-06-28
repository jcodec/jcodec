package org.jcodec.scale;

import org.jcodec.common.model.ColorSpace;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
 * 
 */
public class ColorUtil {

    public static Transform getTransform(ColorSpace from, ColorSpace to) {
        switch (from) {
        case RGB:
            switch (to) {
            case YUV420:
                return new RgbToYuv420(0, 0);
            case YUV422:
                return new RgbToYuv422(0, 0);
            case YUV422_10:
                return new RgbToYuv422(2, 0);
            case YUV444:
                return null;
            case YUV444_10:
                return null;
            }
        case YUV420:
            switch (to) {
            case RGB:
                return new Yuv420pToRgb(0, 0);
            case YUV422:
                return new Yuv420pToYuv422p(0, 0);
            case YUV422_10:
                return new Yuv420pToYuv422p(0, 2);
            case YUV444:
                return null;
            case YUV444_10:
                return null;
            }
        case YUV422:
            switch (to) {
            case RGB:
                return new Yuv422pToRgb(0, 0);
            case YUV420:
                return new Yuv422pToYuv420p(0, 0);
            case YUV444:
                return null;
            case YUV444_10:
                return null;
            }
        case YUV422_10:
            switch (to) {
            case RGB:
                return new Yuv422pToRgb(2, 0);
            case YUV420:
                return new Yuv422pToYuv420p(0, 2);
            case YUV444:
                return null;
            case YUV444_10:
                return null;
            }
        case YUV444:
            switch (to) {
            case RGB:
                return new Yuv444pToRgb(0, 0);
            case YUV420:
                return new Yuv444pToYuv420p(0, 0);
            case YUV444:
                return null;
            case YUV444_10:
                return null;
            }
        case YUV444_10:
            switch (to) {
            case RGB:
                return new Yuv444pToRgb(2, 0);
            case YUV420:
                return new Yuv444pToYuv420p(0, 2);
            case YUV444:
                return null;
            case YUV444_10:
                return null;
            }
        }
        return null;
    }
}
