package org.jcodec.containers.mkv;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.junit.Test;

public class GenericFactoryTest {

    @Test
    public void test() throws Exception {
        GF g = GF.A;
        A a = GF.getClass(GF.A);
        a.a();

        B b = GF.getClass(GF.B);
        b.b();
    }

    public static class A extends C {

        public A(byte[] b) {
            super(b);
            // TODO Auto-generated constructor stub
        }

        public void a() {
            System.out.println("A");
        }
    }

    public static class B extends C {
        
        public B(byte[] b) {
            super(b);
            // TODO Auto-generated constructor stub
        }

        public void b() {
            System.out.println("B");
        }
    }

    public static class C {
        public C(byte[] b) {

        }
    }

    public static enum GF {
        A(A.class, new byte[]{(byte)0x81}), B(B.class, new byte[]{(byte)0x82});

        public final Class clazz;
        public final byte[] id;

        private <Z extends C> GF(Class<Z> clazz, byte[] id) {
            this.clazz = clazz;
            this.id = id;
        }
        
        @SuppressWarnings("unchecked")
        public static <T extends C> T getClass(GF g) throws SecurityException, NoSuchMethodException, IllegalArgumentException, InvocationTargetException, InstantiationException,
        IllegalAccessException {
            return (T) GF.getClass(g.clazz, g.id);
       }

        private static <T extends C> T getClass(Class<T> clazz, byte[] id) throws SecurityException, NoSuchMethodException, IllegalArgumentException, InvocationTargetException, InstantiationException,
                IllegalAccessException {
            Constructor<T> c = clazz.getConstructor(byte[].class);
            return c.newInstance(id);
        }
    }
}
