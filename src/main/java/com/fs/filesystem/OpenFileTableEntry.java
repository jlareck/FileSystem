package com.fs.filesystem;

import com.fs.ldisk.LDisk;

public class OpenFileTableEntry {
    /**
     * Contains buffered data
     */
    byte[] readWriteBuffer;
    /**
     * Current position in file. Index of the file byte
     */
    int currentPositionInFile;
    /**
     * Index of file descriptor for the file
     */
    int fileDescriptorIndex;
    boolean bufferModified;
    int fileBlockInBuffer;

    public OpenFileTableEntry() {
        readWriteBuffer = new byte[LDisk.BLOCK_LENGTH];
        currentPositionInFile = 0;
        fileDescriptorIndex = -1;
        bufferModified = false;
        fileBlockInBuffer = -1;
    }

    /**
     * @return Number of file data block that our entry is currently buffering
     */
    public int getCurrentDataBlockPosition() {
        return currentPositionInFile / LDisk.BLOCK_LENGTH;
    }

    /**
     * @return Current position in readWriteBuffer
     */
    public int getCurrentBufferPosition() {
        return currentPositionInFile % LDisk.BLOCK_LENGTH;
    }
}
