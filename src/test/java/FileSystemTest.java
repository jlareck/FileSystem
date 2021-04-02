
import com.fs.filesystem.FileSystem;
import com.fs.iosystem.IOSystem;
import com.fs.ldisk.LDisk;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class FileSystemTest {
    @Test
    public void test() {
        FileSystem fileSystem = new FileSystem(new IOSystem(new LDisk()));
        assertEquals(fileSystem.create("FILE"), 1);
        assertEquals(fileSystem.destroy("FILE"), 1);
    }
}
