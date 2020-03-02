package org.jcodec.movtool;

import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.RationalLarge;
import org.jcodec.containers.mp4.MP4Util;
import org.jcodec.containers.mp4.boxes.MovieBox;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.jcodec.movtool.MoovVersions.addVersion;
import static org.jcodec.movtool.MoovVersions.listMoovVersionAtoms;
import static org.junit.Assert.assertEquals;

public class MoovVersionsTest {

    File tempFile;

    @Before
    public void setup() throws IOException {
        tempFile = File.createTempFile("testVersions", ".mp4");
        tempFile.deleteOnExit();
        NIOUtils.copyFile(new File("src/test/resources/applegps/gps1.mp4"), tempFile);
    }

    @After
    public void teardown() {
        tempFile.delete();
    }

    private static double durationSec(MovieBox moov) {
        return (double) moov.getDuration() / moov.getTimescale();
    }

    @Test
    public void testAddVersion() throws IOException {
        List<MP4Util.Atom> versions = listMoovVersionAtoms(tempFile);
        assertEquals(1, versions.size());

        MovieBox moov = MP4Util.parseMovie(tempFile);
        assertEquals(2.63, durationSec(moov), 0.01);

        //dummy example - set fps to 42fps
        //refer to org.jcodec.movtool for more moov modification examples
        SetFPS.SetFPSEdit edit = new SetFPS.SetFPSEdit(new RationalLarge(42, 1));
        edit.apply(moov);
        assertEquals(1.88, durationSec(moov), 0.01);

        addVersion(tempFile, moov);

        assertEquals(2, listMoovVersionAtoms(tempFile).size());

        MovieBox reparsed = MP4Util.parseMovie(tempFile);
        assertEquals(1.88, durationSec(reparsed), 0.01);

    }

    @Test
    public void testUndo() throws IOException {
        testAddVersion();
        MoovVersions.undo(tempFile);
        assertEquals(1, listMoovVersionAtoms(tempFile).size());

        MovieBox reparsed = MP4Util.parseMovie(tempFile);
        assertEquals(2.63, durationSec(reparsed), 0.01);
    }

    @Test
    public void testRollback() throws Exception {
        //add moov version with fps == 42
        testAddVersion();

        MovieBox moov = MP4Util.parseMovie(tempFile);

        //add moov version with fps == 84
        SetFPS.SetFPSEdit edit = new SetFPS.SetFPSEdit(new RationalLarge(84, 1));
        edit.apply(moov);
        //duration changed because fps increased but number of frames did not
        assertEquals(0.94, durationSec(moov), 0.01);

        addVersion(tempFile, moov);

        //now tempFile has 3 versions
        assertEquals(3, listMoovVersionAtoms(tempFile).size());

        //rollback to first version
        MoovVersions.rollback(tempFile, listMoovVersionAtoms(tempFile).get(0));

        //check duration is back at original value
        MovieBox reparsed = MP4Util.parseMovie(tempFile);
        assertEquals(2.63, durationSec(reparsed), 0.01);
    }
}
