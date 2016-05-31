package org.jcodec.containers.mkv;
import org.jcodec.Utils;
import org.jcodec.common.io.IOUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Properties;

public class MKVTestSuite {
    
    public final File test1;
    public final File test2;
    public final File test3;
    public final File test4;
    public final File test5;
    public final File test6;
    public final File test7;
    public final File test8;
    public final File base;

    public MKVTestSuite(File dir) {
        base = dir;
        test1 = new File(dir, "test1.mkv");
        test2 = new File(dir, "test2.mkv");
        test3 = new File(dir, "test3.mkv");
        test4 = new File(dir, "test4.mkv");
        test5 = new File(dir, "test5.mkv");
        test6 = new File(dir, "test6.mkv");
        test7 = new File(dir, "test7.mkv");
        test8 = new File(dir, "test8.mkv");
    }
    
    public boolean isSuitePresent() {
        return test1.exists() && test2.exists() && test3.exists() && test4.exists() && test5.exists() && test6.exists() && test7.exists() && test8.exists();
    }

    public File[] allTests(){
        return new File[]{test1, test2, test3, test4, test5, test6, test7, test8};
    }

    public static Properties loadProperties(File file) throws IOException {
        Reader reader = null;
        try {
            Properties props = new Properties();
            if (file.exists()) {
                reader = new BufferedReader(new FileReader(file));
                props.load(reader);
            }
            return props;
        } finally {
            IOUtils.closeQuietly(reader);
        }
    }
    
    public static MKVTestSuite read() throws IOException {
        return readFile(new File("./src/test/resources/mkv/suite.properties"));
    }

    public static MKVTestSuite readFile(File f) throws IOException {
        if (!f.exists())
            throw new RuntimeException(f.getAbsolutePath() + " doesn't exists");
        
        Properties props = loadProperties(f);
        String path = props.getProperty("mkv.test.suite.path");
        File dir = Utils.tildeExpand(path);

        return new MKVTestSuite(dir);
    }
}
