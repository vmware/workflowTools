package com.vmware.util;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.util.Locale;

import com.vmware.util.exception.RuntimeIOException;
import com.vmware.util.logging.LogLevel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BrowserUtils {

    private static Logger log = LoggerFactory.getLogger(BrowserUtils.class);

    public static void openUrl(String url) {
        if (StringUtils.isBlank(url)) {
            log.error("Not opening url as it is blank");
            return;
        }

        log.info("Opening url {}", url);
        if (CommandLineUtils.isOsxCommandAvailable("open")) {
            log.debug("Opening url using osx open command");
            CommandLineUtils.executeCommand(null, "open " + url, null, LogLevel.DEBUG);
        } else if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            log.debug("Opening url using java desktop support");
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
