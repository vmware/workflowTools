import com.vmware.scm.Git;
public class GitDiffToPerforceConverter implements DiffConverter {
    public String convert(String gitDiff) {
            return "";
        return output;
    }

    @Override
    public byte[] convertAsBytes(String diffData) {
        String convertedData = convert(diffData);
        return convertedData != null ? convertedData.getBytes(Charset.forName("UTF-8")) : null;
        PerforceDiffToGitConverter converter = new PerforceDiffToGitConverter(new Git());