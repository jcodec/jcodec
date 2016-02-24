package org.jcodec.containers.mps.psi;

import java.nio.ByteBuffer;

import org.jcodec.common.IntArrayList;
import org.jcodec.common.IntIntMap;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Represents PAT ( Program Association Table ) PSI payload of MPEG Transport
 * stream
 * 
 * @author The JCodec project
 * 
 */
public class PATSection extends PSISection {
    private int[] networkPids;
    private IntIntMap programs;

    public PATSection(PSISection psi, int[] networkPids, IntIntMap programs) {
        super(psi);
        this.networkPids = networkPids;
        this.programs = programs;
    }

    public int[] getNetworkPids() {
        return networkPids;
    }

    public IntIntMap getPrograms() {
        return programs;
    }

    public static PATSection parsePAT(ByteBuffer data) {
        PSISection psi = PSISection.parsePSI(data);

        IntArrayList networkPids = new IntArrayList();
        IntIntMap programs = new IntIntMap();

        while (data.remaining() > 4) {
            int programNum = data.getShort() & 0xffff;
            int w = data.getShort();
            int pid = w & 0x1fff;
            if (programNum == 0)
                networkPids.add(pid);
            else
                programs.put(programNum, pid);
        }

        return new PATSection(psi, networkPids.toArray(), programs);
    }
}