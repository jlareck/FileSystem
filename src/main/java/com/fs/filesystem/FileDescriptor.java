package com.fs.filesystem;

import java.util.Arrays;

public class FileDescriptor {

    public int fileLength; // in bytes
    public int[] fileContentsBlocksIndexes;

    public FileDescriptor(int fileLength, int[] fileContentsBlocksIndexes) {
        this.fileLength = fileLength;
        this.fileContentsBlocksIndexes = fileContentsBlocksIndexes;
    }
    public FileDescriptor() {
        fileLength = -1;
        fileContentsBlocksIndexes = new int[]{-1,-1,-1};
    }


    @Override
    public String toString() {
        return "FileDescriptor{" +
                "fileLength=" + fileLength +
                ", fileContentsBlocksIndexes=" + Arrays.toString(fileContentsBlocksIndexes) +
                '}';
    }
}
