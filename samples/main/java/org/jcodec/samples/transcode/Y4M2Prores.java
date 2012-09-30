package org.jcodec.samples.transcode;

public class Y4M2Prores {
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Syntax: <y4m> <prores>");
            return;
        }
        TranscodeMain.y4m2prores(args[0], args[1]);
    }
}
