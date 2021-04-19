package com.fs.shell;

import com.fs.filesystem.FileSystem;
import com.fs.iosystem.IOSystem;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Scanner;

public class Shell {

    private IOSystem ioSystem;
    private FileSystem fileSystem;

    public Shell(IOSystem ioSystem) {
        this.ioSystem = ioSystem;
        this.fileSystem = null;
    }

    public Shell(IOSystem ioSystem, FileSystem fileSystem) {
        this.ioSystem = ioSystem;
        this.fileSystem = fileSystem;
    }

    public void begin() {
        String[] input;
        String commandName;
        Scanner scanner = new Scanner(System.in);
        System.out.println("Begin...");

        while (true) {
            input = scanner.nextLine().split(" ");
            commandName = input[0];
            for (String word : input) {
                System.out.println(word);
            }

            switch (commandName) {
                case "cd": {
                    if (input.length != 2) {
                        System.out.println("Error");
                    } else {
                        create(input[1]);
                    }
                    break;
                }
                case "de": {
                    if (input.length != 2) {
                        System.out.println("Error");
                    } else {
                        destroy(input[1]);
                    }
                    break;
                }
                case "op": {
                    if (input.length != 2) {
                        System.out.println("Error");
                    } else {
                        open(input[1]);
                    }
                    break;
                }
                case "cl": {
                    if (input.length != 2) {
                        System.out.println("Error");
                    } else {
                        try {
                            close(Integer.parseInt(input[1]));
                        } catch (NumberFormatException e) {
                            System.out.println("Error");
                        }
                    }
                    break;
                }
                case "rd": {
                    if (input.length != 3) {
                        System.out.println("Error");
                    } else {
                        try {
                            read(Integer.parseInt(input[1]), Integer.parseInt(input[2]));
                        } catch (NumberFormatException e) {
                            System.out.println("Error");
                        }
                    }
                    break;
                }
                case "wr": {
                    if (input[2].length() != 1 || input.length != 4) {
                        System.out.println("Error");
                    } else {
                        try {
                            write(Integer.parseInt(input[1]), input[2].charAt(0), Integer.parseInt(input[3]));
                        } catch (NumberFormatException e) {
                            System.out.println("Error");
                        }
                    }
                    break;
                }
                case "sk": {
                    if (input.length != 3) {
                        System.out.println("Error");
                    } else {
                        try {
                            seek(Integer.parseInt(input[1]), Integer.parseInt(input[2]));
                        } catch (NumberFormatException e) {
                            System.out.println("Error");
                        }
                    }
                    break;
                }
                case "dr": {
                    if (input.length != 1) {
                        System.out.println("Error");
                    } else {
                        directory();
                    }
                    break;
                }
                case "in": {
                    if (input.length != 2) {
                        System.out.println("Error");
                    } else {
                        init(input[1]);
                    }
                    break;
                }
                case "sv": {
                    if (input.length != 2) {
                        System.out.println("Error");
                    } else {
                        save(input[1]);
                    }
                    break;
                }
                default: {

                }
            }
        }
    }

    private void create(String fileName) {
        if (fileSystem.create(fileName) == -1) {
            System.out.println("Error");
            return;
        }
        System.out.println("File " + fileName + " created.");
    }

    private void destroy(String fileName) {
        if (fileSystem.destroy(fileName) == -1) {
            System.out.println("Error");
            return;
        }
        System.out.println("File " + fileName + " destroyed.");
    }

    private void open(String fileName) {
        int index = fileSystem.open(fileName);
        if (index == -1) {
            System.out.println("Error");
            return;
        }
        System.out.println("File " + fileName + " opened, index = " + index + ".");
    }

    private void close(int index) {
        if (fileSystem.close(index) == -1) {
            System.out.println("Error");
            return;
        }
        System.out.println("File " + index + " closed.");
    }

    private void read(int index, int count) {
        if (count < 0) {
            System.out.println("Error");
            return;
        }
        ByteBuffer readBuffer = ByteBuffer.allocate(count);
        int numOfReadBytes = fileSystem.read(index, readBuffer, count);
        if (numOfReadBytes == -1) {
            System.out.println("Error");
            return;
        }
        char[] readBytes = new char[numOfReadBytes];
        for (int i = 0; i < numOfReadBytes; i++) {
            readBytes[i] = (char) readBuffer.get();
        }
        System.out.println(numOfReadBytes + " bytes read: " + Arrays.toString(readBytes) + ".");
    }

    private void write(int index, char c, int count) {
        if (count < 0) {
            System.out.println("Error");
            return;
        }
        byte[] memArea = new byte[count];
        for (int i = 0; i < memArea.length; i++) {
            memArea[i] = (byte) c;
        }
        int numOfWrittenBytes = fileSystem.write(index, memArea, count);

        if (numOfWrittenBytes == -1) {
            System.out.println("Error");
            return;
        }
        System.out.println(numOfWrittenBytes + " bytes written.");
    }

    private void seek(int index, int pos) {
        if (fileSystem.seek(index, pos) == -1) {
            System.out.println("Error");
            return;
        }
        System.out.println("Current position - " + pos + ".");
    }

    private void directory() {
        System.out.println("List of all files.");
        fileSystem.listDirectory();
    }

    private void init(String diskCont) {
        //TODO: first parameter - number of blocks (first 3 parameters) but for which purpose it is needed?

        File f = new File(diskCont);
        if (f.exists()) {
            //TODO: Open directory
            fileSystem = new FileSystem(ioSystem.readDiskFromFile(diskCont));
            System.out.println("Disk restored.");
        } else {
            //TODO: Create and open directory
            try {
                f.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            fileSystem = new FileSystem(ioSystem);
            System.out.println("Disk initialized.");
        }
    }

    private void save(String diskCont) {
        //TODO: add closing all files before saving
        fileSystem.closeAllFiles();
        //TODO: fix using of the parameter
        fileSystem.ioSystem.saveDiskToFile(diskCont);
        System.out.println("Disk saved! Congratulations!");
    }


}
