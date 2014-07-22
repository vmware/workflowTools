package com.vmware.utils;

import java.io.IOException;
import java.io.InputStreamReader;

public class ClasspathResource {

    private String fileName;

    public ClasspathResource(String fileName) {
        this.fileName = fileName;
    }

    public String getText() throws IOException {
        return IOUtils.read(ClassLoader.class.getResourceAsStream(fileName));
    }

    public byte[] getBytes() throws IOException {
        return getText().getBytes();
    }

    public InputStreamReader getReader() {
        return new InputStreamReader(ClassLoader.class.getResourceAsStream(fileName));
    }
}
