package com.fs.filesystem;

import com.fs.utils.FileSystemConfig;

/**
 * @author Nikita Pupov, Mykola Medynskyi, Taisiia Fenz
 */

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
    /**
     * Was buffer modified or not
     */
    boolean bufferModified;
    /**
     * File block which is stored in buffer
     */
    int fileBlockInBuffer;

    public OpenFileTableEntry() {
        readWriteBuffer = new byte[FileSystemConfig.BLOCK_LENGTH];
        currentPositionInFile = 0;
        fileDescriptorIndex = -1;
        bufferModified = false;
        fileBlockInBuffer = -1;
    }

    /**
     * @return Number of file data block that our entry is currently buffering
     */
    public int getCurrentDataBlockPosition() {
        return currentPositionInFile / FileSystemConfig.BLOCK_LENGTH;
    }

    /**
     * @return Current position in readWriteBuffer
     */
    public int getCurrentBufferPosition() {
        return currentPositionInFile % FileSystemConfig.BLOCK_LENGTH;
    }
}
