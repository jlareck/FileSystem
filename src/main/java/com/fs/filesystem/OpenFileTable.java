package com.fs.filesystem;

import com.fs.utils.FileSystemConfig;

public class OpenFileTable {
    OpenFileTableEntry[] entries;

    public OpenFileTable() {
        entries = new OpenFileTableEntry[FileSystemConfig.OFT_NUMBER_OF_ENTRIES];
    }
}
