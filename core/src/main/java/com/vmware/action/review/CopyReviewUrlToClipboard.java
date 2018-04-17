package com.vmware.action.review;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.Locale;

import com.vmware.action.base.BaseCommitUsingReviewBoardAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.util.CommandLineUtils;
import com.vmware.util.UrlUtils;
import com.vmware.util.logging.LogLevel;

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

        if (isOsx && CommandLineUtils.isCommandAvailable("pbcopy")) {
            log.debug("Using pbcopy command to copy review url to clipboard as it doesn't cause terminal in full screen mode to jump back to the desktop view");
            CommandLineUtils.executeCommand(null, "pbcopy", reviewUrl, LogLevel.DEBUG);
        } else {
            StringSelection stringSelection = new StringSelection(reviewUrl);
            Clipboard clpbrd = Toolkit.getDefaultToolkit().getSystemClipboard();
            clpbrd.setContents(stringSelection, null);
        }
        log.info("Copied review url to clipboard");
    }
}
