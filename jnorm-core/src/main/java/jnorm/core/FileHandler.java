package jnorm.core;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class FileHandler {

    public static void generateFileFromString(String content, String filePath) {
        try {
            Files.write(Paths.get(filePath), content.getBytes(StandardCharsets.UTF_8));
        } catch (IOException ex) {
            System.err.print("Error while writing file: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    public static void makeDirs(String path) {
        File f = new File(path);
        f.mkdirs();
    }

    public static void deleteDirectoryContent(String filePath) {
        deleteDirectoryContent(new File(filePath));
    }

    public static void deleteDirectoryContent(File path) {
        File[] files = path.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    deleteDirectoryContent(f);
                } else {
                    f.delete();
                }
            }
        }
    }

}
