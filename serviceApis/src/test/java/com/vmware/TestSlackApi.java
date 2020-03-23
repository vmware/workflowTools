package com.vmware;

import com.vmware.slack.Slack;

import org.junit.Test;

public class TestSlackApi {

    @Test
    public void sendMessage() {
        Slack slack = new Slack("https://slack.com", "dbiggs");
        slack.sendMessage("@dbiggs1234", "hello! I'm a real bot!");
    }
}
