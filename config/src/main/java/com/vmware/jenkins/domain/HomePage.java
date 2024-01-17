package com.vmware.jenkins.domain;

import com.google.gson.annotations.Expose;
import com.vmware.util.StringUtils;
import com.vmware.util.db.DbSaveIgnore;
import com.vmware.util.db.DbUtils;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.regex.PatternSyntaxException;

import static com.vmware.util.StringUtils.pluralize;

public class HomePage {

    public Job[] jobs;

    public View[] views;

    @DbSaveIgnore
    @Expose(serialize = false, deserialize = false)
    public List<JobView> jobViews;

    @DbSaveIgnore
    @Expose(serialize = false, deserialize = false)
    private DbUtils dbUtils;

    public void setDbUtils(DbUtils dbUtils) {
        this.dbUtils = dbUtils;
    }

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
        public long failingTestsCount;

        @Expose(serialize = false, deserialize = false)
        public long failingTestMethodsCount;

        @Expose(serialize = false, deserialize = false)
        public long totalTestMethodsCount;

        @Expose(serialize = false, deserialize = false)
        public long failingJobsWithNoTestFailuresCount;

        public Exception failingTestsGenerationException;

        public View() {
        }

        public View(JobView view, int numberOfFailuresNeededToBeConsistentlyFailing) {
            this.name = view.name;
            this.url = view.url;
            this.failingTestsCount = view.failingTestCount(numberOfFailuresNeededToBeConsistentlyFailing);
            this.failingTestMethodsCount = view.failingTestMethodCount();
            this.totalTestMethodsCount = view.totalTestMethodCount();
        }

        public String viewNameWithFailureCount() {
            if (failingTestsGenerationException != null) {
                return name + " (failed with error " + StringUtils.truncateStringIfNeeded(failingTestsGenerationException.getMessage(), 80) + ")";
            } else if (failingTestsCount == 0 && failingJobsWithNoTestFailuresCount > 0) {
                return name + " (" + pluralize(failingJobsWithNoTestFailuresCount, "job failure") + ")";
            } else if (failingTestsCount == 0) {
                return name + " (no test or config method failures)";
            } else {
                return name + " (" + pluralize(failingTestsCount, "failure") + ")";
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

    public void populateFromDb(String matchingViewName, int numberOfFailuresNeededToBeConsistentlyFailing) {
        if (dbUtils == null) {
            return;
        }

        this.jobViews = dbUtils.query(JobView.class, "SELECT * FROM JOB_VIEW WHERE REGEXP_LIKE(NAME, ?)", matchingViewName);
        this.jobViews.forEach(view -> view.setDbUtils(dbUtils));
        this.jobViews.forEach(view -> view.populateJobsFromDb(numberOfFailuresNeededToBeConsistentlyFailing));

        this.views = jobViews.stream().map(jobView -> new HomePage.View(jobView, numberOfFailuresNeededToBeConsistentlyFailing)).toArray(View[]::new);
    }

    public Map<Job, List<TestResult>> jobTestMap(String viewName) {
        return jobViews.stream().filter(view -> view.name.equals(viewName)).findFirst()
                .orElseThrow(() -> new RuntimeException("Expected to find view named " + viewName)).getJobTestMap();
    }
}
