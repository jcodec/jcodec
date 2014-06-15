package org.jcodec.common;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.jcodec.common.Tuple._2;
import org.jcodec.containers.mp4.boxes.Box;
import org.jcodec.containers.mp4.boxes.MovieFragmentBox;

/**
 * 
 * @author Jay Codec
 * 
 */
public class Tuple {

    public static class _1<T0> {
        public final T0 v0;

        public _1(T0 v0) {
            this.v0 = v0;
        }
    }

    public static class _2<T0, T1> {
        public final T0 v0;
        public final T1 v1;

        public _2(T0 v0, T1 v1) {
            this.v0 = v0;
            this.v1 = v1;
        }
    }

    public static class _3<T0, T1, T2> {
        public final T0 v0;
        public final T1 v1;
        public final T2 v2;

        public _3(T0 v0, T1 v1, T2 v2) {
            this.v0 = v0;
            this.v1 = v1;
            this.v2 = v2;
        }
    }

    public static class _4<T0, T1, T2, T3> {
        public final T0 v0;
        public final T1 v1;
        public final T2 v2;
        public final T3 v3;

        public _4(T0 v0, T1 v1, T2 v2, T3 v3) {
            this.v0 = v0;
            this.v1 = v1;
            this.v2 = v2;
            this.v3 = v3;
        }
    }

    public static <T0> _1<T0> _1(T0 v0) {
        return new _1<T0>(v0);
    }

    public static <T0, T1> _2<T0, T1> _2(T0 v0, T1 v1) {
        return new _2<T0, T1>(v0, v1);
    }

    public static <T0, T1, T2> _3<T0, T1, T2> _3(T0 v0, T1 v1, T2 v2) {
        return new _3<T0, T1, T2>(v0, v1, v2);
    }

    public static <T0, T1, T2, T3> _4<T0, T1, T2, T3> _4(T0 v0, T1 v1, T2 v2, T3 v3) {
        return new _4<T0, T1, T2, T3>(v0, v1, v2, v3);
    }

    public static <T0, T1> Map<T0, T1> asMap(Iterable<_2<T0, T1>> it) {
        HashMap<T0, T1> result = new HashMap<T0, T1>();
        for (_2<T0, T1> el : it) {
            result.put(el.v0, el.v1);
        }
        return result;
    }

    public static <T0, T1> Map<T0, T1> asMap(_2<T0, T1>[] arr) {
        HashMap<T0, T1> result = new HashMap<T0, T1>();
        for (_2<T0, T1> el : arr) {
            result.put(el.v0, el.v1);
        }
        return result;
    }

    public static <T0, T1> List<_2<T0, T1>> asList(Map<T0, T1> m) {
        LinkedList<_2<T0, T1>> result = new LinkedList<_2<T0, T1>>();
        Set<Entry<T0, T1>> entrySet = m.entrySet();
        for (Entry<T0, T1> entry : entrySet) {
            result.add(_2(entry.getKey(), entry.getValue()));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public static <T0, T1> _2<T0, T1>[] asArray(Map<T0, T1> m) {
        _2<T0, T1>[] result = (_2<T0, T1>[]) (new Object[m.size()]);
        Set<Entry<T0, T1>> entrySet = m.entrySet();
        int i = 0;
        for (Entry<T0, T1> entry : entrySet) {
            result[i++] = _2(entry.getKey(), entry.getValue());
        }
        return result;
    }

    public static <T0> List<T0> _1_project0(List<_1<T0>> temp) {
        List<T0> result = new LinkedList<T0>();
        for (_1<T0> _1 : temp) {
            result.add(_1.v0);
        }
        return result;
    }

    public static <T0, T1> List<T0> _2_project0(List<_2<T0, T1>> temp) {
        List<T0> result = new LinkedList<T0>();
        for (_2<T0, T1> _2 : temp) {
            result.add(_2.v0);
        }
        return result;
    }

    public static <T0, T1> List<T1> _2_project1(List<_2<T0, T1>> temp) {
        List<T1> result = new LinkedList<T1>();
        for (_2<T0, T1> _2 : temp) {
            result.add(_2.v1);
        }
        return result;
    }

    public static <T0, T1, T2, T3> List<T0> _3_project0(List<_3<T0, T1, T2>> temp) {
        List<T0> result = new LinkedList<T0>();
        for (_3<T0, T1, T2> _3 : temp) {
            result.add(_3.v0);
        }
        return result;
    }

    public static <T0, T1, T2, T3> List<T1> _3_project1(List<_3<T0, T1, T2>> temp) {
        List<T1> result = new LinkedList<T1>();
        for (_3<T0, T1, T2> _3 : temp) {
            result.add(_3.v1);
        }
        return result;
    }

    public static <T0, T1, T2, T3> List<T2> _3_project2(List<_3<T0, T1, T2>> temp) {
        List<T2> result = new LinkedList<T2>();
        for (_3<T0, T1, T2> _3 : temp) {
            result.add(_3.v2);
        }
        return result;
    }

    public static <T0, T1, T2, T3> List<T0> _4_project0(List<_4<T0, T1, T2, T3>> temp) {
        List<T0> result = new LinkedList<T0>();
        for (_4<T0, T1, T2, T3> _4 : temp) {
            result.add(_4.v0);
        }
        return result;
    }

    public static <T0, T1, T2, T3> List<T1> _4_project1(List<_4<T0, T1, T2, T3>> temp) {
        List<T1> result = new LinkedList<T1>();
        for (_4<T0, T1, T2, T3> _4 : temp) {
            result.add(_4.v1);
        }
        return result;
    }

    public static <T0, T1, T2, T3> List<T2> _4_project2(List<_4<T0, T1, T2, T3>> temp) {
        List<T2> result = new LinkedList<T2>();
        for (_4<T0, T1, T2, T3> _4 : temp) {
            result.add(_4.v2);
        }
        return result;
    }

    public static <T0, T1, T2, T3> List<T3> _4_project3(List<_4<T0, T1, T2, T3>> temp) {
        List<T3> result = new LinkedList<T3>();
        for (_4<T0, T1, T2, T3> _4 : temp) {
            result.add(_4.v3);
        }
        return result;
    }
}
