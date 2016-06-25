import com.vmware.Perforce;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TestPerforce {

    private Perforce perforce = new Perforce(new File("/Users/dbiggs/vcd"));

    @Test
    public void canGetRootDirectory() {
        File clientDirectory = perforce.getClientDirectory("dbiggs-vcloud-sp-main");
        assertNotNull(clientDirectory);
        assertEquals("/Users/dbiggs/vcd", clientDirectory.getPath());
    }

    @Test
    public void changelistIsSubmitted() {
        assertEquals("submitted", perforce.getChangelistStatus("448453"));
    }
}
