package com.fs.filesystem;

import com.fs.iosystem.IOSystem;
import com.fs.ldisk.LDisk;
import com.fs.utils.FileSystemConfig;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.BitSet;

public class FileSystem {

    public IOSystem ioSystem;
    public BitSet bitmap;
    public FileDescriptor[] descriptors;
    public Directory directory;
    public OpenFileTable openFileTable;
    /**
     * This constructor initialise bitmap and sets for first 8 bits - true flag.
     * This means that first 8 blocks on disk are reserved for bitmap (1 block, 8 bytes in it),
     * descriptors (6 blocks, 16 bytes per descriptor (4 bytes for file length and 12 for file
     * content blocks indexes), so in one block we have 4 descriptors) and 1 block for directory
     * (this block contains entries from directory, each of them are 8 bytes (4 bytes for file name
     * and 4 for descriptor index)). We also initialize first descriptor which contains indexes
     * to one data blocks of directory.
     *
     * @param ioSystem
     *
     */
    public FileSystem(IOSystem ioSystem) {
        this.ioSystem = ioSystem;

        // bitmap setting true: 1 for bitmap, 6 for descriptors, 3 for directory
        bitmap = new BitSet(FileSystemConfig.BLOCKS_AMOUNT);
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

        saveDescriptorsToDisk();
        saveDirectoryToDisk();
    }
    /**
       initializing file system from disk
     */
    public FileSystem(LDisk ldisk) {
        ByteBuffer diskBlockBuffer = ByteBuffer.allocate(FileSystemConfig.BLOCK_LENGTH);

        byte[] blockBytes = diskBlockBuffer.array();
        byte[] bitMapBytes = Arrays.copyOfRange(blockBytes, 0, FileSystemConfig.BITMAP_LENGTH_ON_DISK);
        //TODO: use implemented method
        this.ioSystem = new IOSystem(ldisk);
        ioSystem.readBlock(0, diskBlockBuffer);
        descriptors = new FileDescriptor[FileSystemConfig.NUMBER_OF_DESCRIPTORS];
        blockBytes = diskBlockBuffer.array();
        bitMapBytes = Arrays.copyOfRange(blockBytes, 0, FileSystemConfig.BITMAP_LENGTH_ON_DISK);
        bitmap = BitSet.valueOf(bitMapBytes);
        openFileTable = new OpenFileTable();
        openFileTable.entries[0] = new OpenFileTableEntry();
        // OFT first entry for directory
        openFileTable.entries[0].fileDescriptorIndex = 0;
        readDescriptorsFromDisk();
        readDirectoryFromDisk();
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
        ByteBuffer bytes = ByteBuffer.allocate(FileSystemConfig.BLOCK_LENGTH);
        ioSystem.readBlock(descriptors[fileDescriptorIndex].fileContentsBlocksIndexes[0], bytes);
        openFileTableEntry.fileBlockInBuffer = 0;
        openFileTableEntry.readWriteBuffer = bytes.array();

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
        if(OFTEntryIndex < 0 || OFTEntryIndex >= OpenFileTable.OFT_NUMBER_OF_ENTRIES) {
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

            if(currentBlockNumberOnDisk != -1) {
                ioSystem.writeBlock(currentBlockNumberOnDisk, entry.readWriteBuffer);
            }
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
        readDescriptorsFromDisk();
        readDirectoryFromDisk();
        readBitMapFromDisk();
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
        if (directory.listOfEntries.size() % FileSystemConfig.MAXIMUM_DIRECTORY_ENTRIES_PER_BLOCK == 0 && directory.listOfEntries.size() > 0) {
            int freeBlockIndex = searchFreeDataBlock(bitmap);
            bitmap.set(freeBlockIndex,true);
            saveBitMapToDisk(bitmap);
            for (int i = 0; i < descriptors[0].fileContentsBlocksIndexes.length; i++) {
                if (descriptors[0].fileContentsBlocksIndexes[i] == -1) {
                    descriptors[0].fileContentsBlocksIndexes[i] = freeBlockIndex;
                    break;
                }
            }
        }
        directory.addEntryToDirectory(fileName, freeDescriptorIndex);

        int freeBlockIndex = searchFreeDataBlock(bitmap);


        bitmap.set(freeBlockIndex,true);

        descriptors[freeDescriptorIndex] = new FileDescriptor(0, new int[]{freeBlockIndex,-1,-1});
        System.out.println("The file " + fileName+ " has been created successfully");
        saveBitMapToDisk(bitmap);
        saveDirectoryToDisk();
        saveDescriptorsToDisk();
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
        readBitMapFromDisk();
        readDirectoryFromDisk();
        readDescriptorsFromDisk();
        if (fileName.length() == 0 || fileName.length() > FileSystemConfig.MAXIMUM_FILE_NAME_LENGTH) {
            System.out.println("ERROR! File name is larger than maximum length or it is equal to zero");
            return FileSystemConfig.ERROR;
        }
        int descriptorIndex = findDescriptorIndexByFileName(fileName);
        if (descriptorIndex == -1) {
            System.out.println("ERROR! There is no file with file name: " + fileName);
            return FileSystemConfig.ERROR;
        }

        for(int i=0;i<openFileTable.entries.length;i++) {
            if(openFileTable.entries[i] != null && openFileTable.entries[i].fileDescriptorIndex == descriptorIndex) {
                System.out.println("ERROR! You must close file before destroying it");
                return FileSystemConfig.ERROR;
            }
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
        if (directory.listOfEntries.size() % FileSystemConfig.MAXIMUM_DIRECTORY_ENTRIES_PER_BLOCK == 0 && directory.listOfEntries.size() > 0) {

            for (int i = descriptors[0].fileContentsBlocksIndexes.length-1; i >= 0; i--) {
                if (descriptors[0].fileContentsBlocksIndexes[i] != -1) {
                    bitmap.set(descriptors[0].fileContentsBlocksIndexes[i], false);
                    descriptors[0].fileContentsBlocksIndexes[i] = -1;
                    break;
                }
            }
        }
        saveDirectoryToDisk();
        saveDescriptorsToDisk();
        saveBitMapToDisk(bitmap);
        return FileSystemConfig.SUCCESS;

    }

    /**
     * Sequentially reads a number of bytes from the specified file into main memory.
     * Reading begins with the current position in the file.
     *
     * - Compute the position within the read/write buffer that corresponds to the current
     * position within the file
     *
     * - Start copying bytes from the buffer into the specified main memory location untill:
     *   - the desired count or the end of the file is reached
     *   - the end of the buffer is reached
     *
     * @param OFTEntryIndex index of file in openFileTable.
     * @param memArea       starting main memory address.
     * @param count         number of bytes to be read.
     * @return int    number of bytes read.
     */
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

        // find current position inside readWriteBuffer
        int currentBufferPosition = entry.currentPositionInFile % FileSystemConfig.BLOCK_LENGTH;
        int currentMemoryPosition = 0;

        int counter = 0;

        // read count bytes starting at readWriteBuffer[currentBufferPosition] to memArea
        for (int i = 0; i < count && i < memArea.array().length; i++) {
            // if end of file, then return number of bytes read
            if (entry.currentPositionInFile == fileDescriptor.fileLength) {
                System.out.println("The end of the file is reached");
                return counter;
            } else {
                // if end of block, then write buffer to the disk, then read next block to RWBuffer
                if (currentBufferPosition == FileSystemConfig.BLOCK_LENGTH) {

                    writeBuffer(entry, fileDescriptor);

                    currentBufferPosition = 0;
                }

                // read 1 byte to memory
                memArea.put(currentMemoryPosition, entry.readWriteBuffer[currentBufferPosition]);
                // update positions, counter
                counter++;
                currentBufferPosition++;
                currentMemoryPosition++;
                // entry.currentPositionInFile - points to first byte after last accessed
                entry.currentPositionInFile++;
            }
        }

        return counter;
    }

    /**
     * Sequentially writes a number of bytes from main memory into the specified file.
     * Writing begins with the current position in the file.
     *
     * The data is transferred from main memory into
     * the buffer until the desired byte count is satisfied. If the end of buffer is reached:
     *  - the buffer is written to disk
     *  - the file descriptor and the bitmap are then updated to reflect the new block
     *
     * @param OFTEntryIndex index of file in openFileTable.
     * @param memArea       starting main memory address.
     * @param count         number of bytes to be written.
     * @return int    number of bytes written to file.
     */
    public int write(int OFTEntryIndex, byte[] memArea, int count) {
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

        if(fileDescriptor.fileLength == -1) {
            return -1;
        }

        //if currentPosition == end of file
        if (entry.currentPositionInFile == 3 * FileSystemConfig.BLOCK_LENGTH) {
            return 0;
        }

        if (writeBuffer(entry, fileDescriptor) == -1) {
            return -1;
        }

        // find current position inside readWriteBffer
        int currentBufferPosition = entry.currentPositionInFile % FileSystemConfig.BLOCK_LENGTH;
        int currentMemoryPosition = 0;

        int counter = 0;

        if (fileDescriptor.fileLength == 0) {
            int newDiskBlock = -1;
            for (int i = 8; i < 64; i++) {
                if (!bitmap.get(i)) {
                    newDiskBlock = i;
                    break;
                }
            }
            entry.fileBlockInBuffer = 0;
            fileDescriptor.fileContentsBlocksIndexes[entry.fileBlockInBuffer] = newDiskBlock;
            fileDescriptor.fileLength += FileSystemConfig.BLOCK_LENGTH;
            bitmap.set(newDiskBlock, true);
        }

        // write count bytes from memArea to ReadWriteBuffer starting at currentBufferPosition
        for (int i = 0; i < count && i < memArea.length; i++) {

            // if end of buffer, check if we can load next block (allocate or read, but previously write that buffer to the disk)
            if (currentBufferPosition == FileSystemConfig.BLOCK_LENGTH) {
                if (entry.fileBlockInBuffer < 2) {
                    currentBufferPosition = 0;
                    writeBuffer(entry, fileDescriptor);
                } else {
                    break;
                }
            }

            // write 1 byte to file
            entry.readWriteBuffer[currentBufferPosition] = memArea[currentMemoryPosition];
            entry.bufferModified = true;

            // update positions, writtenCount
            counter++;
            currentBufferPosition++;
            currentMemoryPosition++;
            entry.currentPositionInFile++;
        }
        saveBitMapToDisk(bitmap);
        saveDirectoryToDisk();
        saveDescriptorsToDisk();
        return counter;
    }

    /**
     * Move the current position of the file to new position.
     *
     * Set current position to new position
     *
     * If the new position is not within the current data block:
     * – write the buffer into the appropriate block on disk
     * – read the new data block from disk into the buffer
     *
     * @param OFTEntryIndex index of file in openFileSystem.
     * @param pos           new position, specifies the number of bytes from the beginning of the file
     * @return int    status.
     */
    public int seek(int OFTEntryIndex, int pos) {
        if(OFTEntryIndex <= 0 || OFTEntryIndex >= OpenFileTable.OFT_NUMBER_OF_ENTRIES) {
            System.out.println("ERROR! WRONG INDEX");
            return -1;
        }

        FileDescriptor fileDescriptor = descriptors[openFileTable.entries[OFTEntryIndex].fileDescriptorIndex];

        if (pos > fileDescriptor.fileLength || pos < 0) {
            return -1;
        }

        OpenFileTableEntry entry = openFileTable.entries[OFTEntryIndex];

        entry.currentPositionInFile = pos;
        writeBuffer(entry, fileDescriptor);

        return 1;
    }

    private int writeBuffer(OpenFileTableEntry entry, FileDescriptor fileDescriptor) {
        if (entry.fileBlockInBuffer == -1) {
            return -1;
        }
        // if buffer holds different block
        if (entry.fileBlockInBuffer != (entry.getCurrentDataBlockPosition())) {
            if (entry.bufferModified) {
                int diskBlock = fileDescriptor.fileContentsBlocksIndexes[entry.fileBlockInBuffer];
                try {
                    ioSystem.writeBlock(diskBlock, entry.readWriteBuffer);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            try {
                int newFileBlock = entry.getCurrentDataBlockPosition();

                if (fileDescriptor.fileContentsBlocksIndexes[newFileBlock] == -1) {
                    int newDiskBlock = -1;
                    for (int i = 8; i < 64; i++) {
                        if (!bitmap.get(i)) {
                            newDiskBlock = i;
                            break;
                        }
                    }
                    if (newDiskBlock == -1) {
                        return -1;
                    }
                    fileDescriptor.fileContentsBlocksIndexes[newFileBlock] = newDiskBlock;
                    fileDescriptor.fileLength += FileSystemConfig.BLOCK_LENGTH;
                    bitmap.set(newDiskBlock, true);
                }

                ByteBuffer temp = ByteBuffer.allocate(FileSystemConfig.BLOCK_LENGTH);
                ioSystem.readBlock(fileDescriptor.fileContentsBlocksIndexes[newFileBlock], temp);
                entry.readWriteBuffer = temp.array();
                entry.bufferModified = false;
                entry.fileBlockInBuffer = newFileBlock;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return 1;
    }

    public void listDirectory() {
        readDirectoryFromDisk();
        readDescriptorsFromDisk();
        for (DirectoryEntry entry: directory.listOfEntries) {
            System.out.println("Name of file: " + entry.fileName + "; Size of file: " + descriptors[entry.fileDescriptorIndex].fileLength);
        }
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
        ByteBuffer diskBlockBuffer = ByteBuffer.allocate(FileSystemConfig.BLOCK_LENGTH);
        ioSystem.readBlock(0, diskBlockBuffer);

        //override bitMap in buffer
        for(int i=0;i<bitSetBytes.length;i++) {
            diskBlockBuffer.put(i, bitSetBytes[i]);
        }

        //flush buffer to disk
        byte[] bufferBytes = diskBlockBuffer.array();
        ioSystem.writeBlock(0, bufferBytes);
    }

    /**
     * Returns BitMap stored on disk
     */
    public BitSet readBitMapFromDisk() {
        ByteBuffer diskBlockBuffer = ByteBuffer.allocate(FileSystemConfig.BLOCK_LENGTH);
        ioSystem.readBlock(0, diskBlockBuffer);

        byte[] blockBytes = diskBlockBuffer.array();
        byte[] bitMapBytes = Arrays.copyOfRange(blockBytes, 0, FileSystemConfig.BITMAP_LENGTH_ON_DISK);

        return BitSet.valueOf(bitMapBytes);
    }

    public void readDescriptorsFromDisk() {
        for (int i = 1; i <= FileSystemConfig.NUMBER_OF_DESCRIPTOR_BLOCKS; i++) {
            ByteBuffer diskBlockBuffer = ByteBuffer.allocate(FileSystemConfig.BLOCK_LENGTH);
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
    public int readDirectoryFromDisk() {
        directory = new Directory();
       // readDescriptorsFromDisk();
        FileDescriptor fileDescriptor = descriptors[openFileTable.entries[0].fileDescriptorIndex];
        int maximumDirectoryEntriesPerBlock = 8;
        int currentPosition = 0;
        boolean check = true;
        for (int i = 0; i < fileDescriptor.fileContentsBlocksIndexes.length && check; i++) {
            if (fileDescriptor.fileContentsBlocksIndexes[i] != -1 ) {
                ByteBuffer buffer = ByteBuffer.allocate(FileSystemConfig.BLOCK_LENGTH);
                ioSystem.readBlock(fileDescriptor.fileContentsBlocksIndexes[i], buffer);
                openFileTable.entries[0].readWriteBuffer = buffer.array();
                for (int j = 0; j < maximumDirectoryEntriesPerBlock; j++) {
                   // openFileTable.entries
                    if (openFileTable.entries[0].readWriteBuffer[currentPosition] == 0) {
                        check = false;
                        break;
                    }
                    String fileName = "";
                    for (int k = 0; k < 4; k++, currentPosition++) {
                        char charFromBuffer = (char) openFileTable.entries[0].readWriteBuffer[currentPosition];
                        if (charFromBuffer != '\0') {
                            fileName += (char) openFileTable.entries[0].readWriteBuffer[currentPosition];
                        }
                    }
                    byte[] integer = Arrays.copyOfRange(openFileTable.entries[0].readWriteBuffer, currentPosition, currentPosition + 4);
                    currentPosition += 4;
                    int fileDescriptorIndex = ByteBuffer.wrap(integer).getInt();
                    directory.addEntryToDirectory(fileName, fileDescriptorIndex);
                }
                if (currentPosition == FileSystemConfig.BLOCK_LENGTH) {
                    currentPosition = 0;
                }
            }
        }
        return FileSystemConfig.SUCCESS;
    }
    public int saveDirectoryToDisk() {
        FileDescriptor fileDescriptor = descriptors[openFileTable.entries[0].fileDescriptorIndex];
        int numberOfDirectoryBlocks = 0;
        for (int i = 0; i < 3; i++) {
            if (fileDescriptor.fileContentsBlocksIndexes[i] != -1) {
                numberOfDirectoryBlocks++;
            }
        }
        int maximumDirectoryEntriesPerBlock = 8;
        if (directory.listOfEntries.size() > maximumDirectoryEntriesPerBlock * numberOfDirectoryBlocks) {
            return FileSystemConfig.ERROR;
        }
        int currentBufferPosition = 0;

        boolean check = true;
        int indexForFileContentsBlocksIndexesArray = 0;
        for (int i = 0; i < directory.listOfEntries.size(); i++) {
            check = true;
            if (openFileTable.entries[0].readWriteBuffer == null) {
                openFileTable.entries[0].readWriteBuffer = new byte[FileSystemConfig.BLOCK_LENGTH];
            }
            for (int j = 0; j < 4; j++) {
                if (j < directory.listOfEntries.get(i).fileName.length()) {
                    openFileTable.entries[0].readWriteBuffer[currentBufferPosition] = (byte) directory.listOfEntries.get(i).fileName.charAt(j);
                }
                else {
                    openFileTable.entries[0].readWriteBuffer[currentBufferPosition] = (byte)'\0';
                }
                currentBufferPosition++;
            }
            byte[] integerToBytes = ByteBuffer.allocate(4).putInt(directory.listOfEntries.get(i).fileDescriptorIndex).array();
            for (int k = 0; k < 4; k++) {
                openFileTable.entries[0].readWriteBuffer[currentBufferPosition] = integerToBytes[k];
                currentBufferPosition++;
            }
            if ((i+1) % maximumDirectoryEntriesPerBlock == 0) {

                while (fileDescriptor.fileContentsBlocksIndexes[indexForFileContentsBlocksIndexesArray] == -1) {
                    indexForFileContentsBlocksIndexesArray++;
                }
                ioSystem.writeBlock(fileDescriptor.fileContentsBlocksIndexes[indexForFileContentsBlocksIndexesArray], openFileTable.entries[0].readWriteBuffer);
                indexForFileContentsBlocksIndexesArray++;
                currentBufferPosition = 0;
                openFileTable.entries[0].readWriteBuffer = null;
                check = false;
            }
        }
        if (check) {
            ioSystem.writeBlock(fileDescriptor.fileContentsBlocksIndexes[indexForFileContentsBlocksIndexesArray], openFileTable.entries[0].readWriteBuffer);
        }
        return FileSystemConfig.SUCCESS;
    }

    public void saveDescriptorsToDisk() {
        for (int i = 1; i <= FileSystemConfig.NUMBER_OF_DESCRIPTOR_BLOCKS; i++) {
            ByteBuffer diskBlock = ByteBuffer.allocate(FileSystemConfig.BLOCK_LENGTH);
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


    public int closeAllFiles() {
        for(int i = 0; i < openFileTable.entries.length; i++) {
            if (openFileTable.entries[i] != null) {
                close(i);
            }
        }
        return 1;
    }

}
