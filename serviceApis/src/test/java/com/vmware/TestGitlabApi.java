package com.vmware;

import com.vmware.gitlab.Gitlab;
import com.vmware.gitlab.domain.MergeRequest;
import com.vmware.gitlab.domain.MergeRequestApprovals;
import com.vmware.gitlab.domain.MergeRequestNote;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TestGitlabApi extends BaseTests {

    private Gitlab gitlab;
    private String username;

    @Before
    public void init() {
        String url = testProperties.getProperty("gitlab.url");
        username = testProperties.getProperty("gitlab.username");
        gitlab = new Gitlab(url, username);
    }

    @Test
    public void getMergeRequest() {
        MergeRequest mergeRequest = gitlab.getMergeRequest(42166, 10158);
        assertNotNull(mergeRequest.title);

        Set<MergeRequestNote> notes = gitlab.getOpenMergeRequestNotes(mergeRequest.projectId, mergeRequest.iid);
        assertEquals(2, notes.size());

        MergeRequestApprovals approvals = gitlab.getMergeRequestApprovals(mergeRequest.projectId, mergeRequest.iid);
        assertTrue(approvals.approvedBy.length > 0);
    }
}
