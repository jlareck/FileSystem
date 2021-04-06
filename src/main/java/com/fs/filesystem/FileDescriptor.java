package com.fs.filesystem;

public class FileDescriptor {

    public int fileLength; // in bytes
    public int[] fileContentsBlocksIndexes;

    public FileDescriptor(int fileLength, int[] fileContentsBlocksIndexes) {
        this.fileLength = fileLength;
        this.fileContentsBlocksIndexes = fileContentsBlocksIndexes;
    }
    public FileDescriptor() {
        fileLength = 0;
        fileContentsBlocksIndexes = new int[]{-1,-1,-1};
    }
}
