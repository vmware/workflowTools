package com.vmware.util;

import java.io.IOException;
import java.io.InputStreamReader;

public class ClasspathResource {

    private String fileName;

    public ClasspathResource(String fileName) {
        this.fileName = fileName;
    }

    public String getText() {
        return IOUtils.read(ClassLoader.class.getResourceAsStream(fileName));
    }

    public byte[] getBytes() {
        return getText().getBytes();
    }

    public InputStreamReader getReader() {
        return new InputStreamReader(ClasspathResource.class.getResourceAsStream(fileName));
    }
}
