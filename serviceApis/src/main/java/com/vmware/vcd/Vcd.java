package com.vmware.vcd;

import java.io.File;
import java.util.Comparator;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import com.vmware.AbstractRestService;
import com.vmware.http.HttpConnection;
import com.vmware.http.HttpResponse;
import com.vmware.http.cookie.ApiAuthentication;
import com.vmware.http.credentials.UsernamePasswordAsker;
import com.vmware.http.credentials.UsernamePasswordCredentials;
import com.vmware.http.exception.BadRequestException;
import com.vmware.http.exception.NotAuthorizedException;
import com.vmware.http.request.RequestHeader;
import com.vmware.http.request.body.RequestBodyHandling;
import com.vmware.util.StringUtils;
import com.vmware.util.ThreadUtils;
import com.vmware.vcd.domain.LinkType;
import com.vmware.vcd.domain.MetaDatasType;
import com.vmware.vcd.domain.QueryResultVMsType;
import com.vmware.vcd.domain.QueryResultVappType;
import com.vmware.vcd.domain.QueryResultVappsType;
import com.vmware.vcd.domain.ResourceType;
import com.vmware.vcd.domain.TaskType;
import com.vmware.vcd.domain.VappType;
import com.vmware.vcd.domain.VcdMediaType;

import static com.vmware.http.cookie.ApiAuthentication.vcd;
import static com.vmware.http.request.RequestHeader.aBasicAuthHeader;
import static com.vmware.http.request.RequestHeader.aContentTypeHeader;
import static com.vmware.http.request.RequestHeader.anAcceptHeader;
import static com.vmware.http.request.UrlParam.forceParam;

/**
 * Represents the Vmware Vcloud Director Api
 */
public class Vcd extends AbstractRestService {

    public static String AUTHORIZATION_HEADER = "x-vcloud-authorization";
    private String apiVersion;
    private String vcdOrg;


    public Vcd(String vcdUrl, String apiVersion, String username, String vcdOrg) {
        super(vcdUrl, "api", ApiAuthentication.vcd, username);
        this.apiVersion = apiVersion;
        this.vcdOrg = vcdOrg;
        this.connection = new HttpConnection(RequestBodyHandling.AsStringJsonEntity);
        this.connection.updateTimezoneAndFormat(TimeZone.getDefault(), "yyyy-MM-dd'T'HH:mm:ss.SSS");
        String apiToken = readExistingApiToken(ApiAuthentication.vcd);
        if (StringUtils.isNotEmpty(apiToken)) {
            connection.addStatefulParam(new RequestHeader(AUTHORIZATION_HEADER, apiToken));
        }
    }

    public TaskType deleteResource(LinkType deleteLink, boolean force) {
        return connection.delete(deleteLink.href, TaskType.class, taskAcceptHeader(), forceParam(force));
    }

    public TaskType updateResource(LinkType link, ResourceType resourceType) {
        return connection.put(link.href, TaskType.class, resourceType, contentTypeHeader(resourceType), taskAcceptHeader());
    }

    public QueryResultVappsType getVapps() {
        QueryResultVappsType vapps = optimisticGet(apiUrl + "/query?type=vApp&links=true", QueryResultVappsType.class,
                acceptHeader(QueryResultVappsType.class));
        if (vapps.record != null) {
            String username = getUsername();
            vapps.record.forEach(vapp -> vapp.setOwnedByWorkflowUser(username.equalsIgnoreCase(vapp.ownerName)));
            Comparator<QueryResultVappType> vappSorter = Comparator.comparing(QueryResultVappType::isOwnedByWorkflowUser)
                    .reversed().thenComparing(QueryResultVappType::getLabel);
            vapps.record.sort(vappSorter);
        }
        return vapps;
    }

    public QueryResultVMsType getVmsForVapp(String vappId) {
        return optimisticGet(apiUrl + "/query?type=vm&filter=container==" + vappId, QueryResultVMsType.class, acceptHeader(QueryResultVMsType.class));
    }

    public MetaDatasType getVappMetaData(LinkType metadataLink) {
        return optimisticGet(metadataLink.href, MetaDatasType.class, acceptHeader(MetaDatasType.class));
    }

    public VappType getVapp(LinkType vappLink) {
        return optimisticGet(vappLink.href, VappType.class, acceptHeader(VappType.class));
    }

    public void waitForTaskToComplete(String taskHref, int amount, TimeUnit timeUnit) {
        long elapsedTimeInMilliseconds = 0;

        long waitTimeInMilliseconds = timeUnit.toMillis(amount);
        while (elapsedTimeInMilliseconds < waitTimeInMilliseconds) {
            TaskType task = optimisticGet(taskHref, TaskType.class, taskAcceptHeader());
            log.info("Task: {}, status: {}", task.operation, task.status);
            if ("SUCCESS".equalsIgnoreCase(task.status)) {
                return;
            } else if ("ERROR".equalsIgnoreCase(task.status)) {
                throw new BadRequestException(task.details);
            } else {
                ThreadUtils.sleep(5, TimeUnit.SECONDS);
                elapsedTimeInMilliseconds += 5000;
            }
        }
    }

    @Override
    protected void checkAuthenticationAgainstServer() {
        getVapps();
    }

    @Override
    protected void loginManually() {
        connection.removeStatefulParam(AUTHORIZATION_HEADER);
        if (StringUtils.isNotEmpty(vcdOrg)) {
            log.info("Using default org {}, enter username as username@[org name] to use a different org", vcdOrg);
        } else {
            log.info("Enter username as username@[org name]");
        }
        UsernamePasswordCredentials credentials = UsernamePasswordAsker.askUserForUsernameAndPassword(vcd);
        if (StringUtils.isNotEmpty(vcdOrg) && !credentials.getUsername().contains("@")) {
            log.info("Appending org name {} to username {}", vcdOrg, credentials.getUsername());
            credentials = new UsernamePasswordCredentials(credentials.getUsername() + "@" + vcdOrg, credentials.getPassword());
        } else {
            vcdOrg = StringUtils.splitOnlyOnce(credentials.getUsername(), ",")[1];
            log.info("Setting vcd org to {}", vcdOrg);
        }

        RequestHeader acceptHeader = anAcceptHeader("*/*;version=" + apiVersion);
        HttpResponse response = connection.post(apiUrl + "/sessions", HttpResponse.class, (Object) null,
                acceptHeader, aBasicAuthHeader(credentials));
        List<String> authzHeaders = response.getHeaders().get(AUTHORIZATION_HEADER);
        if (authzHeaders == null || authzHeaders.isEmpty()) {
            throw new NotAuthorizedException("Could not find authorization header " + AUTHORIZATION_HEADER);
        }
        String apiToken = authzHeaders.get(0);
        saveApiToken(apiToken, ApiAuthentication.vcd);
        connection.addStatefulParam(new RequestHeader(AUTHORIZATION_HEADER, apiToken));
    }

    @Override
    protected File determineApiTokenFile() {
        String homeFolder = System.getProperty("user.home");
        return new File(homeFolder + "/." + vcdOrg.toLowerCase() + "-vcd-api-token.txt");
    }

    private RequestHeader taskAcceptHeader() {
        return anAcceptHeader(mediaType(TaskType.class));
    }

    private RequestHeader acceptHeader(Class<?> resourceClass) {
        return anAcceptHeader(mediaType(resourceClass));
    }

    private RequestHeader contentTypeHeader(ResourceType resource) {
        return aContentTypeHeader(mediaType(resource.getClass()));
    }

    private String mediaType(Class<?> dto) {
        return dto.getAnnotation(VcdMediaType.class).value() + "+json;version=" + apiVersion;
    }

}
