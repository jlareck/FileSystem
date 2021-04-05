package com.fs.filesystem;

import com.fs.utils.FileSystemConfig;

public class OpenFileTable {
    final public static int OFT_NUMBER_OF_ENTRIES = 4;

    public OpenFileTableEntry[] entries;

    public OpenFileTable() {
        entries = new OpenFileTableEntry[OFT_NUMBER_OF_ENTRIES];
    }

    public int getOFTEntryIndexByFDIndex(int fileDescriptorIndex) {
        for (int i = 1; i < entries.length; i++) {
            if (entries[i] != null && entries[i].fileDescriptorIndex == fileDescriptorIndex) {
                return i;
            }
        }

        return -1;
    }

    public int getOFTFreeEntryIndex() {
        for (int i = 1; i < entries.length; i++) {
            if(entries[i] == null) {
                return i;
            }
        }
        return -1;
    }
}
