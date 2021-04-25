package com.fs.filesystem;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Medynskyi Mykola
 */
public class Directory {
    public List<DirectoryEntry> listOfEntries;
    public Directory() {
        listOfEntries = new ArrayList<>();
    }
    public void addEntryToDirectory(String fileName, int fileDescriptorIndex) {
        DirectoryEntry entry = new DirectoryEntry(fileName,fileDescriptorIndex);
        listOfEntries.add(entry);
    }
}
