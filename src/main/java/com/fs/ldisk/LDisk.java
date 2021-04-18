package com.fs.ldisk;

import com.fs.utils.FileSystemConfig;

public class LDisk {
    public byte[][] bytes;

    public LDisk() {
        bytes = new byte[FileSystemConfig.BLOCKS_AMOUNT][FileSystemConfig.BLOCK_LENGTH];
    }
}
