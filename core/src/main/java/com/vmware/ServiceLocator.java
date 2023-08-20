package com.vmware;

import com.vmware.bugzilla.Bugzilla;
import com.vmware.buildweb.Buildweb;
import com.vmware.config.WorkflowConfig;
import com.vmware.config.section.BuildwebConfig;
import com.vmware.config.section.JenkinsConfig;
import com.vmware.config.section.SsoConfig;
import com.vmware.config.section.TrelloConfig;
import com.vmware.config.section.VcdConfig;
import com.vmware.github.Github;
import com.vmware.gitlab.Gitlab;
import com.vmware.http.cookie.ApiAuthentication;
import com.vmware.jenkins.Jenkins;
import com.vmware.jira.Jira;
import com.vmware.reviewboard.ReviewBoard;
import com.vmware.util.StringUtils;
import com.vmware.util.scm.Git;
import com.vmware.util.scm.Perforce;
import com.vmware.trello.Trello;
import com.vmware.vcd.Vcd;

/**
 * Centralized locator for git, perforce, jenkins, jira, reviewboard and trello instances.
 * Ensures that the above classes are singletons.
 */
public class ServiceLocator {

    private Git git;

    private Gitlab gitlab;

    private Github github;

    private Perforce perforce;

    private Jira jira;

    private Bugzilla bugzilla;

    private ReviewBoard reviewBoard;

    private Jenkins jenkins;

    private Buildweb buildweb;

    private Trello trello;

    private Vcd vcd;

    private WorkflowConfig config;

    public ServiceLocator(WorkflowConfig config) {
        this.config = config;
    }

    public Jira getJira() {
        if (jira == null) {
            jira = new Jira(config.jiraConfig.jiraUrl, config.username, config.jiraConfig.jiraCustomFieldNames);
        }
        return jira;
    }

    public Bugzilla getBugzilla() {
        if (bugzilla == null) {
            bugzilla = new Bugzilla(config.bugzillaConfig.bugzillaUrl, config.username, config.bugzillaConfig.bugzillaTestBug);
        }
        return bugzilla;
    }

    public ReviewBoard getReviewBoard() {
        if (reviewBoard == null) {
            reviewBoard = new ReviewBoard(config.reviewBoardConfig.reviewboardUrl, config.username, reviewBoardCredentialsType());
            if (reviewBoard.isConnectionAuthenticated()) {
                reviewBoard.updateClientTimeZone(config.reviewBoardConfig.reviewBoardDateFormat);
            }
        }
        return reviewBoard;
    }

    public Jenkins getJenkins() {
        if (jenkins == null) {
            jenkins = newJenkins();
        }
        return jenkins;
    }

    public Jenkins newJenkins() {
        JenkinsConfig jenkinsConfig = config.jenkinsConfig;
        return new Jenkins(jenkinsConfig.jenkinsUrl, config.username, jenkinsConfig.jenkinsUsesCsrf, jenkinsConfig.disableJenkinsLogin, jenkinsConfig.testReportsUrlOverrides);
    }

    public Buildweb getBuildweb() {
        if (buildweb == null) {
            BuildwebConfig buildwebConfig = config.buildwebConfig;
            buildweb = new Buildweb(buildwebConfig.buildwebUrl, buildwebConfig.buildwebApiUrl,
                    buildwebConfig.buildwebLogFileName, config.username);
        }
        return buildweb;
    }

    public Vcd getVcd() {
        if (vcd == null) {
            VcdConfig vcdConfig = config.vcdConfig;
            SsoConfig ssoConfig = config.ssoConfig;
            String ssoEmail = StringUtils.isNotBlank(ssoConfig.ssoEmail) ? ssoConfig.ssoEmail : git.configValue("user.email");
            vcd = new Vcd(vcdConfig.vcdUrl, vcdConfig.vcdApiVersion, config.username, vcdConfig.defaultVcdOrg, vcdConfig.vcdSso, ssoEmail,
                    vcdConfig.refreshTokenName, vcdConfig.disableVcdRefreshToken, ssoConfig.ssoHeadless, ssoConfig);
        }
        return vcd;
    }

    public Trello getTrello() {
        if (trello == null) {
            SsoConfig ssoConfig = config.ssoConfig;
            TrelloConfig trelloConfig = config.trelloConfig;
            String ssoEmail = StringUtils.isNotBlank(ssoConfig.ssoEmail) ? ssoConfig.ssoEmail : git.configValue("user.email");
            trello = new Trello(trelloConfig.trelloUrl, config.username, trelloConfig.trelloSso, ssoEmail, ssoConfig);
        }
        return trello;
    }

    public Git getGit() {
        if (git == null) {
            git = new Git();
        }
        return git;
    }

    public Perforce getPerforce() {
        if (perforce == null) {
            perforce = new Perforce(config.perforceClientConfig.perforceClientName,
                    config.perforceClientConfig.perforceClientDirectory);
        }
        return perforce;
    }

    public Gitlab getGitlab() {
        if (gitlab == null) {
            gitlab = new Gitlab(config.gitlabConfig.gitlabUrl, config.username);
        }
        return gitlab;
    }

    public Github getGithub() {
        if (github == null) {
            github = new Github(config.githubConfig.githubUrl, config.username);
        }
        return github;
    }

    public ApiAuthentication reviewBoardCredentialsType() {
        return config.reviewBoardConfig.useRbApiToken ? ApiAuthentication.reviewBoard_token : ApiAuthentication.reviewBoard;
    }
}
