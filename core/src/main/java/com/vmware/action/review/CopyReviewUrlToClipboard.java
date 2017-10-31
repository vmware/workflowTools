package com.vmware.action.review;

import com.vmware.action.base.BaseCommitUsingReviewBoardAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.IOUtils;
import com.vmware.util.UrlUtils;
import com.vmware.util.exception.RuntimeIOException;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.util.Locale;

@ActionDescription("Copies the review board url to the clipboard. Handy for pasting it into a browser.")
public class CopyReviewUrlToClipboard extends BaseCommitUsingReviewBoardAction {
    public CopyReviewUrlToClipboard(WorkflowConfig config) {
        super(config);
    }


    @Override
    public void process() {
        String reviewUrl = String.format("%sr/%s/", UrlUtils.addTrailingSlash(reviewBoardConfig.reviewboardUrl), draft.id);

        String osName = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
        boolean isOsx = osName.contains("mac") || osName.contains("darwin");

        if (isOsx) {
            copyUsingPbcopyCommand(reviewUrl);
        } else {
            StringSelection stringSelection = new StringSelection(reviewUrl);
            Clipboard clpbrd = Toolkit.getDefaultToolkit().getSystemClipboard();
            clpbrd.setContents(stringSelection, null);
        }
        log.info("Copied review url to clipboard");
    }

    private void copyUsingPbcopyCommand(String reviewUrl) {
        log.debug("Using pbcopy command to copy review url to clipboard as it doesn't cause terminal in full screen mode to jump back to the desktop view");
        ProcessBuilder builder = new ProcessBuilder("pbcopy").redirectErrorStream(true);
        Process statusProcess;
        try {
            statusProcess = builder.start();
        } catch (IOException e) {
            throw new RuntimeIOException(e);
        }
        IOUtils.write(statusProcess.getOutputStream(), reviewUrl);
        String output = IOUtils.read(statusProcess.getInputStream());
        log.debug(output);
    }
}
