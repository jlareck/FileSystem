package com.fs.iosystem;

import com.fs.ldisk.LDisk;
import com.fs.utils.FileSystemConfig;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;

public class IOSystem {

    private LDisk lDisk;

    public IOSystem(LDisk lDisk) {
        this.lDisk = lDisk;
    }

    public LDisk getlDisk() {
        return lDisk;
    }

    /**
     * This copies the logical block ldisk[i] into main memory starting at the location
     * specified by the pointer p. The number of characters copied corresponds to the
     * block length, B.
     *
     * @param blockIndex index of the block to read
     * @param buffer main memory, we'll store red block there
     */
    public void readBlock(int blockIndex, ByteBuffer buffer) {
        if (0 > blockIndex || blockIndex >= FileSystemConfig.BLOCKS_AMOUNT) {
            throw new IllegalArgumentException("Wrong block index for reading");
        }
        if (buffer.array().length != FileSystemConfig.BLOCK_LENGTH) {
            throw new IllegalArgumentException("Buffer length must be equal to block length");
        }

        for (int k = 0; k < FileSystemConfig.BLOCK_LENGTH; k++) {
            buffer.put(k, lDisk.bytes[blockIndex][k]);
        }
    }

    /**
     * Copies the number of character corresponding to the block length, B (blockLengthInBytes), from
     * main memory starting at the location specified by the pointer p, into the logical
     * block ldisk[blockNumber].
     *
     * @param blockIndex index of the block to write into
     * @param buffer array of bytes to write into disk
     */
    public void writeBlock(int blockIndex, byte[] buffer) {
        if (0 > blockIndex || blockIndex >= FileSystemConfig.BLOCKS_AMOUNT) {
            throw new IllegalArgumentException("Wrong block index for reading");
        }
        if (buffer.length != FileSystemConfig.BLOCK_LENGTH) {
            throw new IllegalArgumentException("Buffer length must be equal to block length");
        }

        for (int k = 0; k < FileSystemConfig.BLOCK_LENGTH; k++) {
            lDisk.bytes[blockIndex][k] = buffer[k];
        }
    }

    public void saveDiskToFile() {
        String filepath = "disk.txt";
        try {
            FileOutputStream fileOut = new FileOutputStream(filepath);
            ObjectOutputStream objectOut = new ObjectOutputStream(fileOut);
            objectOut.writeObject(lDisk);
            objectOut.close();
            System.out.println("The object was succesfully written to a file");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    public LDisk readDiskFromFile() {
        try {
            String filepath = "disk.txt";
            FileInputStream fileIn = new FileInputStream(filepath);
            ObjectInputStream objectIn = new ObjectInputStream(fileIn);

            LDisk obj = (LDisk) objectIn.readObject();

            System.out.println("The Object has been read from the file");
            objectIn.close();
            return obj;

        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

}
