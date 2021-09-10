package org.jcodec.codecs.vpx.vp8.data;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import org.jcodec.codecs.vpx.vp8.Extend;
import org.jcodec.codecs.vpx.vp8.enums.FrameTypeFlags;
import org.jcodec.codecs.vpx.vp8.pointerhelper.FullAccessIntArrPointer;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License.
 * 
 * The class is a direct java port of libvpx's
 * (https://github.com/webmproject/libvpx) relevant VP8 code with significant
 * java oriented refactoring.
 * 
 * @author The JCodec project
 * 
 */
public class Lookahead {

    public static final int PEEK_FORWARD = 1;
    public static final int PEEK_BACKWARD = -1;
    public static final int MAX_LAG_BUFFERS = 25;

    int max_sz; /* Absolute size of the queue */
    int sz; /* Number of buffers currently in the queue */
    int read_idx; /* Read index */
    int write_idx; /* Write index */
    List<LookaheadEntry> buffer; /* Buffer list */

    public void vp8_lookahead_destroy() {
        if (buffer != null) {
            buffer = null;
        }
    }

    public int vp8_lookahead_depth() {
        return sz;
    }

    /* Return the buffer at the given absolute index and increment the index */
    LookaheadEntry pop(boolean read) {
        int curidx = read ? read_idx : write_idx;
        LookaheadEntry buf = buffer.get(curidx);

        assert (curidx < max_sz);
        curidx++;
        if (curidx >= max_sz)
            curidx -= max_sz;
        if (read) {
            read_idx = curidx;
        } else {
            write_idx = curidx;
        }
        return buf;
    }

    public boolean vp8_lookahead_push(YV12buffer src, long ts_start, long ts_end, EnumSet<FrameTypeFlags> flags,
            FullAccessIntArrPointer active_map) {
        LookaheadEntry buf;
        int row, col, active_end;
        int mb_rows = (src.y_height + 15) >> 4;
        int mb_cols = (src.y_width + 15) >> 4;

        if (sz + 2 > max_sz)
            return true;
        sz++;
        buf = pop(false);

        /*
         * Only do this partial copy if the following conditions are all met: 1.
         * Lookahead queue has has size of 1. 2. Active map is provided. 3. This is not
         * a key frame, golden nor altref frame.
         */
        if (max_sz == 1 && active_map != null && flags.isEmpty()) {
            for (row = 0; row < mb_rows; ++row) {
                col = 0;
                int baseidx = row * mb_cols;
                while (true) {
                    /* Find the first active macroblock in this row. */
                    for (; col < mb_cols; ++col) {
                        if (active_map.getRel(baseidx + col) != 0)
                            break;
                    }

                    /* No more active macroblock in this row. */
                    if (col == mb_cols)
                        break;

                    /* Find the end of active region in this row. */
                    active_end = col;

                    for (; active_end < mb_cols; ++active_end) {
                        if (active_map.getRel(baseidx + active_end) == 0)
                            break;
                    }

                    /* Only copy this active region. */
                    Extend.vp8_copy_and_extend_frame_with_rect(src, buf.img, row << 4, col << 4, 16,
                            (active_end - col) << 4);

                    /* Start again from the end of this active region. */
                    col = active_end;
                }
            }
        } else {
            Extend.vp8_copy_and_extend_frame(src, buf.img);
        }
        buf.ts_start = ts_start;
        buf.ts_end = ts_end;
        buf.flags = flags;
        return false;
    }

    public LookaheadEntry vp8_lookahead_peek(int index, int direction) {
        LookaheadEntry buf = null;

        if (direction == PEEK_FORWARD) {
            assert (index < max_sz - 1);
            if (index < sz) {
                index += read_idx;
                if (index >= max_sz)
                    index -= max_sz;
                buf = buffer.get(index);
            }
        } else if (direction == PEEK_BACKWARD) {
            assert (index == 1);

            if (read_idx == 0) {
                index = max_sz - 1;
            } else {
                index = read_idx - index;
            }
            buf = buffer.get(index);
        }

        return buf;
    }

    public LookaheadEntry vp8_lookahead_pop(boolean drain) {
        LookaheadEntry buf = null;

        if (sz != 0 && (drain || sz == max_sz - 1)) {
            buf = pop(true);
            sz--;
        }
        return buf;
    }

    public Lookahead(int width, int height, int depth) {
        int i;

        /* Clamp the lookahead queue depth */
        if (depth < 1) {
            depth = 1;
        } else if (depth > MAX_LAG_BUFFERS) {
            depth = MAX_LAG_BUFFERS;
        }

        /* Keep last frame in lookahead buffer by increasing depth by 1. */
        depth += 1;

        /* Align the buffer dimensions */
        width = (width + 15) & ~15;
        height = (height + 15) & ~15;

        /* Allocate the lookahead structures */
        max_sz = depth;
        buffer = new ArrayList<LookaheadEntry>(depth);
        for (i = 0; i < depth; ++i) {
            LookaheadEntry la = new LookaheadEntry();
            buffer.add(la);
            la.img = new YV12buffer(width, height);
        }
    }

}
