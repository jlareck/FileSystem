
import com.fs.filesystem.Directory;
import com.fs.filesystem.FileSystem;
import com.fs.iosystem.IOSystem;
import com.fs.ldisk.LDisk;
import com.fs.utils.FileSystemConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.BitSet;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class FileSystemTest {
    private FileSystem fileSystem;

    @BeforeEach
    void init() {
        fileSystem = new FileSystem(new IOSystem(new LDisk()));
    }

    @Test
    public void create() {
        assertEquals(1,fileSystem.create("FILE"));
        assertEquals(9, fileSystem.searchFreeDataBlock(fileSystem.bitmap));
    }

    @Test
    public void destroy() {
        assertEquals(1,fileSystem.create("FILE"));
        assertEquals(9, fileSystem.searchFreeDataBlock(fileSystem.bitmap));
        assertEquals(1,fileSystem.destroy("FILE"));
        assertEquals(8, fileSystem.searchFreeDataBlock(fileSystem.bitmap));
    }



    @Test
    public void open() {
        fileSystem.create("FILE");
        int oftIndex = fileSystem.open("FILE");

        //oftIndex points to first empty OFT entry
        assertEquals(1, oftIndex);
        assertNotNull(fileSystem.openFileTable.entries[oftIndex]);
    }

    @Test
    public void open_wrongFileName() {
        fileSystem.create("FILE");
        int response = fileSystem.open("FILE1");
        assertEquals(-1, response);
    }

    @Test
    public void open_alreadyOpened() {
        fileSystem.create("FILE");
        fileSystem.open("FILE");
        int response = fileSystem.open("FILE");
        assertEquals(-1, response);
    }

    @Test
    public void open_noMoreFreeOftEntries() {
        fileSystem.create("F");
        int response = fileSystem.open("F");

        fileSystem.create("F1");
        int response1 = fileSystem.open("F1");

        fileSystem.create("F3");
        int response2 = fileSystem.open("F3");

        fileSystem.create("F4");
        int response3 = fileSystem.open("F4");

        assertEquals(1, response);
        assertEquals(2, response1);
        assertEquals(3, response2);
        assertEquals(-1, response3);
    }

    @Test
    public void close() {
        fileSystem.create("FILE");
        int oftIndex = fileSystem.open("FILE");

        int response = fileSystem.close(oftIndex);

        assertEquals(1, response);
        //entry is cleaned
        assertNull(fileSystem.openFileTable.entries[oftIndex]);
    }

    @Test
    public void close_wrongIndex() {
        fileSystem.create("FILE");
        int oftIndex = fileSystem.open("FILE");

        int response = fileSystem.close(oftIndex + 1);

        assertEquals(-1, response);
    }

    @Test
    public void saveAndReadDescriptors() {
        fileSystem.create("F1");
        fileSystem.create("F2");
        fileSystem.create("F3");
        fileSystem.create("F4");
        fileSystem.saveDescriptorsToDisk();
        fileSystem.descriptors[1] = null;
        assertNull(fileSystem.descriptors[1]);
        fileSystem.readDescriptorsFromDisk();
        assertNotNull(fileSystem.descriptors[1]);
    }

    @Test
    void saveBitMapToDisk() {
        ByteBuffer diskBlockBuffer = ByteBuffer.allocate(FileSystemConfig.BLOCK_LENGTH);
        fileSystem.ioSystem.readBlock(0, diskBlockBuffer);

        byte[] blockBytes = diskBlockBuffer.array();
        byte[] bitMapBytes = Arrays.copyOfRange(blockBytes, 0, FileSystemConfig.BITMAP_LENGTH_ON_DISK);

        BitSet bitMapOnDisk = BitSet.valueOf(bitMapBytes);

        //previously bits were false
        assertFalse(bitMapOnDisk.get(9));
        assertFalse(bitMapOnDisk.get(10));

        //set them to true
        BitSet bitMap = fileSystem.bitmap;
        bitMap.set(9, true);
        bitMap.set(10, true);

        fileSystem.saveBitMapToDisk(bitMap);

        fileSystem.ioSystem.readBlock(0, diskBlockBuffer);
        blockBytes = diskBlockBuffer.array();
        bitMapBytes = Arrays.copyOfRange(blockBytes, 0, FileSystemConfig.BITMAP_LENGTH_ON_DISK);
        bitMapOnDisk = BitSet.valueOf(bitMapBytes);
        //and now these bits are updated
        assertTrue(bitMapOnDisk.get(9));
        assertTrue(bitMapOnDisk.get(10));
    }
    @Test
    void saveDirectoryToDisk() {
        int maxNumberOfFiles = 23;
        for (int i = 0; i < maxNumberOfFiles; i++) {
            fileSystem.create("F" + i);
        }

        fileSystem.directory = new Directory();
        assertEquals(0, fileSystem.directory.listOfEntries.size());
        fileSystem.readDirectoryFromDisk();
        for (int i = 0; i < maxNumberOfFiles; i++) {
            assertEquals("F"+i, fileSystem.directory.listOfEntries.get(i).fileName);
        }
    }


    @Test
    void saveDiskToFile() {
        fileSystem.create("file");
        fileSystem.ioSystem.saveDiskToFile();
        LDisk disk = fileSystem.ioSystem.readDiskFromFile();
        FileSystem newFileSystem = new FileSystem(disk);
        assertEquals(fileSystem.searchFreeDataBlock(fileSystem.bitmap), newFileSystem.searchFreeDataBlock(newFileSystem.bitmap));
        assertEquals(fileSystem.directory.listOfEntries.get(0).fileName, newFileSystem.directory.listOfEntries.get(0).fileName);
    }
}
