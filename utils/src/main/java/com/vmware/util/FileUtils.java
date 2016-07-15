package com.vmware.util;

import com.vmware.util.exception.RuntimeIOException;
import org.slf4j.LoggerFactory;

import javax.xml.bind.DatatypeConverter;
import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class FileUtils {

    public static File createTempFile(String prefix, String suffix) {
        try {
            return File.createTempFile(prefix, suffix);
        } catch (IOException e) {
            throw new RuntimeIOException(e);
        }
    }

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

    public static String readFileAsString(File fileToRead) {
        return new String(readFile(fileToRead), Charset.forName("utf8"));
    }

    public static byte[] readFile(File fileToRead) {
        try {
            return Files.readAllBytes(fileToRead.toPath());
        } catch (IOException e) {
            throw new RuntimeIOException(e);
        }
    }

    public static void saveToFile(File dst, String content) {
        try {
            Files.copy(new ByteArrayInputStream(content.getBytes()), dst.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeIOException(e);
        }
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
