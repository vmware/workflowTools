package com.vmware.utils;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class IOUtils {

    public static void write(File file, String data) {
        try {
            write(new FileOutputStream(file), data);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static void write(OutputStream outputStream, String data) {
        try (BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream)) {
            bufferedOutputStream.write(data.getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void write(File outputFile, List<String> lines) {
        try (FileWriter writer = new FileWriter(outputFile)) {
            for (String line : lines) {
                writer.write(line + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String read(File file) {
        try {
            return read(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static String read(InputStream inputStream) {
        String text = "";

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line = reader.readLine();
            while (line != null) {
                if (!text.isEmpty()) {
                    text += "\n";
                }
                text += line;
                line = reader.readLine();
            }
            return text;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public static List<String> readLines(File file) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)))){
            List<String> lines = new ArrayList<String>();
            String line = reader.readLine();
            while (line != null) {
                lines.add(line);
                line = reader.readLine();
            }
            reader.close();
            return lines;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
