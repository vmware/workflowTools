package com.vmware;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class WorkflowAppLoader {
    private static final String RELEASE_DATE = "May_13_2021";
    private static final String RELEASE_URL = "https://github.com/vmware/workflowTools/releases/download/" + RELEASE_DATE + "/workflow.jar";
    private static final String RELEASE_NAME = "workflow-" + RELEASE_DATE + ".jar";

    private final String tempDirectory;
    private final List<String> argValues;
    private final boolean debugLog;
    private final File expectedReleaseJar;

    public static void main(String[] args) {
        WorkflowAppLoader loader = new WorkflowAppLoader(args);
        loader.downloadJarFileIfNeeded();
        loader.executeWorkflowJar();
    }

    public WorkflowAppLoader(String[] args) {
        this.tempDirectory = getTempDirectory();
        this.argValues = Arrays.asList(args);
        this.debugLog = Stream.of("-d", "--debug", "-t", "--trace").anyMatch(argValues::contains);
        this.expectedReleaseJar = new File(tempDirectory + File.separator + RELEASE_NAME);
    }

    public void executeWorkflowJar() {
        if (debugLog) {
            System.out.println("Launching workflow jar with args " + argValues);
        }
        try {
            URLClassLoader urlClassLoader = new URLClassLoader(
                    new URL[] {expectedReleaseJar.toURI().toURL()},
                    this.getClass().getClassLoader()
            );
            Class classToLoad = Class.forName("com.vmware.WorkflowRunner", true, urlClassLoader);
            Method method = classToLoad.getDeclaredMethod("runWorkflow", ClassLoader.class, List.class);
            Object instance = classToLoad.newInstance();
            method.invoke(instance, urlClassLoader, argValues);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void downloadJarFileIfNeeded() {
        if (debugLog) {
            System.out.println("Expected release jar is " + expectedReleaseJar.getPath());
        }
        if (expectedReleaseJar.exists()) {
            if (debugLog) {
                System.out.println("Jar file " + expectedReleaseJar.getPath() + " already exists");
            }
            return;
        }
        URL releaseURL = createReleaseUrl();
        System.out.println("Downloading workflow release jar " + releaseURL.toString() + " to temp directory " + tempDirectory);

        try {
            ReadableByteChannel readableByteChannel = Channels.newChannel(releaseURL.openStream());
            FileOutputStream fileOutputStream = new FileOutputStream(expectedReleaseJar);
            fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private URL createReleaseUrl() {
        URL releaseURL;
        try {
            releaseURL = URI.create(RELEASE_URL).toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        return releaseURL;
    }

    private String getTempDirectory() {
        try {
            File tempFile = File.createTempFile("sample", "txt");
            String directory = tempFile.getParent();
            tempFile.delete();
            return directory;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
