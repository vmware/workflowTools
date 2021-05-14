package com.vmware;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

public class WorkflowAppLoader {

    private final String releaseDirectory;
    private final List<String> argValues;
    private final boolean debugLog, reset;
    private final File releaseJar;
    private final Map<String, String> manifestAttributes;
    private final File testReleaseJar;

    public static void main(String[] args) {
        WorkflowAppLoader loader = new WorkflowAppLoader(args);
        loader.downloadJarFileIfNeeded();
        loader.executeWorkflowJar();
    }

    public WorkflowAppLoader(String[] args) {
        this.argValues = new ArrayList<>(Arrays.asList(args));
        this.debugLog = Stream.of("-d", "--debug", "-t", "--trace").anyMatch(argValues::contains);
        this.reset = argValues.remove("--reset");
        this.manifestAttributes = getManifestAttributes();
        this.releaseDirectory = manifestAttributes.containsKey("releaseDirectory")
                ? manifestAttributes.get("releaseDirectory") : System.getProperty("java.io.tmpdir");
        this.releaseJar = new File(this.releaseDirectory + File.separator + manifestAttributes.get("releaseJarName"));
        Optional<String> testReleaseJarPath = getArgValue("--test-release-jar");
        this.testReleaseJar = testReleaseJarPath.map(File::new).orElse(null);
    }

    public void executeWorkflowJar() {
        debug("Launching workflow jar with args " + argValues);
        try {

            URLClassLoader urlClassLoader = URLClassLoader.newInstance(new URL[] { releaseJar.toURI().toURL()}, getClass().getClassLoader());
            Class<? extends AppLauncher> classToLoad = (Class<? extends AppLauncher>) urlClassLoader.loadClass(manifestAttributes.get("appMainClass"));
            AppLauncher launcher = classToLoad.newInstance();
            launcher.run(urlClassLoader, argValues);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void downloadJarFileIfNeeded() {
        debug("Expected release jar is " + releaseJar.getPath());
        if (releaseJar.exists() && !reset) {
            debug("Jar file " + releaseJar.getPath() + " already exists");
            return;
        }
        deleteOldReleasesIfNeeded();
        URL releaseURL = createReleaseUrl();
        info("Downloading workflow release jar " + releaseURL.toString() + " to " + releaseJar.getPath());

        try {
            ReadableByteChannel readableByteChannel = Channels.newChannel(releaseURL.openStream());
            FileOutputStream fileOutputStream = new FileOutputStream(releaseJar);
            fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private void deleteOldReleasesIfNeeded() {
        String deleteOldReleasesPattern = manifestAttributes.get("deleteOldReleaseJarPattern");
        if (deleteOldReleasesPattern == null) {
            debug("Delete old releases pattern not set, skipping deletion of old releases");
        }
        Pattern deleteJarPattern = Pattern.compile(deleteOldReleasesPattern);
        File[] matchingReleases = new File(releaseDirectory).listFiles(file -> deleteJarPattern.matcher(file.getName()).matches());
        Arrays.stream(matchingReleases).forEach(release -> {
            info("Deleting old release " + release.getPath());
            release.delete();
        });
    }

    private URL createReleaseUrl() {
        if (testReleaseJar != null) {
            info("Using test release file " + testReleaseJar.getPath());
            try {
                return testReleaseJar.toURI().toURL();
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }
        URL releaseURL;
        try {
            releaseURL = URI.create(manifestAttributes.get("releaseUrl")).toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        return releaseURL;
    }

    private void debug(String message) {
        if (debugLog) {
            System.out.println(message);
        }
    }

    private void info(String message) {
        System.out.println(message);
    }

    private Map<String, String> getManifestAttributes() {
        Optional<String> jarFilePath = getArgValue("--loader-jar-file");

        InputStream manifestInputStream;
        if (jarFilePath.isPresent()) {
            info("Loading manifest from jar file " + jarFilePath.get());
            try {
                JarFile jarFile = new JarFile(jarFilePath.get());
                ZipEntry manifestEntry = jarFile.getEntry(JarFile.MANIFEST_NAME);
                manifestInputStream = jarFile.getInputStream(manifestEntry);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            manifestInputStream = getClass().getClassLoader().getResourceAsStream(JarFile.MANIFEST_NAME);
        }

        try {
            Manifest manifest = new Manifest(manifestInputStream);
            Attributes mainAttributes = manifest.getMainAttributes();
            Set<Object> attributeKeys = mainAttributes.keySet();
            Map<String, String> attributeValues = attributeKeys.stream().collect(Collectors.toMap(String::valueOf, key -> mainAttributes.getValue((Attributes.Name) key)));
            debug("Manifest Attribute values " + attributeValues);
            return attributeValues;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Optional<String> getArgValue(String argName) {
        Optional<String> argValue = argValues.stream().filter(arg -> arg.startsWith(argName + "=")).map(arg -> arg.split("=")[1]).findFirst();
        argValues.removeIf(arg -> arg.startsWith(argName + "="));
        return argValue;
    }
}
