package com.fs.filesystem;

public class FileDescriptor {

    public int fileLength; // in bytes
    public int[] fileContentsInDiskBlocks;

    public FileDescriptor(int fileLength, int[] fileContentsInDiskBlocks) {
        this.fileLength = fileLength;
        this.fileContentsInDiskBlocks = fileContentsInDiskBlocks;
    }
    public FileDescriptor() {
        fileLength = 0;
        fileContentsInDiskBlocks = new int[]{-1,-1,-1};
    }
}
