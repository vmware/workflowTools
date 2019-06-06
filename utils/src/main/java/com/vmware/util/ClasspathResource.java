package com.vmware.util;

import java.io.InputStreamReader;

public class ClasspathResource {

    private final String fileName;
    private final Class classToUseAsLoader;

    public ClasspathResource(String fileName, Class classToUseAsLoader) {
        this.fileName = fileName;
        this.classToUseAsLoader = classToUseAsLoader;
    }

    public String getText() {
        return IOUtils.read(classToUseAsLoader.getResourceAsStream(fileName));
    }

    public byte[] getBytes() {
        return getText().getBytes();
    }

    public InputStreamReader getReader() {
        return new InputStreamReader(classToUseAsLoader.getResourceAsStream(fileName));
    }
}
