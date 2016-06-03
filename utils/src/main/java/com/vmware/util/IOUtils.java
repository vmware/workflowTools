package com.vmware.util;

import com.vmware.util.exception.RuntimeIOException;

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
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

public class IOUtils {

    private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;

    public static void write(File file, String data) {
        try {
            write(new FileOutputStream(file), data);
        } catch (FileNotFoundException e) {
            throw new RuntimeIOException(e);
        }
    }

    public static void write(OutputStream outputStream, String data) {
        try (OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream)) {
            outputStreamWriter.write(data);
        } catch (IOException e) {
            throw new RuntimeIOException(e);
        }
    }

    public static void writeWithoutClosing(OutputStream outputStream, String data) {
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
            outputStreamWriter.write(data);
            outputStreamWriter.flush();
        } catch (IOException e) {
            throw new RuntimeIOException(e);
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
            throw new RuntimeIOException(e);
        }
    }

    public static String readWithoutClosing(InputStream inputStream) {
        InputStreamReader reader = new InputStreamReader(inputStream);
        try {
            return read(reader, false);
        } catch (IOException e) {
            throw new RuntimeIOException(e);
        }
    }

    public static String read(InputStream inputStream) {
        try (InputStreamReader reader = new InputStreamReader(inputStream)) {
            return read(reader, true);
        } catch (IOException e) {
            throw new RuntimeIOException(e);
        }

    }

    public static List<String> readLines(File file) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
            List<String> lines = new ArrayList<>();
            String line = reader.readLine();
            while (line != null) {
                lines.add(line);
                line = reader.readLine();
            }
            reader.close();
            return lines;
        } catch (IOException e) {
            throw new RuntimeIOException(e);
        }
    }

    private static String read(Reader input, boolean readUntilStreamClosed) throws IOException {
        StringWriter writer = new StringWriter();
        char[] buffer = new char[DEFAULT_BUFFER_SIZE];

        int lastReadCount = 0;
        do {
            lastReadCount = input.read(buffer);
            if (lastReadCount != -1) {
                writer.write(buffer, 0, lastReadCount);
            }
        } while (canRead(input, readUntilStreamClosed, lastReadCount));
        String output = writer.toString();
        if (output.endsWith("\n")) {
            output = output.substring(0, output.length() - 1);
        }
        return output;
    }

    private static boolean canRead(Reader reader, boolean readUntilStreamClosed, int lastReadCount) throws IOException {
        if (readUntilStreamClosed) {
            return lastReadCount != -1;
        } else {
            return reader.ready();
        }
    }
}
