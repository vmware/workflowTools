package com.vmware.jenkins.domain;

import com.google.gson.annotations.Expose;
import com.vmware.config.jenkins.Job;
import com.vmware.util.StringUtils;

import java.util.NoSuchElementException;
import java.util.regex.PatternSyntaxException;

public class HomePage {

    public Job[] jobs;

    public View[] views;

    public Job getPrecommitUnitTestsJob() {
        return getJobByName("horizon-workspace-service-pre-commit-unit-tests-all");
    }

    public Job getPrecommitUnitTestsPostgresJob() {
        return getJobByName("horizon-workspace-service-pre-commit-unit-tests-postgres");
    }

    public Job getJobByName(String jobName) {
        for (Job job : jobs) {
            if (job.name.equals(jobName)) {
                return job;
            }
        }
        throw new NoSuchElementException(jobName);
    }

    public class View {
        public String name;
        public String url;

        @Expose(serialize = false, deserialize = false)
        public int failingTestsCount;
        public Exception failingTestsGenerationException;

        public String viewNameWithFailureCount() {
            if (failingTestsGenerationException != null) {
                return name + " (failed with error " + StringUtils.truncateStringIfNeeded(failingTestsGenerationException.getMessage(), 80) + ")";
            } else if (failingTestsCount == 0) {
                return name + " (no test failures)";
            } else {
                return name + " (" + failingTestsCount + " test failures)";
            }
        }

        public String listHtmlFragment() {
            return "<li><a href = \"" + htmlFileName() + "\" " + titleAttribute() + ">" + viewNameWithFailureCount() + "</a></li>";
        }

        public boolean matches(String value) {
            if (name.equalsIgnoreCase(value)) {
                return true;
            }
            String nameInUrl = nameInUrl();
            if (nameInUrl.equalsIgnoreCase(value)) {
                return true;
            }
            try {
                return name.matches(value) || nameInUrl.matches(value);
            } catch (PatternSyntaxException pse) {
                return false;
            }
        }

        public String htmlFileName() {
            return name.trim().replace(" ", "-") + ".html";
        }

        private String nameInUrl() {
            String nameInUrl = StringUtils.substringAfterLast(url, "/view/");
            if (nameInUrl.endsWith("/")) {
                nameInUrl = nameInUrl.substring(0, nameInUrl.length() - 1);
            }
            return nameInUrl;
        }

        private String titleAttribute() {
            if (failingTestsGenerationException == null || failingTestsGenerationException.getMessage() == null) {
                return "";
            }
            String messageWithoutDoubleQuotes = failingTestsGenerationException.getMessage().replace("\"", "'");
            return "title=\"" + messageWithoutDoubleQuotes + "\"";
        }
    }
}
