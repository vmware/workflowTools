package com.vmware.util;

import com.vmware.util.collection.CircularFifoQueue;
import com.vmware.util.exception.RuntimeIOException;
import com.vmware.util.logging.DynamicLogger;
import com.vmware.util.logging.LogLevel;
import org.slf4j.LoggerFactory;

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
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Queue;

public class IOUtils {

    private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;
    private static DynamicLogger logger = new DynamicLogger(LoggerFactory.getLogger(IOUtils.class));

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
        return read(reader, false, null);
    }

    public static String read(String path) {
        if (path.startsWith("http")) {
            try {
                return IOUtils.read(new URL(path).openStream());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            return IOUtils.read(new File(path));
        }
    }

    public static String read(InputStream inputStream) {
        return read(inputStream, null);
    }

    public static String read(InputStream inputStream, LogLevel printLinesLevel) {
        try (InputStreamReader reader = new InputStreamReader(inputStream)) {
            return read(reader, true, printLinesLevel);
        } catch (IOException e) {
            throw new RuntimeIOException(e);
        }
    }

    public static String tail(String url, int numberOfLinesToTail) {
        try {
            logger.log(LogLevel.DEBUG, "Tailing {} lines using url {}", numberOfLinesToTail, url);
            URLConnection urlConnection = new URL(url).openConnection();
            Queue<String> lines = new CircularFifoQueue<>(numberOfLinesToTail);
            addLines(urlConnection.getInputStream(), lines);
            return StringUtils.join(lines, "\n");
        } catch (IOException e) {
            throw new RuntimeIOException(e);
        }
    }

    public static List<String> readLines(File file) {
        try {
            return readLines(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            throw new RuntimeIOException(e);
        }
    }

    public static List<String> readLines(InputStream inputStream) {
        List<String> lines = new ArrayList<>();
        addLines(inputStream, lines);
        return lines;
    }

    public static void addLines(InputStream inputStream, Collection<String> lines) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line = reader.readLine();
            while (line != null) {
                lines.add(line);
                line = reader.readLine();
            }
        } catch (IOException e) {
            throw new RuntimeIOException(e);
        }
    }

    public static String read(Reader input, boolean readUntilStreamClosed, LogLevel printLinesLevel) {
        StringWriter writer = new StringWriter();
        char[] buffer = new char[DEFAULT_BUFFER_SIZE];

        int lastReadCount;
        StringBuilder alreadyWrittenOutput = null;
        do {
            try {
                lastReadCount = input.read(buffer);
            } catch (IOException e) {
                throw new RuntimeIOException(e);
            }
            if (lastReadCount != -1) {
                writer.write(buffer, 0, lastReadCount);
                String outputToWrite = writer.toString();
                if (alreadyWrittenOutput == null) {
                    alreadyWrittenOutput = new StringBuilder(writer.toString());
                } else {
                    outputToWrite = outputToWrite.substring(alreadyWrittenOutput.length());
                    alreadyWrittenOutput.append(outputToWrite);
                }
                if (printLinesLevel != null) {
                    logger.log(printLinesLevel, outputToWrite.trim());
                }
            }
        } while (canRead(input, readUntilStreamClosed, lastReadCount));
        String output = writer.toString();
        if (output.endsWith("\n")) {
            output = output.substring(0, output.length() - 1);
        }
        return output;
    }

    private static boolean canRead(Reader reader, boolean readUntilStreamClosed, int lastReadCount) {
        if (readUntilStreamClosed) {
            return lastReadCount != -1;
        } else {
            try {
                return reader.ready();
            } catch (IOException e) {
                throw new RuntimeIOException(e);
            }
        }
    }
}
