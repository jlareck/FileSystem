package com.fs.utils;

public class FileSystemConfig {
    final public static int NUMBER_OF_DESCRIPTOR_BLOCKS = 6;
    final public static int NUMBER_OF_DESCRIPTORS = 24;
    final public static int NUMBER_OF_DESCRIPTORS_IN_ONE_BLOCK = 4;
    final public static int SIZE_OF_DESCRIPTOR = 16;
    final public static int MAXIMUM_FILE_NAME_LENGTH = 4;
    final public static int ERROR = -1;
    final public static int SUCCESS = 1;
    final public static int MAXIMUM_NUMBER_OF_BLOCKS_PER_FILE = 3;
    final public static int BITMAP_LENGTH_ON_DISK = 8;
    final public static int MAXIMUM_DIRECTORY_ENTRIES_PER_BLOCK = 8;

    public static final int BLOCK_LENGTH = 64;
    public static final int BLOCKS_AMOUNT = 64;
}
