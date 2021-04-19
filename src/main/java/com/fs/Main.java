package com.fs;

import com.fs.filesystem.FileSystem;
import com.fs.iosystem.IOSystem;
import com.fs.ldisk.LDisk;
import com.fs.shell.Shell;

public class Main {

    public static void main(String[] args) {

        LDisk lDisk = new LDisk();
        IOSystem ioSystem = new IOSystem(lDisk);
        FileSystem fileSystem = new FileSystem(ioSystem);
        Shell shell = new Shell(ioSystem, fileSystem);
        shell.begin();
    }
}
