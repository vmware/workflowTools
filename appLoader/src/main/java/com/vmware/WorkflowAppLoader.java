package com.vmware;

import com.vmware.util.logging.WorkflowConsoleHandler;

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
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

public class WorkflowAppLoader {
    private final Logger logger = Logger.getLogger(this.getClass().getName());
    private final String releaseDirectory;
    private final List<String> argValues;
    private final boolean update;
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
        LogManager.getLogManager().reset();
        java.util.logging.Logger globalLogger = java.util.logging.Logger.getLogger("com.vmware");
        globalLogger.addHandler(new WorkflowConsoleHandler());

        boolean debugLog = Stream.of("-d", "--debug").anyMatch(argValues::contains);
        boolean traceLog = Stream.of("-t", "--trace").anyMatch(argValues::contains);
        globalLogger.setLevel(traceLog ? Level.FINEST : debugLog ? Level.FINE : Level.INFO);

        this.update = argValues.remove("--update");
        this.manifestAttributes = getManifestAttributes();

        this.releaseDirectory = determineReleaseDirectory();
        String releaseJarName = manifestAttributes.get("releaseJarName");
        this.releaseJar = new File(this.releaseDirectory + File.separator + releaseJarName);
        this.testReleaseJar = getArgValue("--test-release-jar").map(File::new).orElse(null);
    }

    public void executeWorkflowJar() {
        logger.fine("Launching workflow jar with args " + argValues);
        try {

            URLClassLoader urlClassLoader = URLClassLoader.newInstance(new URL[] { releaseJar.toURI().toURL()}, getClass().getClassLoader());
            String mainClassName = manifestAttributes.get("appMainClass");
            Class<? extends AppLauncher> classToLoad = (Class<? extends AppLauncher>) urlClassLoader.loadClass(mainClassName);
            AppLauncher launcher = classToLoad.newInstance();
            launcher.run(urlClassLoader, argValues);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void downloadJarFileIfNeeded() {
        logger.fine("Expected release jar is " + releaseJar.getAbsolutePath());
        if (releaseJar.exists() && !releaseJar.canWrite()) {
            logger.info("Auto updating workflow tools");
        } else if (releaseJar.exists() && !update) {
            logger.fine(() -> String.format("Jar file %s already exists", releaseJar.getPath()));
            return;
        }
        deleteOldReleasesIfNeeded();
        URL releaseURL = createReleaseUrl();
        logger.info("Downloading workflow release jar " + releaseURL.toString() + " to " + releaseJar.getPath());

        try (ReadableByteChannel readableByteChannel = Channels.newChannel(releaseURL.openStream())) {
            FileOutputStream fileOutputStream = new FileOutputStream(releaseJar);
            fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private void deleteOldReleasesIfNeeded() {
        String deleteOldReleasesPattern = manifestAttributes.get("deleteOldReleaseJarPattern");
        if (deleteOldReleasesPattern == null) {
            logger.fine("Delete old releases pattern not set, skipping deletion of old releases");
            return;
        }
        Pattern deleteJarPattern = Pattern.compile(deleteOldReleasesPattern);
        File[] matchingReleases = new File(releaseDirectory).listFiles(file -> deleteJarPattern.matcher(file.getName()).matches());
        if (matchingReleases == null) {
            return;
        }
        Arrays.stream(matchingReleases).forEach(release -> {
            logger.info("Deleting old release " + release.getPath());
            release.delete();
        });
    }

    private URL createReleaseUrl() {
        if (testReleaseJar != null) {
            logger.info("Using test release file " + testReleaseJar.getPath());
            try {
                return testReleaseJar.toURI().toURL();
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }
        URL releaseURL;
        try {
            String url = manifestAttributes.get("releaseUrl");
            if (url == null || url.isEmpty()) {
                throw new RuntimeException("No releaseUrl specified in manifest attributes");
            }
            logger.fine("Using workflow release url " + url);
            releaseURL = URI.create(url).toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        return releaseURL;
    }

    private Map<String, String> getManifestAttributes() {
        Optional<String> jarFilePath = getArgValue("--loader-jar-file");

        InputStream manifestInputStream;
        if (jarFilePath.isPresent()) {
            logger.info("Loading manifest from jar file " + jarFilePath.get());
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
            logger.fine("Manifest Attribute values " + attributeValues);
            return attributeValues;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String determineReleaseDirectory() {
        if (manifestAttributes.containsKey("releaseDirectory")) {
            return manifestAttributes.get("releaseDirectory");
        } else {
            String userHome = System.getProperty("user.home");
            File releaseDirectory = new File(userHome + File.separator + ".workflowReleaseDirectory");
            if (!releaseDirectory.exists()) {
                releaseDirectory.mkdir();
            }
            return releaseDirectory.getAbsolutePath();
        }
    }

    private Optional<String> getArgValue(String argName) {
        Optional<String> argValue = argValues.stream().filter(arg -> arg.startsWith(argName + "=")).map(arg -> arg.split("=")[1]).findFirst();
        argValues.removeIf(arg -> arg.startsWith(argName + "="));
        return argValue;
    }
}
