package com.vmware.util;

import com.vmware.util.exception.RuntimeIOException;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

public class FileUtils {

    public static String stripExtension(File file) {
        String nameWithExtension = file.getName();
        if (!nameWithExtension.contains(".")) {
            return nameWithExtension;
        }

        return nameWithExtension.substring(0, nameWithExtension.lastIndexOf('.'));
    }

    public static List<File> scanDirectorRecursivelyForFiles(File directoryToScan, FileFilter fileFilter) {
        if (!directoryToScan.exists()) {
            throw new IllegalArgumentException(directoryToScan.getPath() + " does not exist!");
        }

        List<File> files = new ArrayList<File>();
        addDirectoryToList(files, directoryToScan, fileFilter);
        return files;
    }

    public static void copyFile(File src, File dst) {
        long p = 0, dp, size;

        if (!dst.exists()) try {
            dst.createNewFile();
        } catch (IOException e) {
            throw new RuntimeIOException(e);
        }

        try (FileChannel in = new FileInputStream(src).getChannel();
             FileChannel out = new FileOutputStream(dst).getChannel();)
        {
            size = in.size();

            while ((dp = out.transferFrom(in, p, size)) > 0)
            {
                p += dp;
            }
        } catch (IOException e) {
            throw new RuntimeIOException(e);
        }
    }

    private static void addDirectoryToList(List<File> files, File directory, FileFilter fileFilter) {
        File[] filesFound = directory.listFiles(fileFilter);
        for (File fileFound : filesFound) {
            if (fileFound.isDirectory()) {
                addDirectoryToList(files, fileFound, fileFilter);
            } else {
                files.add(fileFound);
            }
        }
    }
}
