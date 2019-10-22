package com.vmware.action.review;

import com.vmware.action.base.BaseCommitUsingReviewBoardAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.CommandLineUtils;
import com.vmware.util.UrlUtils;
import com.vmware.util.logging.LogLevel;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
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

        log.info("Copying review url {} to clipboard", reviewUrl);
        if (isOsx && CommandLineUtils.isCommandAvailable("pbcopy")) {
            log.debug("Using osx pbcopy command to copy review url to clipboard as it doesn't cause terminal in full screen mode to jump back to the desktop view");
            CommandLineUtils.executeCommand(null, "pbcopy", reviewUrl, LogLevel.DEBUG);
        } else {
            log.debug("Using Java clipboard support to copy review url");
            StringSelection stringSelection = new StringSelection(reviewUrl);
            Clipboard clpbrd = Toolkit.getDefaultToolkit().getSystemClipboard();
            clpbrd.setContents(stringSelection, null);
        }
    }
}
