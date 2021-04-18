package com.fs.ldisk;

import com.fs.utils.FileSystemConfig;

import java.io.Serializable;

public class LDisk implements Serializable {
    public byte[][] bytes;

    public LDisk() {
        bytes = new byte[FileSystemConfig.BLOCKS_AMOUNT][FileSystemConfig.BLOCK_LENGTH];
    }
}
