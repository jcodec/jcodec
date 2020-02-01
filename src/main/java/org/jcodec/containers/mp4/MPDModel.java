package org.jcodec.containers.mp4;

import java.io.IOException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.jcodec.api.JCodecException;
import org.jcodec.common.XMLMapper;
import org.jcodec.common.XMLMapper.TypeHandler;
import org.jcodec.common.model.Rational;

public class MPDModel {
    public static class Time {
        public double sec;

        public Time(double sec) {
            this.sec = sec;
        }

        public static Time parseTime(String arg) {
            char[] charArray = arg.toCharArray();
            if (charArray.length < 3 || charArray[0] != 'P' || charArray[1] != 'T')
                return null;
            int tokenStart = 2;
            double sec = 0;
            for (int i = 2; i < charArray.length; i++) {
                if (charArray[i] == 'S' || charArray[i] == 'M' || charArray[i] == 'H' && tokenStart != i) {
                    String token = new String(charArray, tokenStart, i - tokenStart);
                    double parseDouble = Double.parseDouble(token);
                    tokenStart = i + 1;
                    switch (charArray[i]) {
                    case 'S':
                        sec += parseDouble;
                        break;
                    case 'M':
                        sec += parseDouble * 60;
                        break;
                    case 'H':
                        sec += parseDouble * 3600;
                        break;
                    }
                }

            }
            return new Time(sec);
        }

        public double getSec() {
            return sec;
        }
    }

    public static class TypeHandler1 implements TypeHandler {

        @Override
        public Object parse(String value, Class<?> clz) {
            if (clz == Time.class) {
                return Time.parseTime(value);
            }
            return null;
        }

        @Override
        public boolean supports(Class<?> clz) {
            return clz == Time.class;
        }
    }

    public static class MPD {
        public Time mediaPresentationDuration;
        public List<Period> periods = new LinkedList<Period>();

        public void addPeriod(Period arg) {
            periods.add(arg);
        }
    }

    public static class Period {
        public Time start;
        public Time duration;
        public List<AdaptationSet> adaptationSets = new LinkedList<AdaptationSet>();

        public void addAdaptationSet(AdaptationSet arg) {
            this.adaptationSets.add(arg);
        }
    }

    public static class AdaptationSet {
        public boolean segmentAlignment;
        public int maxWidth;
        public int maxHeight;
        public Rational maxFrameRate;
        public Rational par;

        public SegmentTemplate segmentTemplate;
        public List<Representation> representations = new LinkedList<Representation>();

        public void addRepresentation(Representation arg) {
            representations.add(arg);
        }
    }

    public static class SegmentTemplate {
        public int timescale;
        public int duration;
        public String media;
        public int startNumber;
        public String initialization;
    }

    public static class Representation {
        public String id;
        public String mimeType;
        public String codecs;
        public int width;
        public int height;
        public Rational frameRate;
        public Rational sar;
        public int startWithSAP;
        public int bandwidth;
        public int audioSamplingRate;
        public String baseURL;

        public SegmentTemplate segmentTemplate;
        public SegmentBase segmentBase;
        public SegmentList segmentList;
    }

    public static class SegmentBase {
        public String indexRange; // "0-834"
    }

    public static class SegmentList {
        public int duration;
        public Initialization initialization;
        public List<SegmentURL> segmentUrls = new LinkedList<SegmentURL>();

        public void addSegmentURL(SegmentURL arg) {
            segmentUrls.add(arg);
        }
    }

    public static class Initialization {
        public String sourceURL;
    }

    public static class SegmentURL {
        public String media;
    }

    public static MPD parse(URL url) throws IOException, JCodecException {
        try {
            return XMLMapper.map(url.openStream(), MPDModel.MPD.class, new TypeHandler1());
        } catch (ReflectiveOperationException e) {
            throw new JCodecException(e);
        } catch (XMLStreamException e) {
            throw new JCodecException(e);
        }
    }
}
