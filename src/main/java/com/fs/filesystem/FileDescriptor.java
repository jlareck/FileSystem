package com.fs.filesystem;

public class FileDescriptor {

    public int fileLength; // in bytes
    public int[] fileContentsDiskBlocks;

    public FileDescriptor(int fileLength, int[] fileContentsDiskBlocks) {
        this.fileLength = fileLength;
        this.fileContentsDiskBlocks = fileContentsDiskBlocks;
    }
}
