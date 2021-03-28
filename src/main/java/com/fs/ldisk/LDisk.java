package com.fs.ldisk;

public class LDisk {
    public static final int BLOCK_LENGTH = 64;
    public static final int BLOCKS_AMOUNT = 64;
    public byte[][] bytes;

    public LDisk() {
        bytes = new byte[BLOCKS_AMOUNT][BLOCK_LENGTH];
    }
}
