package org.jcodec.containers.mkv.boxes;
import java.nio.ByteBuffer;
import java.util.Date;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed under FreeBSD License
 * 
 * EBML IO implementation
 * 
 * @author The JCodec project
 * 
 */
public class EbmlDate extends EbmlSint {
    private static final int NANOSECONDS_IN_A_SECOND = 1000000000;
    private static final int MILISECONDS_IN_A_SECOND = 1000;
    private static final int NANOSECONDS_IN_A_MILISECOND = NANOSECONDS_IN_A_SECOND / MILISECONDS_IN_A_SECOND;
    public static long MILISECONDS_SINCE_UNIX_EPOCH_START = 978307200; // 2001/01/01 00:00:00 UTC

    public EbmlDate(byte[] id) {
        super(id);
    }

    public void setDate(Date value) {
        setMiliseconds(value.getTime());
    }

    public Date getDate() {
        long val = getLong();
        val = val / NANOSECONDS_IN_A_MILISECOND + MILISECONDS_SINCE_UNIX_EPOCH_START;
        return new Date(val);
    }

    private void setMiliseconds(long milliseconds) {
        setLong((milliseconds - MILISECONDS_SINCE_UNIX_EPOCH_START) * NANOSECONDS_IN_A_MILISECOND);
    }

    @Override
    public void setLong(long value) {
        this.data = ByteBuffer.allocate(8);
        this.data.putLong(value);
        this.data.flip();
    }

}
