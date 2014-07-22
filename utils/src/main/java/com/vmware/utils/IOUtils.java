package com.vmware.utils;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class IOUtils {

    public static void write(File file, String data) throws IOException {
        write(new FileOutputStream(file), data);
    }

    public static void write(OutputStream outputStream, String data) throws IOException {
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);
        bufferedOutputStream.write(data.getBytes());
        bufferedOutputStream.close();
    }

    public static void write(File outputFile, List<String> lines) throws IOException {
        FileWriter writer = new FileWriter(outputFile);
        for (String line : lines) {
            writer.write(line + "\n");
        }
        writer.close();
    }

    public static String read(File file) throws IOException {
        return read(new FileInputStream(file));
    }

    public static String read(InputStream inputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String text = "";

        String line = reader.readLine();
        while (line != null) {
            if (!text.isEmpty()) {
                text += "\n";
            }
            text += line;
            line = reader.readLine();
        }
        reader.close();
        return text;
    }

    public static List<String> readLines(InputStream inputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        List<String> lines = new ArrayList<String>();

        String line = reader.readLine();
        while (line != null) {
            lines.add(line);
            line = reader.readLine();
        }
        reader.close();
        return lines;
    }
}
