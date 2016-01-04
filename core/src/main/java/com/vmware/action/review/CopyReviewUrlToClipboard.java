package com.vmware.action.review;

import com.vmware.action.base.AbstractCommitWithReviewAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.http.UrlUtils;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;

@ActionDescription("Copies the review board url to the clipboard. Handy for pasting it into a browser.")
public class CopyReviewUrlToClipboard extends AbstractCommitWithReviewAction {
    public CopyReviewUrlToClipboard(WorkflowConfig config) throws IllegalAccessException, IOException, URISyntaxException {
        super(config);
    }

    @Override
    public void process() throws IOException, IllegalAccessException, URISyntaxException, ParseException {
        String reviewUrl = String.format("%sr/%s/", UrlUtils.addTrailingSlash(config.reviewboardUrl), draft.id);
        StringSelection stringSelection = new StringSelection (reviewUrl);
        Clipboard clpbrd = Toolkit.getDefaultToolkit().getSystemClipboard ();
        clpbrd.setContents (stringSelection, null);
        log.info("Copied review url to clipboard");
    }
}
