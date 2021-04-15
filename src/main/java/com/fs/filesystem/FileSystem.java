package com.fs.filesystem;

import com.fs.iosystem.IOSystem;
import com.fs.ldisk.LDisk;
import com.fs.utils.FileSystemConfig;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.BitSet;

public class FileSystem {

    public IOSystem ioSystem;
    public BitSet bitmap;
    public FileDescriptor[] descriptors;
    public Directory directory;
    public OpenFileTable openFileTable;
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
        bitmap.set(0,8,true);
        saveBitMapToDisk(bitmap);

        directory = new Directory();
        descriptors = new FileDescriptor[FileSystemConfig.NUMBER_OF_DESCRIPTORS];
        // index where data blocks starts (maybe it will better to change in later)
        int dataBlocksStartingPosition = 7;
        // initializing descriptor for directory TODO: save directory descriptor to disk
        descriptors[0] = new FileDescriptor(0,
                new int[]{dataBlocksStartingPosition, -1, -1});

        openFileTable = new OpenFileTable();
        openFileTable.entries[0] = new OpenFileTableEntry();
        // OFT first entry for directory
        openFileTable.entries[0].fileDescriptorIndex = 0;
    }

    /**
     * Opens file by its fileName and places it in OFT
     * @param fileName name of the file
     * @return index of file in OFT
     */
    public int open(String fileName) {
        int fileDescriptorIndex = findDescriptorIndexByFileName(fileName);
        if(fileDescriptorIndex == -1) {
            System.out.println("ERROR! FILE " + fileName + " DOESN'T EXIST");
            return -1;
        }

        if(openFileTable.getOFTEntryIndexByFDIndex(fileDescriptorIndex) != -1) {
            System.out.println("ERROR! FILE " + fileName + " ALREADY OPENED");
            return -1;
        }

        int freeOFTEntryIndex = openFileTable.getOFTFreeEntryIndex();
        if(freeOFTEntryIndex == -1) {
            System.out.println("ERROR! NO MORE SPACE IN OFT");
            return -1;
        }

        OpenFileTableEntry openFileTableEntry = new OpenFileTableEntry();
        openFileTableEntry.fileDescriptorIndex = fileDescriptorIndex;

        //read first block of file to the buffer in OFT if file is not empty
        if(descriptors[fileDescriptorIndex].fileLength > 0) {
            ByteBuffer bytes = ByteBuffer.allocate(LDisk.BLOCK_LENGTH);
            ioSystem.readBlock(descriptors[fileDescriptorIndex].fileContentsBlocksIndexes[0], bytes);
            openFileTable.entries[freeOFTEntryIndex].fileBlockInBuffer = 0;
            openFileTableEntry.readWriteBuffer = bytes.array();
        }

        openFileTable.entries[freeOFTEntryIndex] = openFileTableEntry;

        return freeOFTEntryIndex;
    }

    /**
     * Finds OFT entry by index
     * Writes buffered data to LDisk
     * Removes entry from OFT
     * @param OFTEntryIndex index of file in OFT
     * @return 1 if everything is OK
     */
    public int close(int OFTEntryIndex) {
        if(OFTEntryIndex <= 0 || OFTEntryIndex >= OpenFileTable.OFT_NUMBER_OF_ENTRIES) {
            System.out.println("ERROR! WRONG INDEX");
            return -1;
        }

        OpenFileTableEntry entry = openFileTable.entries[OFTEntryIndex];

        if(entry == null) {
            System.out.println("ERROR! WRONG INDEX");
            return -1;
        }

        FileDescriptor fileDescriptor = descriptors[entry.fileDescriptorIndex];

        //when buffer closes, we must write buffer content to OFT
        if(fileDescriptor.fileLength > 0) {

            int currentBlockNumber = entry.getCurrentDataBlockPosition();

            int currentBlockNumberOnDisk = fileDescriptor.fileContentsBlocksIndexes[currentBlockNumber];

            ioSystem.writeBlock(currentBlockNumberOnDisk, entry.readWriteBuffer);
        }

        //remove OFT entry
        openFileTable.entries[OFTEntryIndex] = null;

        return 1;
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
        // Checking if there is already file in the directory with name: fileName
        for (int i = 0; i < directory.listOfEntries.size(); i++) {
            if (directory.listOfEntries.get(i).fileName.equals(fileName)) {
                System.out.println("ERROR! THE " + fileName + " ALREADY EXISTS");
                return FileSystemConfig.ERROR;
            }
        }
        // searching free descriptor
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

        directory.addEntryToDirectory(fileName, freeDescriptorIndex);

        int freeBlockIndex = searchFreeDataBlock(bitmap);
        bitmap.set(freeBlockIndex,true);
        saveBitMapToDisk(bitmap);
        // TODO: maybe it is better to read bitmap from disk before searching free block
        descriptors[freeDescriptorIndex] = new FileDescriptor(0, new int[]{freeBlockIndex,-1,-1});
        System.out.println("The file " + fileName+ " has been created successfully");
        // TODO: save directory and desriptor to disk

        //saveDescriptorsToDisk();
        return FileSystemConfig.SUCCESS;
    }
    public int searchFreeDataBlock(BitSet bits) {
        for (int i = 0; i < bits.size(); i++) {
            if (!bits.get(i)) {
                return i;
            }
        }
        return -1;
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
        int descriptorIndex = findDescriptorIndexByFileName(fileName);
        if (descriptorIndex == -1) {
            System.out.println("ERROR! There is no file with file name: " + fileName);
            return FileSystemConfig.ERROR;
        }

        for (int i = 0; i < descriptors[descriptorIndex].fileContentsBlocksIndexes.length; i++) {
            if (descriptors[descriptorIndex].fileContentsBlocksIndexes[i] != -1) {
                bitmap.set(descriptors[descriptorIndex].fileContentsBlocksIndexes[i], false);
                saveBitMapToDisk(bitmap);
                ioSystem.writeBlock(descriptors[descriptorIndex].fileContentsBlocksIndexes[i], new byte[64]);
            }
        }
        // TODO: save directory to disk
        directory.listOfEntries.remove(findDirectoryEntryIndex(descriptorIndex));
        descriptors[descriptorIndex] = null;
        System.out.println("The file " + fileName+ " has been destroyed successfully");
        return FileSystemConfig.SUCCESS;

    }

    public int read(int OFTEntryIndex, ByteBuffer memArea, int count) {
        if(OFTEntryIndex <= 0 || OFTEntryIndex >= OpenFileTable.OFT_NUMBER_OF_ENTRIES) {
            System.out.println("ERROR! WRONG INDEX");
            return -1;
        }
        if (count <= 0) {
            return -1;
        }

        OpenFileTableEntry entry = openFileTable.entries[OFTEntryIndex];

        if(entry == null) {
            System.out.println("ERROR! WRONG INDEX");
            return -1;
        }

        FileDescriptor fileDescriptor = descriptors[entry.fileDescriptorIndex];

        if(fileDescriptor.fileLength == 0) {
            return -1;
        }

        if (writeBuffer(entry, fileDescriptor) == -1) {
            return -1;
        }

        // find current position inside RWBuffer
        int currentBufferPosition = entry.currentPositionInFile % ioSystem.getlDisk().BLOCK_LENGTH;
        int currentMemoryPosition = 0;

        int counter = 0;

        // read count bytes starting at RWBuffer[currentBufferPosition] to memArea
        for (int i = 0; i < count && i < memArea.array().length; i++) {
            // if end of file -> return number of bytes read
            if (entry.currentPositionInFile == fileDescriptor.fileLength) {
                System.out.println("The end of the file is reached");
                return counter;
            } else {
                // if end of block -> write buffer to the disk, then read next block to RWBuffer
                if (currentBufferPosition == ioSystem.getlDisk().BLOCK_LENGTH) {

                    writeBuffer(entry, fileDescriptor);

                    currentBufferPosition = 0;
                }

                // read 1 byte to memory
                memArea.put(currentMemoryPosition, entry.readWriteBuffer[currentBufferPosition]);
                // update positions, counter
                counter++;
                currentBufferPosition++;
                currentMemoryPosition++;
                entry.currentPositionInFile++;
            }
        }

        // entry.currentPosition - points to first byte after last accessed
        return counter;
    }

    private int writeBuffer(OpenFileTableEntry entry, FileDescriptor fileDescriptor) {
        return -1;
    }

    private int findDescriptorIndexByFileName(String fileName) {
        // TODO: read directory from disk
        for (int i = 0; i < directory.listOfEntries.size(); i++) {
            if (directory.listOfEntries.get(i).fileName.equals(fileName)) {
                return directory.listOfEntries.get(i).fileDescriptorIndex;
            }
        }
        return -1;
    }
    private int findDirectoryEntryIndex(int descriptorIndex) {
        // TODO: read directory from disk
        for (int i = 0; i < FileSystemConfig.NUMBER_OF_DESCRIPTORS - 1; i++) {
            if (directory.listOfEntries.get(i) != null &&
                    directory.listOfEntries.get(i).fileDescriptorIndex == descriptorIndex) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Update bitmap on disk
     */
    public void saveBitMapToDisk(BitSet bitmap) {
        //convert bitMap to byte array
        byte[] bitSetBytes = bitmap.toByteArray();

        //read first block, because it contains bitmap(first 7 bytes)
        ByteBuffer diskBlockBuffer = ByteBuffer.allocate(LDisk.BLOCK_LENGTH);
        ioSystem.readBlock(0, diskBlockBuffer);

        //override bitMap in buffer
        for(int i=0;i<bitSetBytes.length;i++) {
            diskBlockBuffer.put(i, bitSetBytes[i]);
        }

        //flush buffer to disk
        byte[] bufferBytes = diskBlockBuffer.array();
        ioSystem.writeBlock(0, bufferBytes);
    }

    public void readDescriptorsFromDisk() {
        for (int i = 1; i <= FileSystemConfig.NUMBER_OF_DESCRIPTOR_BLOCKS; i++) {
            ByteBuffer diskBlockBuffer = ByteBuffer.allocate(LDisk.BLOCK_LENGTH);
            ioSystem.readBlock(i, diskBlockBuffer);
            for (int j = 0; j < FileSystemConfig.NUMBER_OF_DESCRIPTORS_IN_ONE_BLOCK; j++) {
                int lengthOfFile = diskBlockBuffer.getInt();
                if (lengthOfFile == -1) {
                    for (int k = 0; k < FileSystemConfig.MAXIMUM_NUMBER_OF_BLOCKS_PER_FILE; k++){
                        diskBlockBuffer.getInt();
                    }
                    // TODO: discuss if it is necessary to restore empty descriptors or not
                    descriptors[(i-1) * FileSystemConfig.NUMBER_OF_DESCRIPTORS_IN_ONE_BLOCK + j] = null;
                }
                else {
                    int[] dataBlocks = new int[FileSystemConfig.MAXIMUM_NUMBER_OF_BLOCKS_PER_FILE];
                    for (int k = 0; k < FileSystemConfig.MAXIMUM_NUMBER_OF_BLOCKS_PER_FILE; k++){
                        dataBlocks[k] = diskBlockBuffer.getInt();
                    }
                    descriptors[(i-1) * FileSystemConfig.NUMBER_OF_DESCRIPTORS_IN_ONE_BLOCK + j]
                            = new FileDescriptor(lengthOfFile, dataBlocks);
                }
            }
        }
    }

    public void saveDescriptorsToDisk() {
        for (int i = 1; i <= FileSystemConfig.NUMBER_OF_DESCRIPTOR_BLOCKS; i++) {
            ByteBuffer diskBlock = ByteBuffer.allocate(LDisk.BLOCK_LENGTH);
            for (int j = 0; j < FileSystemConfig.NUMBER_OF_DESCRIPTORS_IN_ONE_BLOCK; j++) {
                int currentDescriptor = (i-1) * FileSystemConfig.NUMBER_OF_DESCRIPTORS_IN_ONE_BLOCK + j;
                if (descriptors[currentDescriptor] == null) {
                    diskBlock.putInt(-1);
                    for (int k = 0; k < FileSystemConfig.MAXIMUM_NUMBER_OF_BLOCKS_PER_FILE; k++) {
                        diskBlock.putInt(-1);
                    }
                }
                else {
                    diskBlock.putInt(descriptors[currentDescriptor].fileLength);
                    for (int k = 0; k < FileSystemConfig.MAXIMUM_NUMBER_OF_BLOCKS_PER_FILE; k++) {
                        diskBlock.putInt(descriptors[currentDescriptor].fileContentsBlocksIndexes[k]);
                    }
                }
            }
            ioSystem.writeBlock(i, diskBlock.array());
        }
    }
}
