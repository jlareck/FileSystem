package com.fs.filesystem;

import com.fs.iosystem.IOSystem;
import com.fs.ldisk.LDisk;

import java.nio.ByteBuffer;

public class OpenFileTableEntry {
    byte[] readWriteBuffer;
    int currentPositionIndex;
    int fileDescriptorIndex;

    public OpenFileTableEntry() {
        readWriteBuffer = new byte[LDisk.BLOCK_LENGTH];
        currentPositionIndex = 0;
        fileDescriptorIndex = -1;
    }
}
