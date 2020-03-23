package com.vmware.slack;

import com.google.gson.FieldNamingPolicy;
import com.vmware.AbstractRestService;
import com.vmware.http.HttpConnection;
import com.vmware.http.cookie.ApiAuthentication;
import com.vmware.http.exception.BadRequestException;
import com.vmware.http.json.ConfiguredGsonBuilder;
import com.vmware.http.request.RequestHeader;
import com.vmware.http.request.body.RequestBodyHandling;
import com.vmware.slack.domain.ChannelMessage;
import com.vmware.slack.domain.SlackResponse;

public class Slack extends AbstractRestService {

    public Slack(String url, String username) {
        super(url, "/api", ApiAuthentication.none, username);
        this.connection = new HttpConnection(RequestBodyHandling.AsStringJsonEntity,
                new ConfiguredGsonBuilder().namingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).build());
        this.connection.addStatefulParam(RequestHeader.aBearerAuthHeader("xoxp-7985532022-10677794721-956768831555-bf19feadf00192d278f8abb84cae19a4"));
        //this.connection.addStatefulParam(RequestHeader.aBearerAuthHeader("xoxb-7985532022-968583689670-Wa3NVsVoumuefF5TesHjxxxC"));
    }

    public void sendMessage(String channel, String text) {
        ChannelMessage message = new ChannelMessage();
        message.channel = channel;
        message.text = text;

        SlackResponse response = optimisticPost(apiUrl + "/chat.postMessage", SlackResponse.class, message);
        checkResponse(response);
    }

    @Override
    protected void checkAuthenticationAgainstServer() {

    }

    @Override
    protected void loginManually() {

    }

    private void checkResponse(SlackResponse response) {
        if (response.ok) {
            return;
        }

        throw new BadRequestException(response.toString());
    }
}
