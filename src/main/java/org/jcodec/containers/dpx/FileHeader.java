package org.jcodec.containers.dpx;

import java.util.Date;

public class FileHeader {
    public int magic; // SDPX big endian (0x53445058) or XPDS little endinan 
    public int imageOffset; //Offset to image data in bytes
    public String version;
    public int ditto;
    public String filename;
    public Date created;
    public int filesize;
    public String creator;
    public String projectName;
    public String copyright;
    public int encKey;
//        public byte[] reserved;

    public int genericHeaderLength;
    public int industryHeaderLength;
    public int userHeaderLength;
}
