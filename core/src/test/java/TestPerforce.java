import com.vmware.Perforce;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
}
