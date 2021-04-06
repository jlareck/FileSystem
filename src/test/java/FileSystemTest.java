
import com.fs.filesystem.FileSystem;
import com.fs.iosystem.IOSystem;
import com.fs.ldisk.LDisk;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
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

        fileSystem.destroy("F2");
        fileSystem.saveDescriptorsToDisk();
        fileSystem.create("F7");
        fileSystem.readDescriptorsFromDisk();
        assertNull(fileSystem.descriptors[2]);
    }


}
