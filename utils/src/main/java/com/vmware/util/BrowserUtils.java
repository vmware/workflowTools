package com.vmware.util;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;

import com.vmware.util.exception.RuntimeIOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BrowserUtils {

    private static Logger log = LoggerFactory.getLogger(BrowserUtils.class);

    public static void openUrl(String url) {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            log.info("Opening url {}", url);
            try {
                Desktop.getDesktop().browse(URI.create(url));
            } catch (IOException e) {
                throw new RuntimeIOException(e);
            }
        } else {
            log.error("Cannot open url {} as BROWSER action for Java desktop is not supported", url);
        }
    }
}
