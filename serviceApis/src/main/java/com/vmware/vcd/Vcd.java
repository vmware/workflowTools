package com.vmware.vcd;

import java.io.File;
import java.net.URI;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vmware.AbstractRestService;
import com.vmware.chrome.ChromeDevTools;
import com.vmware.chrome.SsoClient;
import com.vmware.config.section.SsoConfig;
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
import com.vmware.util.UrlUtils;
import com.vmware.util.exception.FatalException;
import com.vmware.util.input.InputUtils;
import com.vmware.vcd.domain.ApiRequest;
import com.vmware.vcd.domain.LinkType;
import com.vmware.vcd.domain.MetaDatasType;
import com.vmware.vcd.domain.OauthClient;
import com.vmware.vcd.domain.OauthClientRegistration;
import com.vmware.vcd.domain.OauthToken;
import com.vmware.vcd.domain.OauthTokenRequest;
import com.vmware.vcd.domain.QueryResultVMsType;
import com.vmware.vcd.domain.QueryResultVappType;
import com.vmware.vcd.domain.QueryResultVappsType;
import com.vmware.vcd.domain.QuotaPools;
import com.vmware.vcd.domain.ResourceType;
import com.vmware.vcd.domain.TaskType;
import com.vmware.vcd.domain.UserSession;
import com.vmware.vcd.domain.UserType;
import com.vmware.vcd.domain.VcdMediaType;

import static com.vmware.http.cookie.ApiAuthentication.vcd_token;
import static com.vmware.http.request.RequestHeader.aBasicAuthHeader;
import static com.vmware.http.request.RequestHeader.aBearerAuthHeader;
import static com.vmware.http.request.RequestHeader.aContentTypeHeader;
import static com.vmware.http.request.RequestHeader.anAcceptHeader;
import static com.vmware.http.request.UrlParam.forceParam;

/**
 * Represents the Vmware Vcloud Director Api
 */
public class Vcd extends AbstractRestService {
    public static final String ACCESS_TOKEN_HEADER = "X-VMWARE-VCLOUD-ACCESS-TOKEN";
    private static final String SSO_LOGIN_BUTTON = "ssoLoginButton";
    private final String apiVersion;
    private final String cloudapiUrl;
    private final String oauthUrl;
    private final boolean useVcdSso;
    private final String ssoEmail;
    private String refreshTokenName;
    private boolean disableRefreshToken;
    private boolean useSiteSpecificToken;
    private final SsoConfig ssoConfig;
    private String vcdOrg;
    private String apiToken;

    public Vcd(String vcdUrl, String apiVersion, String username, String password, String vcdOrg) {
        super(vcdUrl, "api", null, username);
        this.cloudapiUrl = baseUrl + "cloudapi/1.0.0";
        this.oauthUrl = baseUrl + "oauth";
        this.apiVersion = apiVersion;
        this.vcdOrg = vcdOrg;
        this.ssoConfig = null;
        this.ssoEmail = null;
        this.useVcdSso = false;
        this.connection = new HttpConnection(RequestBodyHandling.AsStringJsonEntity);
        this.connection.updateTimezoneAndFormat(TimeZone.getDefault(), "yyyy-MM-dd'T'HH:mm:ss.SSS");
        this.connection.setDisableHostnameVerification(true);
        apiToken = loginWithCredentials(new UsernamePasswordCredentials(username + "@" + vcdOrg, password));
        connection.addStatefulParam(aBearerAuthHeader(apiToken));
    }

    public Vcd(String vcdUrl, String apiVersion, String username, String vcdOrg, boolean useVcdSso, String ssoEmail, String refreshTokenName, boolean disableRefreshToken, boolean ssoHeadless, SsoConfig ssoConfig) {
        super(vcdUrl, "api", ApiAuthentication.vcd_token, username);
        this.useVcdSso = useVcdSso;
        this.ssoEmail = ssoEmail;
        this.refreshTokenName = refreshTokenName;
        this.disableRefreshToken = disableRefreshToken;
        this.cloudapiUrl = baseUrl + "cloudapi/1.0.0";
        this.oauthUrl = baseUrl + "oauth/tenant";
        this.apiVersion = apiVersion;
        this.vcdOrg = vcdOrg;
        this.ssoConfig = ssoConfig;
        this.connection = new HttpConnection(RequestBodyHandling.AsStringJsonEntity);
        this.connection.updateTimezoneAndFormat(TimeZone.getDefault(), "yyyy-MM-dd'T'HH:mm:ss.SSS");
        apiToken = readExistingApiToken(ApiAuthentication.vcd_token);
        if (StringUtils.isNotEmpty(apiToken)) {
            connection.addStatefulParam(aBearerAuthHeader(apiToken));
        }
    }

    public void saveRefreshToken(String token) {
        super.saveApiToken(token, ApiAuthentication.vcd_refresh);
    }

    public Map getResourceAsMap(String resourcePath, String acceptType) {
        return get(UrlUtils.addRelativePaths(apiUrl, resourcePath), Map.class, anAcceptHeader(acceptType + "+json;version=" + apiVersion));
    }

    public Map updateResourceFromMap(String resourcePath, Map resourceValue, String contentType, String acceptType) {
        return put(UrlUtils.addRelativePaths(apiUrl, resourcePath), Map.class, resourceValue,
                aContentTypeHeader(contentType + "+json;version=" + apiVersion), anAcceptHeader(acceptType + "+json;version=" + apiVersion));
    }

    public HttpResponse executeRequest(ApiRequest apiRequest) {
        RequestHeader acceptHeader = new RequestHeader("Accept", apiRequest.acceptType + ";version=" + apiVersion);
        RequestHeader contentTypeHeader = StringUtils.isNotBlank(apiRequest.contentType)
                ? new RequestHeader("Content-Type", apiRequest.contentType + ";version=" + apiVersion) : null;
        RequestHeader[] headers = Stream.of(acceptHeader, contentTypeHeader).filter(Objects::nonNull).toArray(RequestHeader[]::new);
        return connection.executeApiRequest(apiRequest.methodType, apiRequest.url, HttpResponse.class, apiRequest.requestBody, headers);
    }

    public TaskType deleteResource(LinkType deleteLink, boolean force) {
        return connection.delete(deleteLink.href, null, TaskType.class, taskAcceptHeader(), forceParam(force));
    }

    public TaskType updateResource(LinkType link, ResourceType resourceType) {
        return connection.put(link.href, TaskType.class, resourceType, contentTypeHeader(resourceType), taskAcceptHeader());
    }

    public TaskType postResource(LinkType link, ResourceType resourceType) {
        if (resourceType != null) {
            return post(link.href, TaskType.class, resourceType, contentTypeHeader(resourceType), taskAcceptHeader());
        } else {
            return post(link.href, TaskType.class, (Object) null, taskAcceptHeader());
        }
    }


    public <T extends ResourceType> T getResource(LinkType link, Class<T> resourceTypeClass) {
        return get(link.href, resourceTypeClass, acceptHeader(resourceTypeClass));
    }

    public QueryResultVappType queryVappById(String id) {
        QueryResultVappsType vapps = queryVapps("id==" + id);
        if (vapps.record == null || vapps.record.isEmpty()) {
            throw new FatalException("Failed to find vapp for id " + id);
        }
        return vapps.record.get(0);
    }

    public QueryResultVappsType queryVapps(String... filters) {
        QueryResultVappsType vapps = query("vApp", QueryResultVappsType.class, true, filters);
        if (vapps.record != null) {
            UserSession currentSession = getCurrentSession();
            vapps.record.forEach(vapp -> vapp.setOwnedByWorkflowUser(currentSession.user.name.equalsIgnoreCase(vapp.ownerName)));
            Comparator<QueryResultVappType> vappSorter = Comparator.comparing(QueryResultVappType::isOwnedByWorkflowUser)
                    .reversed().thenComparing(QueryResultVappType::getLabel);
            vapps.record.sort(vappSorter);
        }
        return vapps;
    }

    public QueryResultVMsType queryVmsForVapp(String vappId) {
        return queryVms("container==" + vappId);
    }

    public QueryResultVMsType queryVms(String... filters) {
        return query("vm", QueryResultVMsType.class, true, filters);
    }

    public MetaDatasType getVappMetaData(LinkType metadataLink) {
        return getResource(metadataLink, MetaDatasType.class);
    }

    public UserSession getCurrentSession() {
        return get(cloudapiUrl + "/sessions/current", UserSession.class, acceptHeader(UserSession.class));
    }

    public UserType getLoggedInUser() {
        UserSession session = getCurrentSession();
        String userId = StringUtils.substringAfterLast(session.user.id, ":");
        return get(apiUrl + "/admin/user/" + userId, UserType.class, acceptHeader(UserType.class));
    }

    public <T extends ResourceType> T query(String queryType, Class<T> responseTypeClass, boolean includeLinks, String... filterValues) {
        String queryUrl = String.format("%s/query?type=%s&links=%s", apiUrl, queryType,  includeLinks);
        if (filterValues.length > 0) {
            queryUrl += "&filter=" + Arrays.stream(filterValues).filter(Objects::nonNull).collect(Collectors.joining(";"));
        }
        return get(queryUrl, responseTypeClass, acceptHeader(responseTypeClass));
    }

    public QuotaPools getQuotaPools(String userId) {
        String quotaUrl = String.format("%s/users/%s/quotas", cloudapiUrl, userId);
        return get(quotaUrl, QuotaPools.class, acceptHeader(QuotaPools.class));
    }

    public void waitForTaskToComplete(String taskHref, int amount, TimeUnit timeUnit) {
        long elapsedTimeInMilliseconds = 0;

        long waitTimeInMilliseconds = timeUnit.toMillis(amount);
        while (elapsedTimeInMilliseconds < waitTimeInMilliseconds) {
            TaskType task = get(taskHref, TaskType.class, taskAcceptHeader());
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

    public void createRefreshTokenIfNeeded() {
        if (StringUtils.isEmpty(vcdOrg) || disableRefreshToken) {
            return;
        }
        String refreshToken = readExistingApiToken(ApiAuthentication.vcd_refresh);
        if (StringUtils.isNotBlank(refreshToken)) {
            log.info("Refresh token already exists in {}", determineApiTokenFile(ApiAuthentication.vcd_refresh).getPath());
            return;
        }
        HttpResponse sessionResponse = get(cloudapiUrl + "/sessions/current", HttpResponse.class, acceptHeader(UserSession.class));
        if (!sessionResponse.containsLink("add", "OAuthToken")) {
            log.info("Logged in user doesn't have link for rel add with model OAuthToken");
            return;
        }

        log.info("Attempting to create refresh token {}. Use config flag --disable-vcd-refresh to disable use of refresh tokens", refreshTokenName);
        OauthClient oauthClient;
        try {
            oauthClient = post(UrlUtils.addRelativePaths(oauthUrl, vcdOrg, "register"), OauthClient.class,
                    new OauthClientRegistration(refreshTokenName), aBearerAuthHeader(apiToken));
        } catch (BadRequestException bre) {
            if (bre.getMessage().contains("token with the given name already exists")) {
                log.error("Refresh token {} already exsits in VCD, use --vcd-refresh-token-name to specify a different name", refreshTokenName);
                return;
            } else {
                throw bre;
            }
        }

        OauthTokenRequest tokenRequest = new OauthTokenRequest(oauthClient.jwtGrantRequest(), oauthClient.clientId, apiToken);
        connection.setRequestBodyHandling(RequestBodyHandling.AsUrlEncodedFormEntity);
        OauthToken token = post(UrlUtils.addRelativePaths(oauthUrl, vcdOrg, "token"), OauthToken.class,
                tokenRequest, aBearerAuthHeader(apiToken));
        connection.setRequestBodyHandling(RequestBodyHandling.AsStringJsonEntity);
        saveRefreshToken(token.refreshToken);
    }

    @Override
    protected void checkAuthenticationAgainstServer() {
        String queryVappsUrl = String.format("%s/query?type=%s", apiUrl, "vApp");
        connection.get(queryVappsUrl, QueryResultVappsType.class);
    }

    @Override
    protected void loginManually() {
        connection.removeStatefulParam(RequestHeader.AUTHORIZATION);
        String refreshToken = readExistingApiToken(ApiAuthentication.vcd_refresh);
        if (StringUtils.isNotBlank(refreshToken) && StringUtils.isNotBlank(vcdOrg) && !disableRefreshToken) {
            File refreshTokenFile = determineApiTokenFile(ApiAuthentication.vcd_refresh);
            log.info("Using refresh token from {}", refreshTokenFile.getPath());
            try {
                apiToken = createAccessTokenUsingRefreshToken(refreshToken);
                saveApiToken(apiToken, ApiAuthentication.vcd_token);
                connection.addStatefulParam(aBearerAuthHeader(apiToken));
                return;
            } catch (BadRequestException bre) {
                log.info("Failed to get api token for refresh token", bre);
                String vcdHostName = URI.create(baseUrl).getHost();
                if (!refreshTokenFile.getName().contains(vcdHostName)) {
                    log.info("Creating site specific refresh token for {}", vcdHostName);
                    useSiteSpecificToken = true;
                }
            }
        }
        if (useVcdSso && StringUtils.isEmpty(vcdOrg)) {
            vcdOrg = InputUtils.readValueUntilNotBlank("Vcd Organization");
        }
        if (useVcdSso) {
            SsoClient client = new SsoClient(ssoConfig, getUsername(), ssoEmail);
            try {
                Consumer<ChromeDevTools> ssoNavigateFunction = ChromeDevTools::waitForDomContentEvent;
                String siteUrl = UrlUtils.addRelativePaths(baseUrl, "tenant", vcdOrg.toLowerCase(), "vdcs");
                apiToken = client.loginAndGetApiToken(siteUrl, siteUrl, SSO_LOGIN_BUTTON,
                        ssoNavigateFunction, (devTools) -> devTools.getValue("this.localStorage.getItem('jwt')"));
            } catch (RuntimeException e) {
                log.debug(String.valueOf(e.getMessage()), e);
                log.info("Encountered exception when using SSO: {}", e.getMessage());
                log.info("Failed to get api token via SSO, please enter api token");
                apiToken = InputUtils.readValueUntilNotBlank("Bearer Api Token");
            }
            saveApiToken(apiToken, vcd_token);
            connection.addStatefulParam(aBearerAuthHeader(apiToken));
            createRefreshTokenIfNeeded();
            return;
        }
        if (StringUtils.isNotEmpty(vcdOrg)) {
            log.info("Using default org {}, enter username as username@[org name] to use a different org", vcdOrg);
        } else {
            log.info("Enter username as username@[org name]");
        }
        UsernamePasswordCredentials credentials = UsernamePasswordAsker.askUserForUsernameAndPassword(vcd_token, getUsername());
        if (StringUtils.isNotEmpty(vcdOrg) && !credentials.getUsername().contains("@")) {
            log.info("Appending org name {} to username {}", vcdOrg, credentials.getUsername());
            credentials = new UsernamePasswordCredentials(credentials.getUsername() + "@" + vcdOrg, credentials.getPassword());
        } else {
            vcdOrg = StringUtils.splitOnlyOnce(credentials.getUsername(), "@")[1];
            log.info("Setting vcd org to {}", vcdOrg);
        }

        apiToken = loginWithCredentials(credentials);
        saveApiToken(apiToken, vcd_token);
        connection.addStatefulParam(aBearerAuthHeader(apiToken));
        createRefreshTokenIfNeeded();
    }

    @Override
    protected File determineApiTokenFile(ApiAuthentication apiAuthentication) {
        String homeFolder = System.getProperty("user.home");

        File apiOrgTokenFile = new File(homeFolder + "/." + vcdOrg.toLowerCase() + "-" + URI.create(baseUrl).getHost()
                + "-" + apiAuthentication.getFileName().substring(1));
        if (apiOrgTokenFile.exists() || useSiteSpecificToken) {
            useSiteSpecificToken = true;
            return apiOrgTokenFile;
        } else {
            log.debug("Org api token file {} does not exist", apiOrgTokenFile.getPath());
        }
        return new File(homeFolder + "/." + vcdOrg.toLowerCase() + "-" + apiAuthentication.getFileName().substring(1));
    }

    private RequestHeader taskAcceptHeader() {
        return acceptHeader(TaskType.class);
    }

    private RequestHeader acceptHeader(Class<?> resourceClass) {
        return anAcceptHeader(mediaType(resourceClass));
    }

    private RequestHeader contentTypeHeader(ResourceType resource) {
        return aContentTypeHeader(mediaType(resource.getClass()));
    }

    private String mediaType(Class<?> dto) {
        String mediaType = dto.getAnnotation(VcdMediaType.class).value();
        if ("application/json".equalsIgnoreCase(mediaType)) {
            return mediaType + ";version=" + apiVersion;
        } else {
            return dto.getAnnotation(VcdMediaType.class).value() + "+json;version=" + apiVersion;
        }
    }

    private String loginWithCredentials(UsernamePasswordCredentials credentials) {
        String loginUrl = baseUrl + "api/sessions";
        RequestHeader acceptHeader = anAcceptHeader("*/*;version=" + apiVersion);
        log.debug("Logging into vcd at url {} with username {} and accept header {}", loginUrl, credentials.getUsername(), acceptHeader);
        HttpResponse response = connection.post(loginUrl, HttpResponse.class, (Object) null,
                acceptHeader, aBasicAuthHeader(credentials));
        List<String> authzHeaders = response.getHeaders().get(ACCESS_TOKEN_HEADER);
        if (authzHeaders == null || authzHeaders.isEmpty()) {
            throw new NotAuthorizedException("Could not find access token header " + ACCESS_TOKEN_HEADER);
        }
        return authzHeaders.get(0);
    }

    private String createAccessTokenUsingRefreshToken(String refreshToken) {
        String tokenUrl = UrlUtils.addRelativePaths(oauthUrl, vcdOrg, "token");
        connection.setRequestBodyHandling(RequestBodyHandling.AsUrlEncodedFormEntity);
        OauthToken token = connection.post(tokenUrl, OauthToken.class, new OauthTokenRequest("refresh_token", refreshToken),
                acceptHeader(OauthToken.class));
        connection.setRequestBodyHandling(RequestBodyHandling.AsStringJsonEntity);
        return token.accessToken;
    }
}
