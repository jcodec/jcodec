package org.jcodec.common;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * @author The JCodec project
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

    public static interface Mapper<T, U> {
        U map(T t);
    }

    public static <T0, U> List<_1<U>> _1map0(List<_1<T0>> l, Mapper<T0, U> mapper) {
        LinkedList<_1<U>> result = new LinkedList<_1<U>>();
        for (_1<T0> _1 : l) {
            result.add(_1(mapper.map(_1.v0)));
        }
        return result;
    }

    public static <T0, T1, U> List<_2<U, T1>> _2map0(List<_2<T0, T1>> l, Mapper<T0, U> mapper) {
        LinkedList<_2<U, T1>> result = new LinkedList<_2<U, T1>>();
        for (_2<T0, T1> _2 : l) {
            result.add(_2(mapper.map(_2.v0), _2.v1));
        }
        return result;
    }

    public static <T0, T1, U> List<_2<T0, U>> _2map1(List<_2<T0, T1>> l, Mapper<T1, U> mapper) {
        LinkedList<_2<T0, U>> result = new LinkedList<_2<T0, U>>();
        for (_2<T0, T1> _2 : l) {
            result.add(_2(_2.v0, mapper.map(_2.v1)));
        }
        return result;
    }

    public static <T0, T1, T2, U> List<_3<U, T1, T2>> _3map0(List<_3<T0, T1, T2>> l, Mapper<T0, U> mapper) {
        LinkedList<_3<U, T1, T2>> result = new LinkedList<_3<U, T1, T2>>();
        for (_3<T0, T1, T2> _3 : l) {
            result.add(_3(mapper.map(_3.v0), _3.v1, _3.v2));
        }
        return result;
    }

    public static <T0, T1, T2, U> List<_3<T0, U, T2>> _3map1(List<_3<T0, T1, T2>> l, Mapper<T1, U> mapper) {
        LinkedList<_3<T0, U, T2>> result = new LinkedList<_3<T0, U, T2>>();
        for (_3<T0, T1, T2> _3 : l) {
            result.add(_3(_3.v0, mapper.map(_3.v1), _3.v2));
        }
        return result;
    }

    public static <T0, T1, T2, U> List<_3<T0, T1, U>> _3map3(List<_3<T0, T1, T2>> l, Mapper<T2, U> mapper) {
        LinkedList<_3<T0, T1, U>> result = new LinkedList<_3<T0, T1, U>>();
        for (_3<T0, T1, T2> _3 : l) {
            result.add(_3(_3.v0, _3.v1, mapper.map(_3.v2)));
        }
        return result;
    }

    public static <T0, T1, T2, T3, U> List<_4<U, T1, T2, T3>> _4map0(List<_4<T0, T1, T2, T3>> l, Mapper<T0, U> mapper) {
        LinkedList<_4<U, T1, T2, T3>> result = new LinkedList<_4<U, T1, T2, T3>>();
        for (_4<T0, T1, T2, T3> _4 : l) {
            result.add(_4(mapper.map(_4.v0), _4.v1, _4.v2, _4.v3));
        }
        return result;
    }

    public static <T0, T1, T2, T3, U> List<_4<T0, U, T2, T3>> _4map1(List<_4<T0, T1, T2, T3>> l, Mapper<T1, U> mapper) {
        LinkedList<_4<T0, U, T2, T3>> result = new LinkedList<_4<T0, U, T2, T3>>();
        for (_4<T0, T1, T2, T3> _4 : l) {
            result.add(_4(_4.v0, mapper.map(_4.v1), _4.v2, _4.v3));
        }
        return result;
    }

    public static <T0, T1, T2, T3, U> List<_4<T0, T1, U, T3>> _4map3(List<_4<T0, T1, T2, T3>> l, Mapper<T2, U> mapper) {
        LinkedList<_4<T0, T1, U, T3>> result = new LinkedList<_4<T0, T1, U, T3>>();
        for (_4<T0, T1, T2, T3> _4 : l) {
            result.add(_4(_4.v0, _4.v1, mapper.map(_4.v2), _4.v3));
        }
        return result;
    }

    public static <T0, T1, T2, T3, U> List<_4<T0, T1, T2, U>> _4map4(List<_4<T0, T1, T2, T3>> l, Mapper<T3, U> mapper) {
        LinkedList<_4<T0, T1, T2, U>> result = new LinkedList<_4<T0, T1, T2, U>>();
        for (_4<T0, T1, T2, T3> _4 : l) {
            result.add(_4(_4.v0, _4.v1, _4.v2, mapper.map(_4.v3)));
        }
        return result;
    }
}
