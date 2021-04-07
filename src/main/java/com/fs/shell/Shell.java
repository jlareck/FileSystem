package com.fs.shell;

import com.fs.filesystem.FileSystem;
import com.fs.iosystem.IOSystem;

import java.util.Scanner;

public class Shell {

    private FileSystem fileSystem;

    public Shell(FileSystem fileSystem) {
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
        //TODO: Create method
    }

    private void write(int index, char c, int count) {
       //TODO: Create method
    }

    private void seek(int index, int pos) {
        //TODO: Create method
    }

    private void directory() {
        //TODO: Create method
    }

    private void init(String diskCont) {
        //TODO: Create method
    }

    private void save(String diskCont) {
        //TODO: Create method
    }


}
