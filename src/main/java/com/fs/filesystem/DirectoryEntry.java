package com.fs.filesystem;

/**
 * @author Medyskyi Mykola
 */
public class DirectoryEntry {
    public String fileName;
    public int fileDescriptorIndex;

    public DirectoryEntry(String fileName, int fileDescriptorIndex) {
        this.fileDescriptorIndex = fileDescriptorIndex;
        this.fileName = fileName;
    }
}
