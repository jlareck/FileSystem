package com.fs.filesystem;

import com.fs.iosystem.IOSystem;
import com.fs.ldisk.LDisk;
import com.fs.utils.FileSystemConfig;

import java.util.BitSet;

public class FileSystem {

    public IOSystem ioSystem;
    public BitSet bitmap;
    public FileDescriptor[] descriptors;
    public Directory directory;

    /**
     * This constructor initialise bitmap and sets for first 10 bits - true flag.
     * This means that first 10 blocks on disk are reserved for bitmap (1 block, 8 bytes in it),
     * descriptors (6 blocks, 16 bytes per descriptor (4 bytes for file length and 12 for file
     * content blocks indexes), so in one block we have 4 descriptors) and 3 blocks for directory
     * (those blocks contain entries from directory, each of them are 8 bytes (4 bytes for file name
     * and 4 for descriptor index)). We also initialize first descriptor which contains indexes
     * to 3 data blocks of directory.
     *
     * @param ioSystem
     *
     */
    public FileSystem(IOSystem ioSystem) {
        this.ioSystem = ioSystem;

        // bitmap setting true: 1 for bitmap, 6 for descriptors, 3 for directory
        bitmap = new BitSet(LDisk.BLOCKS_AMOUNT);
        bitmap.set(0,10,true);
        this.ioSystem.writeBlock(0, bitmap.toByteArray());
        directory = new Directory();
        descriptors = new FileDescriptor[FileSystemConfig.NUMBER_OF_DESCRIPTORS];
        // index where data blocks starts (maybe it will better to change in later)
        int dataBlocksStartingPosition = 7;
        descriptors[0] = new FileDescriptor(0,
                new int[]{dataBlocksStartingPosition, dataBlocksStartingPosition+1, dataBlocksStartingPosition+2});

    }

    /**
     * Create method - creates file with given file name. If file name is larger
     * than maximum file name length or the length is less than 1, it prints a message
     * and returns status ERROR which is equal to -1;
     *
     * It also return ERROR when there is no free file descriptor or if the file already exists
     * with the given fileName in the file system.
     *
     * If the file can be created than it returns SUCCESS status (it is equal to 1)
     *
     * @param fileName
     *
     */
    public int create(String fileName) {
        if (fileName.length() < 1 || fileName.length() > FileSystemConfig.MAXIMUM_FILE_NAME_LENGTH) {
            System.out.println("ERROR! File name is larger than maximum length or it is less than 1");
            return FileSystemConfig.ERROR;
        }
        int freeDescriptorIndex = -1;
        for (int i = 0; i < descriptors.length; i++) {
            if (descriptors[i] == null) {
                freeDescriptorIndex = i;
                break;
            }
        }
        if (freeDescriptorIndex == -1) {
            System.out.println("ERROR! THERE IS NO FREE DESCRIPTOR IN THE FILESYSTEM");
            return FileSystemConfig.ERROR;
        }
        for (int i = 0; i < directory.listOfEntries.size(); i++) {
            if (directory.listOfEntries.get(i).fileName.equals(fileName)) {
                System.out.println("ERROR! THE " + fileName + " ALREADY EXISTS");
                return FileSystemConfig.ERROR;
            }
        }
        directory.addEntryToDirectory(fileName, freeDescriptorIndex);
        descriptors[freeDescriptorIndex] = new FileDescriptor();
        System.out.println("The file " + fileName+ " has been created successfully");
        return FileSystemConfig.SUCCESS;
    }


    /**
     * Destroy method - destroys the file from the file system. If file name is larger
     * than maximum file name length or the length is less than 1, it prints a message
     * and returns status ERROR which is equal to -1;
     *
     * If it there is no file with given file name, it will print a message about it and return
     * ERROR status.
     *
     * If the file can be destroyed, it will return SUCCESS status
     *
     * @param fileName
     *
     */
    public int destroy(String fileName) {
        if (fileName.length() == 0 || fileName.length() > FileSystemConfig.MAXIMUM_FILE_NAME_LENGTH) {
            System.out.println("ERROR! File name is larger than maximum length or it is equal to zero");
            return FileSystemConfig.ERROR;
        }
        int descriptorIndex = findDescriptorIndex(fileName);
        if (descriptorIndex == -1) {
            System.out.println("ERROR! There is no file with file name: " + fileName);
            return FileSystemConfig.ERROR;
        }

        for (int i = 0; i < descriptors[descriptorIndex].fileContentsDiskBlocks.length; i++) {
            if (descriptors[descriptorIndex].fileContentsDiskBlocks[i] != -1) {
                bitmap.set(descriptors[descriptorIndex].fileContentsDiskBlocks[i], false);
                ioSystem.writeBlock(descriptors[descriptorIndex].fileContentsDiskBlocks[i], new byte[64]);
            }
        }
        directory.listOfEntries.remove(findDirectoryEntryIndex(descriptorIndex));
        descriptors[descriptorIndex] = null;
        System.out.println("The file " + fileName+ " has been destroyed successfully");
        return FileSystemConfig.SUCCESS;

    }
    private int findDescriptorIndex(String fileName) {
        for (int i = 0; i < directory.listOfEntries.size(); i++) {
            if (directory.listOfEntries.get(i).fileName.equals(fileName)) {
                return directory.listOfEntries.get(i).fileDescriptorIndex;
            }
        }
        return -1;
    }
    private int findDirectoryEntryIndex(int descriptorIndex) {
        for (int i = 0; i < FileSystemConfig.NUMBER_OF_DESCRIPTORS - 1; i++) {
            if (directory.listOfEntries.get(i) != null &&
                    directory.listOfEntries.get(i).fileDescriptorIndex == descriptorIndex) {
                return i;
            }
        }
        return -1;
    }

}
