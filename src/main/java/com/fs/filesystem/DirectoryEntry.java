package com.fs.filesystem;

public class DirectoryEntry {
    String fileName;
    int fileDescriptorIndex;

    public DirectoryEntry(String fileName, int fileDescriptorIndex) {
        this.fileDescriptorIndex = fileDescriptorIndex;
        this.fileName = fileName;
    }
}
