package com.fs.iosystem;

import com.fs.ldisk.LDisk;
import com.fs.utils.FileSystemConfig;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;

/**
 * @author Nikita Pupov
 */
public class IOSystem {

    private LDisk lDisk;

    public IOSystem(LDisk lDisk) {
        this.lDisk = lDisk;
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

    /**
     * @author Medynskyi Mykola
     * @contributor Taisiia Fenz
     *
     * saving lDisk to txt file
     */
    public void saveDiskToFile(String filePath) {
        try {
            FileOutputStream fileOut = new FileOutputStream(filePath);
            ObjectOutputStream objectOut = new ObjectOutputStream(fileOut);
            objectOut.writeObject(lDisk);
            objectOut.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * @author Medysnkyi Mykola
     * @contributor Taisiia Fenz
     *
     * reading disk from txt file
     *
     */
    public LDisk readDiskFromFile(String filePath) {
        try {
            FileInputStream fileIn = new FileInputStream(filePath);
            ObjectInputStream objectIn = new ObjectInputStream(fileIn);

            LDisk obj = (LDisk) objectIn.readObject();

            objectIn.close();
            return obj;

        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

}
