import com.vmware.scm.FileChange;
import com.vmware.scm.Perforce;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TestPerforce {

    private Perforce perforce = new Perforce("dbiggs-vcloud-sp-main");

    @Test
    public void canGetRootDirectory() {
        File clientDirectory = perforce.getWorkingDirectory();
        assertNotNull(clientDirectory);
        assertEquals("/Users/dbiggs/vcd", clientDirectory.getPath());
    }

    @Test
    public void changelistIsSubmitted() {
        assertEquals("submitted", perforce.getChangelistStatus("448453"));
    }

    @Test
    public void canGetFileChangesInChangelist() {
        List<String> openChangelists = perforce.getPendingChangelists();
        assertFalse("Need a pending changelist to run this test", openChangelists.isEmpty());
        List<FileChange> changes = perforce.getFileChangesForPendingChangelist(openChangelists.get(0));
        assertFalse("Should not be empty", changes.isEmpty());
    }
}
