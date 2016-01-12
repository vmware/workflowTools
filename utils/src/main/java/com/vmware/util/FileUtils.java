package com.vmware.util;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
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

    public static void copyFile(File src, File dst) throws IOException
    {
        long p = 0, dp, size;
        FileChannel in = null, out = null;

        try
        {
            if (!dst.exists()) dst.createNewFile();

            in = new FileInputStream(src).getChannel();
            out = new FileOutputStream(dst).getChannel();
            size = in.size();

            while ((dp = out.transferFrom(in, p, size)) > 0)
            {
                p += dp;
            }
        }
        finally {
            try
            {
                if (out != null) out.close();
            }
            finally {
                if (in != null) in.close();
            }
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
