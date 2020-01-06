package com.vmware.config;

import com.vmware.util.CommandLineUtils;
import com.vmware.util.StringUtils;
import com.vmware.util.logging.LogLevel;
import com.vmware.util.scm.Git;
import com.vmware.util.scm.Perforce;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Encapsulates determining the username when running a workflow
 */
public class UsernameParser {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    private Git git = new Git();

    private String username;

    private String source;

    public String[] parse() {
        parseUsernameFromGitEmail();
        parseUsernameFromPerforceIfBlank();
        parseUsernameFromWhoamIIfBlank();
        if (StringUtils.isNotEmpty(username)) {
            return new String[] {username, source};
        } else {
            return null;
        }
    }

    private void parseUsernameFromGitEmail() {
        String gitUserEmail = git.configValue("user.email");
        if (StringUtils.isNotEmpty(gitUserEmail) && gitUserEmail.contains("@")) {
            this.username = gitUserEmail.substring(0, gitUserEmail.indexOf("@"));
            log.info("No username set, parsed username {} from git config user.email {}", username, gitUserEmail);
            this.source = "Git user.email";
        }
    }

    private void parseUsernameFromPerforceIfBlank() {
        if (StringUtils.isNotEmpty(username)) {
            return;
        }
        Perforce perforce = new Perforce(System.getProperty("user.dir"));
        if (perforce.isLoggedIn()) {
            this.username = perforce.getUsername();
            log.info("No username set, using perforce user {} as username", username);
            this.source = "Perforce user";
        }
    }

    private void parseUsernameFromWhoamIIfBlank() {
        if (StringUtils.isNotEmpty(username) || !CommandLineUtils.isCommandAvailable("whoami")) {
            return;
        }
        String fullUsername = CommandLineUtils.executeCommand("whoami", LogLevel.DEBUG);
        String[] usernamePieces = fullUsername.split("\\\\");
        this.username = usernamePieces[usernamePieces.length - 1];
        log.info("No username set, parsed username {} from whoami output {}", username, fullUsername);
        this.source = "Whoami command";
    }
}
