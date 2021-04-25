package com.fs.shell;

/**
 * @author Fenz Taisiia
 */
public interface ShellView {

    void begin();
    void create(String fileName);
    void destroy(String fileName);
    void open(String fileName);
    void close(int index);
    void read(int index, int count);
    void write(int index, char c, int count);
    void seek(int index, int pos);
    void directory();
    void init(String diskCont); //parameters
    void save(String diskCont);
}
