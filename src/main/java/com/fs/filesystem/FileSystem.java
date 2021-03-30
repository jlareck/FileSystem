package com.fs.filesystem;

import com.fs.iosystem.IOSystem;
import com.fs.ldisk.LDisk;

public class FileSystem {

    public IOSystem ioSystem;
    public byte[] bitmap;

    public FileSystem(IOSystem ioSystem) {
        this.ioSystem = ioSystem;
        bitmap = new byte[LDisk.BLOCKS_AMOUNT];
    }

}
