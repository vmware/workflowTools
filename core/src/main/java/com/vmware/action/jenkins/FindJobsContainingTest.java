package com.vmware.action.jenkins;

import com.google.gson.Gson;
import com.vmware.action.BaseAction;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.http.json.ConfiguredGsonBuilder;
import com.vmware.jenkins.domain.JobBuild;
import com.vmware.jenkins.domain.TestResult;
import com.vmware.util.StringUtils;
import com.vmware.util.db.DbUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@ActionDescription("Checks tests database for matching test")
public class FindJobsContainingTest extends BaseAction {
    public FindJobsContainingTest(WorkflowConfig config) {
        super(config);
        super.addFailWorkflowIfBlankProperties("testName");
    }

    @Override
    protected void failWorkflowIfConditionNotMet() {
        super.failWorkflowIfConditionNotMet();
        failIfTrue(!fileSystemConfig.databaseConfigured(), "no database configured");
    }

    @Override
    public void process() {
        DbUtils dbUtils = createDbUtils();
        dbUtils.createConnection();

        String searchText = jenkinsConfig.testName.replace("*", "%");
        List searchParams = new ArrayList();
        String sqlQuery = "SELECT * FROM TEST_RESULT WHERE ";
        if (StringUtils.isNotBlank(jenkinsConfig.jenkinsView)) {
            String viewSubQuery = "SELECT jb.id FROM JOB_BUILD jb, JOB j, JOB_VIEW_MAPPING jvm, JOB_VIEW jv WHERE jb.job_id = j.id " +
                    "AND j.id = jvm.job_id AND jvm.view_id = jv.id AND jv.name = ?";
            searchParams.add(jenkinsConfig.jenkinsView);
            sqlQuery += "job_build_id in (" + viewSubQuery + ") AND ";
        }
        if (searchText.contains(".")) {
            String[] classAndTestName = searchText.split("\\.");
            searchParams.add(classAndTestName[0]);
            searchParams.add(classAndTestName[1]);
            sqlQuery += "CLASS_NAME LIKE ? AND NAME LIKE ? ORDER BY STATUS, STARTED_AT DESC";
        } else {
            searchParams.add(searchText);
            sqlQuery += "NAME LIKE ? ORDER BY STATUS, STARTED_AT DESC";
        }

        List<TestResult> testResults = dbUtils.query(TestResult.class, sqlQuery, searchParams.toArray());

        String viewSuffix = StringUtils.isNotBlank(jenkinsConfig.jenkinsView) ? " in view " + jenkinsConfig.jenkinsView : "";
        if (testResults.isEmpty()) {
            log.debug("No test methods found for {}{}", jenkinsConfig.testName, viewSuffix);
        } else {
            log.debug("{} test methods found matching {}{}", testResults.size(), jenkinsConfig.testName, viewSuffix);
        }
        final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd");
        List<Map<String, Object>> values = new ArrayList<>();
        Map<String, String[]> usedUrls = new HashMap<>();
        IntStream.range(0, testResults.size()).forEach(index -> {
            TestResult testResult = testResults.get(index);
            JobBuild jobBuild = dbUtils.queryUnique(JobBuild.class, "SELECT * FROM JOB_BUILD jb WHERE jb.id = ?", testResult.jobBuildId);
            testResult.setUrlForTestMethod(jobBuild.getTestReportsUIUrl(), usedUrls);
            Map<String, Object> resultValues = new HashMap<>();
            resultValues.put("buildName", jobBuild.name);
            resultValues.put("startedAt", dateFormat.format(testResult.startedAt));
            resultValues.put("name", testResult.classAndTestName());
            resultValues.put("status", testResult.status);
            resultValues.put("url", testResult.url);

            values.add(resultValues);
        });
        Gson gson = new ConfiguredGsonBuilder().setPrettyPrinting().build();
        log.info(gson.toJson(values));

        dbUtils.closeConnection();
    }

    private DbUtils createDbUtils() {
        Stream.of("databaseDriverFile", "databaseDriverClass").forEach(this::failIfUnset);
        log.debug("Using database driver {} and class {}", fileSystemConfig.databaseDriverFile, fileSystemConfig.databaseDriverClass);
        log.debug("Using database {} for test results", fileSystemConfig.databaseUrl);
        return new DbUtils(new File(fileSystemConfig.databaseDriverFile), fileSystemConfig.databaseDriverClass,
                fileSystemConfig.databaseUrl, fileSystemConfig.dbConnectionProperties());
    }
}
