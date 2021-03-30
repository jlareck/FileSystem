package com.fs.filesystem;

import com.fs.iosystem.IOSystem;
import com.fs.ldisk.LDisk;

import java.util.BitSet;

public class FileSystem {

    public IOSystem ioSystem;
    public BitSet bitmap;

    public FileSystem(IOSystem ioSystem) {
        this.ioSystem = ioSystem;
        // bitmap setting true: 1 for bitmap, 6 for descriptors, 3 for directory
        bitmap = new BitSet(64);
        bitmap.set(0,10,true);
    }

}
