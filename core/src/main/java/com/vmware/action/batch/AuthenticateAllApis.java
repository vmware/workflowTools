package com.vmware.action.batch;

import com.vmware.AbstractService;
import com.vmware.action.BaseAction;
import com.vmware.bugzilla.Bugzilla;
import com.vmware.config.ActionDescription;
import com.vmware.config.WorkflowConfig;
import com.vmware.gitlab.Gitlab;
import com.vmware.http.cookie.ApiAuthentication;
import com.vmware.jenkins.Jenkins;
import com.vmware.jira.Jira;
import com.vmware.reviewboard.ReviewBoard;
import com.vmware.trello.Trello;
import com.vmware.util.StringUtils;
import com.vmware.vcd.Vcd;

import static com.vmware.util.StringUtils.firstNonEmpty;

@ActionDescription("Ensures that all apis have a valid token / cookie. Primarily for testing.")
public class AuthenticateAllApis extends BaseAction {

    public AuthenticateAllApis(WorkflowConfig config) {
        super(config);
    }

    @Override
    public void process() {
        checkAuthentication(new Gitlab(gitlabConfig.gitlabUrl));
        String ssoEmail = StringUtils.isNotBlank(ssoConfig.ssoEmail) ? ssoConfig.ssoEmail : git.configValue("user.email");
        checkAuthentication(new Vcd(vcdConfig.vcdUrl, vcdConfig.vcdApiVersion, vcdConfig.vcdApiVersion, vcdConfig.defaultVcdOrg, vcdConfig.vcdSso,
                ssoEmail, vcdConfig.refreshTokenName, vcdConfig.disableVcdRefreshToken, vcdConfig.vcdSsoButtonId, ssoConfig));

        ApiAuthentication reviewBoardCredentialsType = config.reviewBoardConfig.useRbApiToken ? ApiAuthentication.reviewBoard_token : ApiAuthentication.reviewBoard_cookie;
        checkAuthentication(new ReviewBoard(reviewBoardConfig.reviewboardUrl, determineUsername(reviewBoardConfig.rbUsername), reviewBoardCredentialsType));
        checkAuthentication(new Bugzilla(bugzillaConfig.bugzillaUrl, determineUsername(bugzillaConfig.bugzillaUsername), bugzillaConfig.bugzillaTestBug, bugzillaConfig.bugzillaSso, ssoConfig, bugzillaConfig.bugzillaSsoLoginId));
        checkAuthentication(new Jira(jiraConfig.jiraUrl, determineUsername(jiraConfig.jiraUsername), jiraConfig.jiraCustomFieldNames));
        checkAuthentication(new Jenkins(jenkinsConfig.jenkinsUrl, determineUsername(jenkinsConfig.jenkinsUsername), jenkinsConfig.jenkinsUsesCsrf, jenkinsConfig.disableJenkinsLogin, jenkinsConfig.testReportsUrlOverrides));
        checkAuthentication(new Trello(trelloConfig.trelloUrl, determineUsername(trelloConfig.trelloUsername), trelloConfig.trelloSso, ssoEmail, ssoConfig));
    }

    private void checkAuthentication(AbstractService restService) {
        if (StringUtils.isEmpty(restService.baseUrl)) {
            log.info("Skipping check of service {} as the base url is blank", restService.getClass().getSimpleName());
        }
        String serviceName = restService.getClass().getSimpleName();
        log.info("Checking authentication for service {}", serviceName);
        restService.setupAuthenticatedConnection();
        log.info("Finished checking authentication for service {}", serviceName);
    }

    private String determineUsername(String overrideValue) {
        return firstNonEmpty(overrideValue, config.username);
    }
}
