package com.vmware.action.filesystem;

import java.net.URI;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Base64;
import java.util.stream.Collectors;

import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.http.ssl.WorkflowCertificateManager;

import static com.vmware.util.StringUtils.BEGIN_CERT;
import static com.vmware.util.StringUtils.END_CERT;

@ActionDescription("Reads certs from the specified url.")
public class ReadCertsFromUrl extends BaseAction {
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    public ReadCertsFromUrl(WorkflowConfig config) {
        super(config);
        super.addFailWorkflowIfBlankProperties("sourceUrl");
    }

    @Override
    public void process() {
        log.info("Reading SSL certificates from url {}", fileSystemConfig.sourceUrl);
        X509Certificate[] certificates = new WorkflowCertificateManager().getCertificatesForUri(URI.create(fileSystemConfig.sourceUrl));
        fileSystemConfig.fileData = Arrays.stream(certificates).map(this::formatCrtFileContents).collect(Collectors.joining("\n"));
        log.debug("Cert data {}", fileSystemConfig.fileData);
    }

    private String formatCrtFileContents(final X509Certificate certificate) {
        final Base64.Encoder encoder = Base64.getMimeEncoder(64, LINE_SEPARATOR.getBytes());


        try {
            final byte[] rawCrtText = certificate.getEncoded();
            final String encodedCertText = new String(encoder.encode(rawCrtText));
            return BEGIN_CERT + LINE_SEPARATOR + encodedCertText + LINE_SEPARATOR + END_CERT;
        } catch (CertificateEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
